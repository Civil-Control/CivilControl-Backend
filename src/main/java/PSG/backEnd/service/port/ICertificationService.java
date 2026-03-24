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

    void deleteCertification(Long id);
}
