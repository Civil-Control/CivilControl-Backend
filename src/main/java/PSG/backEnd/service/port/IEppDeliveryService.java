package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.EppDeliveryBatchDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryFilterDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IEppDeliveryService {
    EppDeliveryResponseDTO createEppDelivery(EppDeliveryDTO eppDeliveryDTO);
    List<EppDeliveryResponseDTO> createBatchEppDeliveries(EppDeliveryBatchDTO batchDTO);
    EppDeliveryResponseDTO getEppDeliveryById(Long id);
    EppDeliveryResponseDTO updateEppDelivery(Long id, EppDeliveryDTO eppDeliveryDTO);
    void deleteEppDelivery(Long id);
    Page<EppDeliveryResponseDTO> getAllEppDeliveries(EppDeliveryFilterDTO filterDTO, Pageable pageable);
}

