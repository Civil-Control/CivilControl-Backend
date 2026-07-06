package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.notification.NotificationSubscriptionNotFoundException;
import PSG.backEnd.exception.notification.NotificationSubscriptionNotValidException;
import PSG.backEnd.exception.user.UserNotFoundException;
import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.notification.NotificationInboxItemDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionResponseDTO;
import PSG.backEnd.model.entity.notification.NotificationAlert;
import PSG.backEnd.model.entity.notification.NotificationLog;
import PSG.backEnd.model.entity.notification.NotificationSubscription;
import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.security.UserDeletedEvent;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.model.mapper.NotificationSubscriptionMapper;
import PSG.backEnd.repository.NotificationLogRepository;
import PSG.backEnd.repository.NotificationSubscriptionRepository;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.service.notification.resolver.NextDueDateResolver;
import PSG.backEnd.service.port.INotificationSubscriptionService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class NotificationSubscriptionService implements INotificationSubscriptionService {

    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository logRepository;
    private final UserRepository userRepository;
    private final NotificationSubscriptionMapper mapper;
    private final MessageSourceHelper messageSourceHelper;
    private final Map<NotificationSubjectType, NextDueDateResolver> resolvers;

    public NotificationSubscriptionService(
            NotificationSubscriptionRepository subscriptionRepository,
            NotificationLogRepository logRepository,
            UserRepository userRepository,
            NotificationSubscriptionMapper mapper,
            MessageSourceHelper messageSourceHelper,
            List<NextDueDateResolver> resolverList) {
        this.subscriptionRepository = subscriptionRepository;
        this.logRepository          = logRepository;
        this.userRepository         = userRepository;
        this.mapper                 = mapper;
        this.messageSourceHelper    = messageSourceHelper;
        this.resolvers = resolverList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NextDueDateResolver::getSubjectType, Function.identity()));
    }

    // ==================== CRUD ====================

    @Override
    public NotificationSubscriptionResponseDTO createSubscription(NotificationSubscriptionDTO dto) {
        User currentUser  = getCurrentUser();
        Long currentUserId = currentUser.getId();

        checkCreatePermission(dto.userId(), currentUserId);

        User targetUser = userRepository.findByIdAndDeletedFalse(dto.userId())
                .orElseThrow(() -> new UserNotFoundException(dto.userId()));

        if (dto.subjectType() == NotificationSubjectType.CUSTOM_REMINDER) {
            throw new NotificationSubscriptionNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.customReminderNotAllowed"));
        }

        validateChannelVerification(dto, targetUser);

        if (dto.subjectId() != null) {
            NextDueDateResolver resolver = resolvers.get(dto.subjectType());
            if (resolver == null || resolver.resolveForId(dto.subjectId()).isEmpty()) {
                throw new NotificationSubscriptionNotValidException(
                        messageSourceHelper.getMessage("notification.subscription.subjectNotFound"));
            }
        }

        boolean duplicate = dto.subjectId() != null
                ? subscriptionRepository.existsByUserIdAndSubjectTypeAndSubjectIdAndDeletedFalse(
                        dto.userId(), dto.subjectType(), dto.subjectId())
                : subscriptionRepository.existsByUserIdAndSubjectTypeAndSubjectIdIsNullAndDeletedFalse(
                        dto.userId(), dto.subjectType());
        if (duplicate) {
            throw new NotificationSubscriptionNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.duplicate"));
        }

        validateUniqueAlertDays(dto);

        NotificationSubscription entity = mapper.toEntity(dto);
        entity.setSubscribedByUserId(currentUserId);
        entity.setChannels(dto.channels());

        dto.alerts().stream()
                .map(a -> {
                    NotificationAlert alert = mapper.alertToEntity(a);
                    alert.setSubscription(entity);
                    return alert;
                })
                .forEach(entity.getAlerts()::add);

        try {
            return enrichAndMap(subscriptionRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw new NotificationSubscriptionNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.duplicate"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSubscriptionResponseDTO getSubscriptionById(Long id) {
        NotificationSubscription entity = subscriptionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotificationSubscriptionNotFoundException(id));
        checkOwnerOrAdmin(entity);
        return enrichAndMap(entity);
    }

    @Override
    public NotificationSubscriptionResponseDTO updateSubscription(Long id, NotificationSubscriptionDTO dto) {
        NotificationSubscription entity = subscriptionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotificationSubscriptionNotFoundException(id));
        checkOwnerOrAdmin(entity);
        validateUniqueAlertDays(dto);

        entity.setChannels(dto.channels());
        entity.setActive(dto.active());

        entity.getAlerts().clear();
        dto.alerts().stream()
                .map(a -> {
                    NotificationAlert alert = mapper.alertToEntity(a);
                    alert.setSubscription(entity);
                    return alert;
                })
                .forEach(entity.getAlerts()::add);

        return enrichAndMap(subscriptionRepository.save(entity));
    }

    @Override
    public void deleteSubscription(Long id) {
        NotificationSubscription entity = subscriptionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotificationSubscriptionNotFoundException(id));
        checkOwnerOrAdmin(entity);
        entity.setDeleted(true);
        subscriptionRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSubscriptionResponseDTO> getMySubscriptions() {
        Long currentUserId = getCurrentUser().getId();
        return subscriptionRepository.findAllByUserIdAndDeletedFalse(currentUserId).stream()
                .map(this::enrichAndMap)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSubscriptionResponseDTO> getAllSubscriptions() {
        return subscriptionRepository.findAllByDeletedFalse().stream()
                .map(this::enrichAndMap)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSubscriptionResponseDTO> getUserSubscriptions(Long userId) {
        if (!hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
            throw new NotificationSubscriptionNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.forbidden"));
        }
        return subscriptionRepository.findAllByUserIdAndDeletedFalse(userId).stream()
                .map(this::enrichAndMap)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationInboxItemDTO> getInbox(int limit) {
        Long currentUserId = getCurrentUser().getId();
        List<Long> subscriptionIds = subscriptionRepository
                .findAllByUserIdAndDeletedFalse(currentUserId).stream()
                .map(NotificationSubscription::getId)
                .toList();

        if (subscriptionIds.isEmpty()) return List.of();

        return logRepository.findSystemInboxBySubscriptionIds(
                subscriptionIds, PageRequest.of(0, limit)).stream()
                .map(this::toInboxItem)
                .toList();
    }

    @Override
    public void markInboxItemRead(Long logId) {
        NotificationLog log = getOwnedInboxLog(logId);
        if (!log.isRead()) {
            log.setRead(true);
            logRepository.save(log);
        }
    }

    @Override
    public void dismissInboxItem(Long logId) {
        NotificationLog log = getOwnedInboxLog(logId);
        if (!log.isDismissed()) {
            log.setDismissed(true);
            logRepository.save(log);
        }
    }

    // ==================== Event listener ====================

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        subscriptionRepository.markDeletedByUserId(event.userId());
        log.info("Soft-deleted subscriptions for userId={}", event.userId());
    }

    // ==================== Private helpers ====================

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean hasAuthority(String permission) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals(permission));
    }

    private void checkCreatePermission(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            if (!hasAuthority(AppPermissions.NOTIFICATION_SELF_SUBSCRIBE)
                    && !hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
                throw new NotificationSubscriptionNotValidException(
                        messageSourceHelper.getMessage("notification.subscription.forbidden"));
            }
        } else {
            if (!hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
                throw new NotificationSubscriptionNotValidException(
                        messageSourceHelper.getMessage("notification.subscription.forbidden"));
            }
        }
    }

    private void checkOwnerOrAdmin(NotificationSubscription entity) {
        Long currentUserId = getCurrentUser().getId();
        if (!entity.getUserId().equals(currentUserId)
                && !hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
            throw new NotificationSubscriptionNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.forbidden"));
        }
    }

    private void validateChannelVerification(NotificationSubscriptionDTO dto, User targetUser) {
        if (dto.channels() == null) return;
        boolean needsEmail = dto.channels().contains(NotificationChannel.EMAIL);
        if (!needsEmail) return;

        if (!Boolean.TRUE.equals(targetUser.getEmailVerified())) {
            throw new NotificationSubscriptionNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.channel.emailNotVerified"));
        }
    }

    private void validateUniqueAlertDays(NotificationSubscriptionDTO dto) {
        if (dto.alerts() == null) return;
        Set<Integer> seen = new HashSet<>();
        dto.alerts().forEach(a -> {
            if (!seen.add(a.daysBeforeAlert())) {
                throw new NotificationSubscriptionNotValidException(
                        messageSourceHelper.getMessage("notification.subscription.alerts.duplicate"));
            }
        });
    }

    private NotificationSubscriptionResponseDTO enrichAndMap(NotificationSubscription entity) {
        NotificationSubscriptionResponseDTO base = mapper.toResponseDto(entity);

        String userFullName = userRepository.findByIdAndDeletedFalse(entity.getUserId())
                .map(User::getFullName)
                .orElse("—");

        String subjectDisplayName;
        if (entity.getSubjectId() == null) {
            subjectDisplayName = "Todos (" + entity.getSubjectType().getDisplayName() + ")";
        } else {
            NextDueDateResolver resolver = resolvers.get(entity.getSubjectType());
            subjectDisplayName = resolver != null
                    ? resolver.resolveForId(entity.getSubjectId()).stream()
                              .findFirst()
                              .map(SubjectDueDateInfo::displayName)
                              .orElse("—")
                    : "—";
        }

        return new NotificationSubscriptionResponseDTO(
                base.id(), base.userId(), userFullName, base.subscribedByUserId(),
                base.subjectType(), base.subjectId(), subjectDisplayName,
                base.channels(), base.alerts(), base.active()
        );
    }

    private NotificationInboxItemDTO toInboxItem(NotificationLog log) {
        String subjectDisplayName = "—";
        if (log.getSubjectId() != null) {
            NextDueDateResolver resolver = resolvers.get(log.getSubjectType());
            if (resolver != null) {
                subjectDisplayName = resolver.resolveForId(log.getSubjectId()).stream()
                        .findFirst()
                        .map(SubjectDueDateInfo::displayName)
                        .orElse("—");
            }
        }

        int daysUntilDue = log.getDueDate() != null
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), log.getDueDate())
                : 0;

        return new NotificationInboxItemDTO(
                log.getId(),
                log.getSubjectType(),
                log.getSubjectType().getDisplayName(),
                log.getSubjectId(),
                subjectDisplayName,
                log.getDueDate(),
                daysUntilDue,
                log.getSentAt(),
                log.isRead()
        );
    }

    private NotificationLog getOwnedInboxLog(Long logId) {
        NotificationLog log = logRepository.findById(logId)
                .orElseThrow(() -> new NotificationSubscriptionNotFoundException(logId));
        Long currentUserId = getCurrentUser().getId();
        boolean owned = subscriptionRepository.findByIdAndDeletedFalse(log.getSubscriptionId())
                .map(sub -> sub.getUserId().equals(currentUserId))
                .orElse(false);
        if (!owned) {
            throw new NotificationSubscriptionNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.forbidden"));
        }
        return log;
    }
}
