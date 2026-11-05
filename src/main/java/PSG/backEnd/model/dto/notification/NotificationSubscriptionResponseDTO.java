package PSG.backEnd.model.dto.notification;

import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;

import java.util.List;
import java.util.Set;

public record NotificationSubscriptionResponseDTO(
        Long id,
        Long userId,
        String userFullName,
        Long subscribedByUserId,
        NotificationSubjectType subjectType,
        Long subjectId,
        String subjectDisplayName,
        Set<NotificationChannel> channels,
        List<NotificationAlertResponseDTO> alerts,
        boolean active
) {
    public record NotificationAlertResponseDTO(
            Long id,
            Integer daysBeforeAlert,
            boolean active
    ) {}
}
