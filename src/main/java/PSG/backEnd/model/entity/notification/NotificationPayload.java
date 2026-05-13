package PSG.backEnd.model.entity.notification;

import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;

import java.time.LocalDate;
import java.util.Map;

public record NotificationPayload(
        NotificationSubjectType subjectType,
        Long subjectId,
        String subjectDisplayName,
        LocalDate dueDate,
        int daysUntilDue,
        Long userId,
        Map<NotificationChannel, String> channelAddresses
) {}
