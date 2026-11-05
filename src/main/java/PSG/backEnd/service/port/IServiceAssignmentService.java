package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IServiceAssignmentService {
    ServiceAssignmentResponseDTO createServiceAssignment(ServiceAssignmentDTO dto);
    ServiceAssignmentResponseDTO getServiceAssignmentById(Long id);
    Page<ServiceAssignmentResponseDTO> getAllServiceAssignments(ServiceAssignmentFilterDTO filterDTO, Pageable pageable);
    ServiceAssignmentResponseDTO updateServiceAssignment(Long id, ServiceAssignmentDTO dto);
    void deleteServiceAssignment(Long id);
}
