package PSG.backEnd.service.notification.resolver;

import PSG.backEnd.model.entity.notification.CustomReminder;
import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.model.enums.notification.ReminderRecurrence;
import PSG.backEnd.repository.CustomReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomReminderDueDateResolver implements NextDueDateResolver {

    private final CustomReminderRepository reminderRepository;

    @Override
    public NotificationSubjectType getSubjectType() {
        return NotificationSubjectType.CUSTOM_REMINDER;
    }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long reminderId) {
        return reminderRepository.findByIdAndDeletedFalse(reminderId)
                .filter(CustomReminder::isActive)
                .map(r -> {
                    LocalDate next = computeNextOccurrence(r);
                    if (next == null) return null;
                    return new SubjectDueDateInfo(r.getId(), r.getTitle(), next, r.getDescription());
                })
                .filter(Objects::nonNull)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return List.of();
    }

    private LocalDate computeNextOccurrence(CustomReminder reminder) {
        LocalDate today = LocalDate.now();
        LocalDate anchor = reminder.getReminderDate();

        if (reminder.getRecurrenceType() == ReminderRecurrence.NONE) {
            return anchor;
        }

        LocalDate next = anchor;
        while (next.isBefore(today)) {
            next = advance(next, reminder.getRecurrenceType());
        }

        LocalDate endDate = reminder.getRecurrenceEndDate();
        if (endDate != null && next.isAfter(endDate)) {
            return null;
        }

        return next;
    }

    private LocalDate advance(LocalDate date, ReminderRecurrence type) {
        return switch (type) {
            case DAILY   -> date.plusDays(1);
            case WEEKLY  -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY  -> date.plusYears(1);
            case NONE    -> throw new IllegalStateException("NONE should not be advanced");
        };
    }
}
