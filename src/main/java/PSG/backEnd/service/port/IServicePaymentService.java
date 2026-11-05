package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IServicePaymentService {
    ServicePaymentResponseDTO createServicePayment(ServicePaymentDTO servicePaymentDTO);
    ServicePaymentResponseDTO getServicePaymentById(Long id);
    Page<ServicePaymentResponseDTO> getAllServicePayments(ServicePaymentFilterDTO filterDTO, Pageable pageable);
    ServicePaymentResponseDTO updateServicePayment(Long id, ServicePaymentDTO servicePaymentDTO);
    void deleteServicePayment(Long id);
}

