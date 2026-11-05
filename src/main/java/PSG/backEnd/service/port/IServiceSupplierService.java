package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IServiceSupplierService {
    ServiceSupplierResponseDTO createServiceSupplier(ServiceSupplierDTO serviceSupplierDTO);
    ServiceSupplierResponseDTO getServiceSupplierById(Long id);
    Page<ServiceSupplierResponseDTO> getAllServiceSuppliers(ServiceSupplierFilterDTO filterDTO, Pageable pageable);
    ServiceSupplierResponseDTO updateServiceSupplier(Long id, ServiceSupplierDTO serviceSupplierDTO);
    void deleteServiceSupplier(Long id);
}

