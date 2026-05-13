package PSG.backEnd.service.notification;

import PSG.backEnd.model.entity.notification.NotificationAlert;
import PSG.backEnd.model.entity.notification.NotificationPayload;
import PSG.backEnd.model.entity.notification.NotificationSubscription;
import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.repository.NotificationLogRepository;
import PSG.backEnd.repository.NotificationSubscriptionRepository;
import PSG.backEnd.repository.TenantRepository;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.service.notification.resolver.NextDueDateResolver;
import PSG.backEnd.service.util.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    @Scheduled(cron = "0 0 8 * * *")
    @Async
    public void runDailyCheck() {
        log.info("NotificationScheduler: starting daily run");
        tenantRepository.findAllByDeletedFalseAndActiveTrue().forEach(tenant -> {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                processTenant(tenant.getId());
            } catch (Exception e) {
                log.error("NotificationScheduler: error processing tenant {}: {}",
                        tenant.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        });
        log.info("NotificationScheduler: daily run complete");
    }

    private void processTenant(Long tenantId) {
        LocalDate today = LocalDate.now();
        Set<String> sentToday = logRepository.findSentKeysForTenantAndDate(tenantId, today);
        List<NotificationSubscription> subscriptions =
                subscriptionRepository.findAllByDeletedFalseAndActiveTrue();

        for (NotificationSubscription sub : subscriptions) {
            NextDueDateResolver resolver = resolvers.get(sub.getSubjectType());
            if (resolver == null) continue;

            List<SubjectDueDateInfo> subjects = sub.getSubjectId() != null
                    ? resolver.resolveForId(sub.getSubjectId())
                    : resolver.resolveAll();

            for (SubjectDueDateInfo info : subjects) {
                for (NotificationAlert alert : sub.getAlerts()) {
                    if (!alert.isActive()) continue;
                    processAlert(sub, alert, info, today, sentToday, tenantId);
                }
            }
        }
    }

    private void processAlert(NotificationSubscription sub,
                               NotificationAlert alert,
                               SubjectDueDateInfo info,
                               LocalDate today,
                               Set<String> sentToday,
                               Long tenantId) {
        LocalDate triggerDate = info.dueDate().minusDays(alert.getDaysBeforeAlert());
        if (!today.equals(triggerDate)) return;

        String dedupKey = sub.getId() + "_" + alert.getId() + "_"
                + (info.subjectId() != null ? info.subjectId() : "null");
        if (sentToday.contains(dedupKey)) return;

        userRepository.findByIdAndDeletedFalse(sub.getUserId()).ifPresent(user -> {
            Map<NotificationChannel, String> channelAddresses =
                    new EnumMap<>(NotificationChannel.class);
            channelAddresses.put(NotificationChannel.EMAIL, user.getEmail());
            channelAddresses.put(NotificationChannel.WHATSAPP, user.getWhatsappNumber());

            NotificationPayload payload = new NotificationPayload(
                    sub.getSubjectType(),
                    info.subjectId(),
                    info.displayName(),
                    info.dueDate(),
                    alert.getDaysBeforeAlert(),
                    user.getId(),
                    channelAddresses
            );
            dispatchService.dispatch(payload, sub.getChannels(), sub.getId(), alert.getId(), tenantId);
        });
    }
}
