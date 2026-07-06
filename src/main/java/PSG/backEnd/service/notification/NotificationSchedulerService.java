package PSG.backEnd.service.notification;

import PSG.backEnd.model.dto.notification.NotificationRunReportDTO;
import PSG.backEnd.model.entity.notification.NotificationAlert;
import PSG.backEnd.model.entity.notification.NotificationPayload;
import PSG.backEnd.model.entity.notification.NotificationSubscription;
import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.repository.NotificationLogRepository;
import PSG.backEnd.repository.NotificationSubscriptionRepository;
import PSG.backEnd.repository.TenantRepository;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.service.notification.resolver.NextDueDateResolver;
import PSG.backEnd.service.util.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationSchedulerService {

    private final TenantRepository tenantRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository logRepository;
    private final UserRepository userRepository;
    private final Map<NotificationSubjectType, NextDueDateResolver> resolvers;
    private final NotificationDispatchService dispatchService;

    public NotificationSchedulerService(
            TenantRepository tenantRepository,
            NotificationSubscriptionRepository subscriptionRepository,
            NotificationLogRepository logRepository,
            UserRepository userRepository,
            List<NextDueDateResolver> resolverList,
            NotificationDispatchService dispatchService) {
        this.tenantRepository       = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.logRepository          = logRepository;
        this.userRepository         = userRepository;
        this.dispatchService        = dispatchService;
        this.resolvers = resolverList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NextDueDateResolver::getSubjectType, Function.identity()));
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Argentina/Buenos_Aires")
    public void runDailyCheck() {
        log.info("NotificationScheduler: starting daily run");
        tenantRepository.findAllByDeletedFalseAndActiveTrue().forEach(tenant -> {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                NotificationRunReportDTO report = processTenant(tenant.getId(), false, false);
                log.info("NotificationScheduler: tenant {} → dispatched {} of {} evaluated",
                        tenant.getId(), report.dispatched(), report.items().size());
            } catch (Exception e) {
                log.error("NotificationScheduler: error processing tenant {}: {}",
                        tenant.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        });
        log.info("NotificationScheduler: daily run complete");
    }

    /**
     * On-demand run for the current tenant. Used by the manual trigger endpoint to test
     * and audit the notification pipeline without waiting for the daily cron.
     *
     * @param force  ignore the per-cycle deduplication (re-dispatch even if already sent today)
     * @param dryRun evaluate and report outcomes without dispatching anything
     */
    @Transactional(readOnly = true)
    public NotificationRunReportDTO runManual(boolean force, boolean dryRun) {
        Long tenantId = TenantContext.getCurrentTenant();
        log.info("NotificationScheduler: MANUAL run tenant={} force={} dryRun={}", tenantId, force, dryRun);
        return processTenant(tenantId, force, dryRun);
    }

    private NotificationRunReportDTO processTenant(Long tenantId, boolean force, boolean dryRun) {
        LocalDate today = LocalDate.now();
        Set<String> sentCycleKeys = force
                ? Set.of()
                : logRepository.findSentCycleKeysForTenant(tenantId, today);

        List<NotificationSubscription> subscriptions =
                subscriptionRepository.findAllByDeletedFalseAndActiveTrue();

        List<NotificationRunReportDTO.Item> items = new ArrayList<>();
        int subjectsEvaluated = 0;
        int dispatched = 0;

        for (NotificationSubscription sub : subscriptions) {
            NextDueDateResolver resolver = resolvers.get(sub.getSubjectType());
            if (resolver == null) continue;

            List<SubjectDueDateInfo> subjects = sub.getSubjectId() != null
                    ? resolver.resolveForId(sub.getSubjectId())
                    : resolver.resolveAll();
            subjectsEvaluated += subjects.size();

            for (SubjectDueDateInfo info : subjects) {
                for (NotificationAlert alert : sub.getAlerts()) {
                    if (!alert.isActive()) continue;
                    NotificationRunReportDTO.Item item =
                            evaluate(sub, alert, info, today, sentCycleKeys, tenantId, dryRun);
                    items.add(item);
                    if ("DISPATCHED".equals(item.outcome())) dispatched++;
                }
            }
        }

        log.info("NotificationScheduler: tenant={} subs={} subjects={} dispatched={} dryRun={} force={}",
                tenantId, subscriptions.size(), subjectsEvaluated, dispatched, dryRun, force);

        return new NotificationRunReportDTO(
                today, dryRun, force, subscriptions.size(), subjectsEvaluated, dispatched, items);
    }

    private NotificationRunReportDTO.Item evaluate(NotificationSubscription sub,
                                                   NotificationAlert alert,
                                                   SubjectDueDateInfo info,
                                                   LocalDate today,
                                                   Set<String> sentCycleKeys,
                                                   Long tenantId,
                                                   boolean dryRun) {
        LocalDate triggerDate = info.dueDate().minusDays(alert.getDaysBeforeAlert());
        String dedupKey = sub.getId() + "_" + alert.getId() + "_"
                + (info.subjectId() != null ? info.subjectId() : "null")
                + "_" + info.dueDate();

        String outcome;
        if (today.isBefore(triggerDate) || today.isAfter(info.dueDate())) {
            outcome = "OUT_OF_WINDOW";
        } else if (sentCycleKeys.contains(dedupKey)) {
            outcome = "ALREADY_SENT";
        } else {
            Optional<User> user = userRepository.findByIdAndDeletedFalse(sub.getUserId());
            if (user.isEmpty()) {
                outcome = "USER_NOT_FOUND";
            } else if (dryRun) {
                outcome = "WOULD_DISPATCH";
            } else {
                Map<NotificationChannel, String> channelAddresses =
                        new EnumMap<>(NotificationChannel.class);
                channelAddresses.put(NotificationChannel.EMAIL, user.get().getEmail());

                NotificationPayload payload = new NotificationPayload(
                        sub.getSubjectType(),
                        info.subjectId(),
                        info.displayName(),
                        info.description(),
                        info.dueDate(),
                        alert.getDaysBeforeAlert(),
                        user.get().getId(),
                        channelAddresses
                );
                dispatchService.dispatch(payload, sub.getChannels(), sub.getId(), alert.getId(), tenantId);
                outcome = "DISPATCHED";
            }
        }

        return new NotificationRunReportDTO.Item(
                sub.getId(), sub.getSubjectType(), info.subjectId(), info.displayName(),
                info.dueDate(), alert.getDaysBeforeAlert(), triggerDate, sub.getChannels(), outcome);
    }
}
