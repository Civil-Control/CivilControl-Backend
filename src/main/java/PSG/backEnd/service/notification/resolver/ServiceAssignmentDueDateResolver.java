package PSG.backEnd.service.notification.resolver;

import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.serviceSupplier.SpecificDueDate;
import PSG.backEnd.model.enums.Periodicity;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceAssignmentDueDateResolver implements NextDueDateResolver {

    private final ServiceAssignmentRepository serviceAssignmentRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.SERVICE_ASSIGNMENT; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long assignmentId) {
        return serviceAssignmentRepository.findByIdAndDeletedFalse(assignmentId)
                .flatMap(this::computeNextDueDate)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return serviceAssignmentRepository.findByDeletedFalse().stream()
                .flatMap(a -> computeNextDueDate(a).stream())
                .toList();
    }

    private Optional<SubjectDueDateInfo> computeNextDueDate(ServiceAssignment assignment) {
        LocalDate today = LocalDate.now();

        if (assignment.getPeriodicity() == Periodicity.IRREGULAR) {
            return assignment.getSpecificDueDates().stream()
                    .map(SpecificDueDate::getDueDate)
                    .filter(d -> !d.isBefore(today))
                    .min(Comparator.naturalOrder())
                    .map(d -> new SubjectDueDateInfo(assignment.getId(), buildDisplayName(assignment), d));
        }

        if (assignment.getEstimatedDueDay() == null || assignment.getPeriodicity() == null) {
            return Optional.empty();
        }

        int intervalMonths = switch (assignment.getPeriodicity()) {
            case MENSUAL    -> 1;
            case BIMESTRAL  -> 2;
            case TRIMESTRAL -> 3;
            case SEMESTRAL  -> 6;
            case ANUAL      -> 12;
            default         -> 1;
        };

        int dueDay = assignment.getEstimatedDueDay();
        LocalDate candidate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));
        if (candidate.isBefore(today)) {
            LocalDate next = candidate.plusMonths(intervalMonths);
            candidate = next.withDayOfMonth(Math.min(dueDay, next.lengthOfMonth()));
        }
        return Optional.of(new SubjectDueDateInfo(assignment.getId(), buildDisplayName(assignment), candidate));
    }

    private String buildDisplayName(ServiceAssignment a) {
        return a.getServiceSupplier().getSupplier().getLegalName() + " — " + a.getServiceType().name();
    }
}
