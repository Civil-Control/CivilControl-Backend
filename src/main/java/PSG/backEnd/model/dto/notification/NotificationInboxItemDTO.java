package PSG.backEnd.model.dto.notification;

import PSG.backEnd.model.enums.notification.NotificationSubjectType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificationInboxItemDTO(
        Long logId,
        NotificationSubjectType subjectType,
        String subjectTypeDisplayName,
        Long subjectId,
        String subjectDisplayName,
        LocalDate dueDate,
        int daysUntilDue,
        LocalDateTime sentAt,
        boolean read
) {}
