package PSG.backEnd.model.dto.notification;

import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.ReminderRecurrence;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CustomReminderResponseDTO(
        Long id,
        Long userId,
        String userFullName,
        Long createdByUserId,
        String title,
        String description,
        LocalDate reminderDate,
        ReminderRecurrence recurrenceType,
        LocalDate recurrenceEndDate,
        Set<NotificationChannel> channels,
        List<NotificationSubscriptionResponseDTO.NotificationAlertResponseDTO> alerts,
        boolean active,
        Long subscriptionId
) {}
