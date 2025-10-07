package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentFilterDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ILicencePlatePaymentService {
    LicencePlatePaymentResponseDTO createLicencePlatePayment(LicencePlatePaymentDTO licencePlatePaymentDTO);
    LicencePlatePaymentResponseDTO getLicencePlatePaymentById(Long id);
    LicencePlatePaymentResponseDTO updateLicencePlatePayment(Long id, LicencePlatePaymentDTO licencePlatePaymentDTO);
    void deleteLicencePlatePayment(Long id);
    Page<LicencePlatePaymentResponseDTO> getAllLicencePlatePayments(LicencePlatePaymentFilterDTO filterDTO, Pageable pageable);
}

