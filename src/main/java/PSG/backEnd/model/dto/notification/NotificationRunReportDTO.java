package PSG.backEnd.model.dto.notification;

import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record NotificationRunReportDTO(
        LocalDate today,
        boolean dryRun,
        boolean force,
        int subscriptionsEvaluated,
        int subjectsEvaluated,
        int dispatched,
        List<Item> items
) {
    public record Item(
            Long subscriptionId,
            NotificationSubjectType subjectType,
            Long subjectId,
            String subjectDisplayName,
            LocalDate dueDate,
            Integer daysBeforeAlert,
            LocalDate triggerDate,
            Set<NotificationChannel> channels,
            String outcome
    ) {}
}
