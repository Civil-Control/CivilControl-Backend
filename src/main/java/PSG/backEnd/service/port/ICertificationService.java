package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.contracts.CertificationDTO;
import PSG.backEnd.model.dto.contracts.CertificationFilterDTO;
import PSG.backEnd.model.dto.contracts.CertificationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICertificationService {

    CertificationResponseDTO createCertification(CertificationDTO dto);

    Page<CertificationResponseDTO> getAllCertifications(CertificationFilterDTO filterDTO, Pageable pageable);

    CertificationResponseDTO getCertificationById(Long id);

    CertificationResponseDTO updateCertification(Long id, CertificationDTO dto);

    CertificationResponseDTO markAsCobrado(Long id);

    void deleteCertification(Long id);

    /**
     * Links a certification to a sales-document from the sales-document side (mirrors the
     * existing link performed via {@code createCertification}/{@code updateCertification}'s
     * {@code salesDocumentId} field, which is driven from the certification side). Bumps the
     * certification's status to {@code FACTURADO} (or {@code COBRADO} if the invoice is already
     * paid) — same rule, same code path, either direction.
     */
    CertificationResponseDTO linkToSalesDocument(Long certificationId, Long salesDocumentId);

    /**
     * Clears the certification's sales-document link. Reverts status to {@code APROBADO} when it
     * was {@code FACTURADO}/{@code COBRADO}. Mirrors clearing {@code salesDocumentId} via
     * {@code updateCertification}.
     */
    CertificationResponseDTO unlinkFromSalesDocument(Long certificationId);
}
