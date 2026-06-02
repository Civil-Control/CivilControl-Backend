package PSG.backEnd.model.entity.notification;

import java.time.LocalDate;

public record SubjectDueDateInfo(
        Long subjectId,
        String displayName,
        LocalDate dueDate,
        String description
) {
    public SubjectDueDateInfo(Long subjectId, String displayName, LocalDate dueDate) {
        this(subjectId, displayName, dueDate, null);
    }
}
