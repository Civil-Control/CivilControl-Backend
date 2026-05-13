package PSG.backEnd.service.notification.resolver;

import PSG.backEnd.model.entity.contracts.WorkContract;
import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.enums.contracts.WorkContractStatus;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.repository.WorkContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkContractDueDateResolver implements NextDueDateResolver {

    private final WorkContractRepository workContractRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.WORK_CONTRACT; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long contractId) {
        return workContractRepository.findByIdAndDeletedFalse(contractId)
                .filter(c -> c.getStatus() == WorkContractStatus.ACTIVO && c.getEndDate() != null)
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getEndDate()))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return workContractRepository.findAllByDeletedFalseAndStatus(WorkContractStatus.ACTIVO).stream()
                .filter(c -> c.getEndDate() != null)
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getEndDate()))
                .toList();
    }

    private String buildDisplayName(WorkContract c) {
        return "Contrato " + c.getContractNumber() + " — " + c.getClient().getBusinessName();
    }
}
