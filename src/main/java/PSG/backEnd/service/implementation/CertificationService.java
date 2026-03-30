package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.contracts.CertificationNotFoundException;
import PSG.backEnd.exception.contracts.WorkContractNotFoundException;
import PSG.backEnd.model.dto.contracts.CertificationDTO;
import PSG.backEnd.model.dto.contracts.CertificationFilterDTO;
import PSG.backEnd.model.dto.contracts.CertificationResponseDTO;
import PSG.backEnd.model.entity.contracts.Certification;
import PSG.backEnd.model.entity.contracts.WorkContract;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.mapper.CertificationMapper;
import PSG.backEnd.repository.CertificationRepository;
import PSG.backEnd.repository.SalesDocumentRepository;
import PSG.backEnd.repository.WorkContractRepository;
import PSG.backEnd.service.port.ICertificationService;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CertificationService implements ICertificationService {

    private final CertificationRepository certificationRepository;
    private final WorkContractRepository workContractRepository;
    private final SalesDocumentRepository salesDocumentRepository;
    private final CertificationMapper certificationMapper;

    @Override
    @Transactional
    public CertificationResponseDTO createCertification(CertificationDTO dto) {
        Long tenantId = TenantContext.getCurrentTenant();

        WorkContract contract = workContractRepository.findByIdAndDeletedFalse(dto.workContractId())
                .orElseThrow(() -> new WorkContractNotFoundException(dto.workContractId()));

        int nextNumber = certificationRepository.findMaxCertificationNumber(dto.workContractId(), tenantId) + 1;

        Certification entity = certificationMapper.toEntity(dto);
        entity.setContract(contract);
        entity.setCertificationNumber(nextNumber);
        entity.setDeleted(false);

        SalesDocument salesDocument = null;
        if (dto.salesDocumentId() != null) {
            salesDocument = salesDocumentRepository.findByIdAndDeletedFalse(dto.salesDocumentId())
                    .orElseThrow(() -> new RuntimeException("SalesDocument not found: " + dto.salesDocumentId()));
            entity.setSalesDocument(salesDocument);
            entity.setStatus(Boolean.TRUE.equals(salesDocument.getPaid())
                    ? CertificationStatus.COBRADO
                    : CertificationStatus.FACTURADO);
        } else {
            entity.setSalesDocument(null);
        }

        Certification saved = certificationRepository.save(entity);
        CertificationResponseDTO response = certificationMapper.toResponseDto(saved);

        if (salesDocument != null) {
            return buildWithLabel(response, salesDocument);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificationResponseDTO> getAllCertifications(CertificationFilterDTO filterDTO, Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenant();
        return certificationRepository.findAllWithFilters(
                tenantId,
                filterDTO.workContractId(),
                filterDTO.clientId(),
                filterDTO.status(),
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.hasInvoice(),
                filterDTO.salesDocumentId(),
                pageable
        ).map(c -> {
            CertificationResponseDTO dto2 = certificationMapper.toResponseDto(c);
            if (c.getSalesDocument() != null) {
                return buildWithLabel(dto2, c.getSalesDocument());
            }
            return dto2;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CertificationResponseDTO getCertificationById(Long id) {
        Certification c = certificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CertificationNotFoundException(id));
        CertificationResponseDTO dto = certificationMapper.toResponseDto(c);
        if (c.getSalesDocument() != null) {
            return buildWithLabel(dto, c.getSalesDocument());
        }
        return dto;
    }

    @Override
    @Transactional
    public CertificationResponseDTO updateCertification(Long id, CertificationDTO dto) {
        Certification existing = certificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CertificationNotFoundException(id));

        certificationMapper.partialUpdate(dto, existing);

        if (dto.workContractId() != null) {
            WorkContract contract = workContractRepository.findByIdAndDeletedFalse(dto.workContractId())
                    .orElseThrow(() -> new WorkContractNotFoundException(dto.workContractId()));
            existing.setContract(contract);
        }

        SalesDocument salesDocument = null;
        if (dto.salesDocumentId() != null) {
            salesDocument = salesDocumentRepository.findByIdAndDeletedFalse(dto.salesDocumentId())
                    .orElseThrow(() -> new RuntimeException("SalesDocument not found: " + dto.salesDocumentId()));
            existing.setSalesDocument(salesDocument);
            existing.setStatus(Boolean.TRUE.equals(salesDocument.getPaid())
                    ? CertificationStatus.COBRADO
                    : CertificationStatus.FACTURADO);
        } else {
            existing.setSalesDocument(null);
            // If invoice removed and status was auto-set, revert to APROBADO
            if (existing.getStatus() == CertificationStatus.FACTURADO
                    || existing.getStatus() == CertificationStatus.COBRADO) {
                existing.setStatus(CertificationStatus.APROBADO);
            }
        }

        Certification saved = certificationRepository.save(existing);
        CertificationResponseDTO response = certificationMapper.toResponseDto(saved);
        if (salesDocument != null) {
            return buildWithLabel(response, salesDocument);
        }
        return response;
    }

    @Override
    @Transactional
    public void deleteCertification(Long id) {
        Certification existing = certificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CertificationNotFoundException(id));
        existing.setDeleted(true);
        certificationRepository.save(existing);
    }

    @Override
    @Transactional
    public CertificationResponseDTO markAsCobrado(Long id) {
        Certification existing = certificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CertificationNotFoundException(id));
        existing.setStatus(CertificationStatus.COBRADO);
        Certification saved = certificationRepository.save(existing);
        CertificationResponseDTO response = certificationMapper.toResponseDto(saved);
        if (saved.getSalesDocument() != null) {
            return buildWithLabel(response, saved.getSalesDocument());
        }
        return response;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private CertificationResponseDTO buildWithLabel(CertificationResponseDTO dto, SalesDocument sd) {
        String label = buildSalesDocumentLabel(sd);
        return new CertificationResponseDTO(
                dto.id(),
                dto.certificationNumber(),
                dto.workContractId(),
                dto.contractNumber(),
                dto.certificationDate(),
                dto.certifiedAmount(),
                dto.salesDocumentId(),
                label,
                dto.status(),
                dto.comment(),
                dto.deleted()
        );
    }

    private String buildSalesDocumentLabel(SalesDocument sd) {
        if (sd == null) return null;
        String type  = sd.getDocumentType() != null ? sd.getDocumentType().name() : "";
        String branch = sd.getBranchCode()    != null ? sd.getBranchCode()         : "00000";
        String number = sd.getDocumentNumber() != null ? sd.getDocumentNumber()    : "00000000";
        String amount = sd.getTotal() != null
                ? NumberFormat.getNumberInstance(new Locale("es", "AR"))
                              .format(sd.getTotal())
                : "0";
        return type + " " + branch + "-" + number + " — $" + amount;
    }
}
