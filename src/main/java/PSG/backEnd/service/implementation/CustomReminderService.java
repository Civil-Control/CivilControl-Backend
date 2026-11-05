package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.notification.CustomReminderNotFoundException;
import PSG.backEnd.exception.notification.CustomReminderNotValidException;
import PSG.backEnd.exception.user.UserNotFoundException;
import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.notification.CustomReminderDTO;
import PSG.backEnd.model.dto.notification.CustomReminderResponseDTO;
import PSG.backEnd.model.dto.notification.NotificationAlertDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionResponseDTO;
import PSG.backEnd.model.entity.notification.CustomReminder;
import PSG.backEnd.model.entity.notification.NotificationAlert;
import PSG.backEnd.model.entity.notification.NotificationSubscription;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.security.UserDeletedEvent;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.repository.CustomReminderRepository;
import PSG.backEnd.repository.NotificationSubscriptionRepository;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.service.port.ICustomReminderService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CustomReminderService implements ICustomReminderService {

    private final CustomReminderRepository reminderRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public CustomReminderResponseDTO createReminder(CustomReminderDTO dto) {
        User currentUser = getCurrentUser();
        Long currentUserId = currentUser.getId();

        checkCreatePermission(dto.userId(), currentUserId);

        User targetUser = userRepository.findByIdAndDeletedFalse(dto.userId())
                .orElseThrow(() -> new UserNotFoundException(dto.userId()));

        validateChannelVerification(dto, targetUser);
        validateDates(dto);
        validateUniqueAlertDays(dto);

        CustomReminder reminder = CustomReminder.builder()
                .userId(dto.userId())
                .createdByUserId(currentUserId)
                .title(dto.title())
                .description(dto.description())
                .reminderDate(dto.reminderDate())
                .recurrenceType(dto.recurrenceType())
                .recurrenceEndDate(dto.recurrenceEndDate())
                .active(dto.active())
                .build();

        CustomReminder savedReminder = reminderRepository.save(reminder);

        NotificationSubscription subscription = buildSubscription(dto, savedReminder.getId(), currentUserId);
        NotificationSubscription savedSubscription = subscriptionRepository.save(subscription);

        savedReminder.setSubscriptionId(savedSubscription.getId());
        reminderRepository.save(savedReminder);

        return toResponseDto(savedReminder, savedSubscription, targetUser.getFullName());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomReminderResponseDTO getReminderById(Long id) {
        CustomReminder reminder = findOrThrow(id);
        checkOwnerOrAdmin(reminder);
        return enrichAndMap(reminder);
    }

    @Override
    public CustomReminderResponseDTO updateReminder(Long id, CustomReminderDTO dto) {
        CustomReminder reminder = findOrThrow(id);
        checkOwnerOrAdmin(reminder);
        validateDates(dto);
        validateUniqueAlertDays(dto);

        reminder.setTitle(dto.title());
        reminder.setDescription(dto.description());
        reminder.setReminderDate(dto.reminderDate());
        reminder.setRecurrenceType(dto.recurrenceType());
        reminder.setRecurrenceEndDate(dto.recurrenceEndDate());
        reminder.setActive(dto.active());
        reminderRepository.save(reminder);

        if (reminder.getSubscriptionId() != null) {
            subscriptionRepository.findByIdAndDeletedFalse(reminder.getSubscriptionId())
                    .ifPresent(sub -> {
                        User targetUser = userRepository.findByIdAndDeletedFalse(reminder.getUserId()).orElse(null);
                        if (targetUser != null) {
                            validateChannelVerificationForUpdate(dto, targetUser);
                        }
                        sub.setChannels(dto.channels());
                        sub.setActive(dto.active());
                        sub.getAlerts().clear();
                        dto.alerts().forEach(a -> {
                            NotificationAlert alert = NotificationAlert.builder()
                                    .subscription(sub)
                                    .daysBeforeAlert(a.daysBeforeAlert())
                                    .active(a.active())
                                    .build();
                            sub.getAlerts().add(alert);
                        });
                        subscriptionRepository.save(sub);
                    });
        }

        return enrichAndMap(reminder);
    }

    @Override
    public void deleteReminder(Long id) {
        CustomReminder reminder = findOrThrow(id);
        checkOwnerOrAdmin(reminder);

        reminder.setDeleted(true);
        reminderRepository.save(reminder);

        if (reminder.getSubscriptionId() != null) {
            subscriptionRepository.findByIdAndDeletedFalse(reminder.getSubscriptionId())
                    .ifPresent(sub -> {
                        sub.setDeleted(true);
                        subscriptionRepository.save(sub);
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomReminderResponseDTO> getMyReminders() {
        Long currentUserId = getCurrentUser().getId();
        return reminderRepository.findAllByUserIdAndDeletedFalse(currentUserId).stream()
                .map(this::enrichAndMap)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomReminderResponseDTO> getAllReminders() {
        if (!hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
            throw new CustomReminderNotValidException(
                    messageSourceHelper.getMessage("custom.reminder.forbidden"));
        }
        return reminderRepository.findAllByDeletedFalse().stream()
                .map(this::enrichAndMap)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomReminderResponseDTO> getUserReminders(Long userId) {
        if (!hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
            throw new CustomReminderNotValidException(
                    messageSourceHelper.getMessage("custom.reminder.forbidden"));
        }
        return reminderRepository.findAllByUserIdAndDeletedFalse(userId).stream()
                .map(this::enrichAndMap)
                .toList();
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        reminderRepository.markDeletedByUserId(event.userId());
        log.info("Soft-deleted reminders for userId={}", event.userId());
    }

    // ==================== Private helpers ====================

    private CustomReminder findOrThrow(Long id) {
        return reminderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CustomReminderNotFoundException(id));
    }

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
                throw new CustomReminderNotValidException(
                        messageSourceHelper.getMessage("custom.reminder.forbidden"));
            }
        } else {
            if (!hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
                throw new CustomReminderNotValidException(
                        messageSourceHelper.getMessage("custom.reminder.forbidden"));
            }
        }
    }

    private void checkOwnerOrAdmin(CustomReminder reminder) {
        Long currentUserId = getCurrentUser().getId();
        if (!reminder.getUserId().equals(currentUserId)
                && !hasAuthority(AppPermissions.NOTIFICATION_ASSIGN_OTHERS)) {
            throw new CustomReminderNotValidException(
                    messageSourceHelper.getMessage("custom.reminder.forbidden"));
        }
    }

    private void validateChannelVerification(CustomReminderDTO dto, User targetUser) {
        if (dto.channels() == null) return;
        if (dto.channels().contains(NotificationChannel.EMAIL)
                && !Boolean.TRUE.equals(targetUser.getEmailVerified())) {
            throw new CustomReminderNotValidException(
                    messageSourceHelper.getMessage("notification.subscription.channel.emailNotVerified"));
        }
    }

    private void validateChannelVerificationForUpdate(CustomReminderDTO dto, User targetUser) {
        validateChannelVerification(dto, targetUser);
    }

    private void validateDates(CustomReminderDTO dto) {
        if (dto.reminderDate() == null) return;
        if (dto.recurrenceEndDate() != null
                && !dto.recurrenceEndDate().isAfter(dto.reminderDate())) {
            throw new CustomReminderNotValidException(
                    messageSourceHelper.getMessage("custom.reminder.endDateBeforeStart"));
        }
    }

    private void validateUniqueAlertDays(CustomReminderDTO dto) {
        if (dto.alerts() == null) return;
        Set<Integer> seen = new HashSet<>();
        dto.alerts().forEach(a -> {
            if (!seen.add(a.daysBeforeAlert())) {
                throw new CustomReminderNotValidException(
                        messageSourceHelper.getMessage("notification.subscription.alerts.duplicate"));
            }
        });
    }

    private NotificationSubscription buildSubscription(CustomReminderDTO dto, Long reminderId, Long currentUserId) {
        NotificationSubscription sub = NotificationSubscription.builder()
                .userId(dto.userId())
                .subscribedByUserId(currentUserId)
                .subjectType(NotificationSubjectType.CUSTOM_REMINDER)
                .subjectId(reminderId)
                .channels(dto.channels())
                .active(dto.active())
                .build();

        List<NotificationAlertDTO> alertDtos = (dto.alerts() == null || dto.alerts().isEmpty())
                ? List.of(new NotificationAlertDTO(0, true))
                : dto.alerts();

        alertDtos.forEach(a -> {
            NotificationAlert alert = NotificationAlert.builder()
                    .subscription(sub)
                    .daysBeforeAlert(a.daysBeforeAlert())
                    .active(a.active())
                    .build();
            sub.getAlerts().add(alert);
        });

        return sub;
    }

    private CustomReminderResponseDTO enrichAndMap(CustomReminder reminder) {
        String userFullName = userRepository.findByIdAndDeletedFalse(reminder.getUserId())
                .map(User::getFullName)
                .orElse("—");

        NotificationSubscription sub = reminder.getSubscriptionId() != null
                ? subscriptionRepository.findByIdAndDeletedFalse(reminder.getSubscriptionId()).orElse(null)
                : null;

        return toResponseDto(reminder, sub, userFullName);
    }

    private CustomReminderResponseDTO toResponseDto(CustomReminder reminder,
                                                     NotificationSubscription sub,
                                                     String userFullName) {
        var alerts = sub != null
                ? sub.getAlerts().stream()
                        .map(a -> new NotificationSubscriptionResponseDTO.NotificationAlertResponseDTO(
                                a.getId(), a.getDaysBeforeAlert(), a.isActive()))
                        .toList()
                : List.<NotificationSubscriptionResponseDTO.NotificationAlertResponseDTO>of();

        Set<NotificationChannel> channels = sub != null ? sub.getChannels() : Set.of();

        return new CustomReminderResponseDTO(
                reminder.getId(),
                reminder.getUserId(),
                userFullName,
                reminder.getCreatedByUserId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getReminderDate(),
                reminder.getRecurrenceType(),
                reminder.getRecurrenceEndDate(),
                channels,
                alerts,
                reminder.isActive(),
                reminder.getSubscriptionId()
        );
    }
}
