package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentBatchDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentFilterDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ILicencePlatePaymentService {
    LicencePlatePaymentResponseDTO createLicencePlatePayment(LicencePlatePaymentDTO licencePlatePaymentDTO);
    List<LicencePlatePaymentResponseDTO> createBatchLicencePlatePayments(LicencePlatePaymentBatchDTO batchDTO);
    LicencePlatePaymentResponseDTO getLicencePlatePaymentById(Long id);
    LicencePlatePaymentResponseDTO updateLicencePlatePayment(Long id, LicencePlatePaymentDTO licencePlatePaymentDTO);
    void deleteLicencePlatePayment(Long id);
    Page<LicencePlatePaymentResponseDTO> getAllLicencePlatePayments(LicencePlatePaymentFilterDTO filterDTO, Pageable pageable);
}

