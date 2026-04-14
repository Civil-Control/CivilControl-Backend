package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.client.ClientNotFoundException;
import PSG.backEnd.exception.contracts.WorkContractAlreadyExistsException;
import PSG.backEnd.exception.contracts.WorkContractNotFoundException;
import PSG.backEnd.model.dto.contracts.*;
import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.contracts.WorkContract;
import PSG.backEnd.model.mapper.WorkContractMapper;
import PSG.backEnd.repository.ClientRepository;
import PSG.backEnd.repository.CertificationRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ProjectAreaTaskRepository;
import PSG.backEnd.repository.WorkContractRepository;
import PSG.backEnd.service.port.IWorkContractService;
import PSG.backEnd.service.util.MessageSourceHelper;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WorkContractService implements IWorkContractService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final WorkContractRepository workContractRepository;
    private final CertificationRepository certificationRepository;
    private final ClientRepository clientRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaTaskRepository projectAreaTaskRepository;
    private final WorkContractMapper workContractMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public WorkContractResponseDTO createWorkContract(WorkContractDTO dto) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (workContractRepository.existsByContractNumberAndTenantIdAndDeletedFalse(dto.contractNumber(), tenantId)) {
            throw new WorkContractAlreadyExistsException(
                    messageSourceHelper.getMessage("workContract.contractNumber.alreadyExists", dto.contractNumber()));
        }

        WorkContract entity = workContractMapper.toEntity(dto);
        entity.setDeleted(false);

        Client client = clientRepository.findByIdAndDeletedFalse(dto.clientId())
                .orElseThrow(() -> new ClientNotFoundException(dto.clientId()));
        entity.setClient(client);

        if (dto.projectAreaId() != null) {
            ProjectArea pa = projectAreaRepository.findById(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException("ProjectArea not found: " + dto.projectAreaId()));
            entity.setProjectArea(pa);
        } else {
            entity.setProjectArea(null);
        }

        if (dto.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(dto.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException("ProjectAreaTask not found: " + dto.projectAreaTaskId()));
            entity.setProjectAreaTask(task);
        } else {
            entity.setProjectAreaTask(null);
        }

        return workContractMapper.toResponseDto(workContractRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkContractResponseDTO> getAllWorkContracts(WorkContractFilterDTO filterDTO, Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenant();
        return workContractRepository.findAllWithFilters(
                tenantId,
                filterDTO.clientId(),
                filterDTO.contractNumber(),
                filterDTO.projectAreaId(),
                filterDTO.status(),
                filterDTO.currency(),
                filterDTO.contractDateFrom(),
                filterDTO.contractDateTo(),
                filterDTO.minContractedAmount(),
                filterDTO.maxContractedAmount(),
                filterDTO.search(),
                pageable
        ).map(workContractMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkContractResponseDTO getWorkContractById(Long id) {
        return workContractRepository.findByIdAndDeletedFalse(id)
                .map(workContractMapper::toResponseDto)
                .orElseThrow(() -> new WorkContractNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkContractStatsDTO getWorkContractStats(Long id) {
        WorkContract contract = workContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new WorkContractNotFoundException(id));

        Long tenantId = TenantContext.getCurrentTenant();
        CertificationStatsProjection stats = certificationRepository.getStatsByContractId(id, tenantId);

        BigDecimal contractedAmount = contract.getContractedAmount();
        BigDecimal totalCertified  = stats != null ? stats.getTotalCertified()  : BigDecimal.ZERO;
        BigDecimal totalInvoiced   = stats != null ? stats.getTotalInvoiced()   : BigDecimal.ZERO;
        BigDecimal totalCollected  = stats != null ? stats.getTotalCollected()  : BigDecimal.ZERO;

        BigDecimal pendingToCertify  = contractedAmount.subtract(totalCertified).max(BigDecimal.ZERO);
        BigDecimal pendingToInvoice  = totalCertified.subtract(totalInvoiced).max(BigDecimal.ZERO);
        BigDecimal pendingToCollect  = totalInvoiced.subtract(totalCollected).max(BigDecimal.ZERO);

        BigDecimal certificationProgress = safePercent(totalCertified, contractedAmount);
        BigDecimal invoicingProgress     = safePercent(totalInvoiced,   totalCertified);
        BigDecimal collectionProgress    = safePercent(totalCollected,  totalInvoiced);

        return new WorkContractStatsDTO(
                contractedAmount,
                totalCertified, pendingToCertify, certificationProgress,
                totalInvoiced,  pendingToInvoice, invoicingProgress,
                totalCollected, pendingToCollect, collectionProgress
        );
    }

    @Override
    @Transactional
    public WorkContractResponseDTO updateWorkContract(Long id, WorkContractDTO dto) {
        Long tenantId = TenantContext.getCurrentTenant();
        WorkContract existing = workContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new WorkContractNotFoundException(id));

        if (dto.contractNumber() != null &&
            !dto.contractNumber().equals(existing.getContractNumber()) &&
            workContractRepository.existsByContractNumberAndTenantIdAndDeletedFalseAndIdNot(
                    dto.contractNumber(), tenantId, id)) {
            throw new WorkContractAlreadyExistsException(
                    messageSourceHelper.getMessage("workContract.contractNumber.alreadyExists", dto.contractNumber()));
        }

        workContractMapper.partialUpdate(dto, existing);

        if (dto.clientId() != null) {
            Client client = clientRepository.findByIdAndDeletedFalse(dto.clientId())
                    .orElseThrow(() -> new ClientNotFoundException(dto.clientId()));
            existing.setClient(client);
        }

        if (dto.projectAreaId() != null) {
            ProjectArea pa = projectAreaRepository.findById(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException("ProjectArea not found: " + dto.projectAreaId()));
            existing.setProjectArea(pa);
        } else if (dto.projectAreaId() == null && dto.contractNumber() != null) {
            existing.setProjectArea(null);
        }

        if (dto.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(dto.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException("ProjectAreaTask not found: " + dto.projectAreaTaskId()));
            existing.setProjectAreaTask(task);
        } else if (dto.projectAreaTaskId() == null && dto.contractNumber() != null) {
            existing.setProjectAreaTask(null);
        }

        return workContractMapper.toResponseDto(workContractRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteWorkContract(Long id) {
        WorkContract existing = workContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new WorkContractNotFoundException(id));
        existing.setDeleted(true);
        workContractRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkContract getEntityById(Long id) {
        return workContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new WorkContractNotFoundException(id));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
