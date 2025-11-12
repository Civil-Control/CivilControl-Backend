package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.EppDeliveryDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryFilterDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IEppDeliveryService {
    EppDeliveryResponseDTO createEppDelivery(EppDeliveryDTO eppDeliveryDTO);
    EppDeliveryResponseDTO getEppDeliveryById(Long id);
    EppDeliveryResponseDTO updateEppDelivery(Long id, EppDeliveryDTO eppDeliveryDTO);
    void deleteEppDelivery(Long id);
    Page<EppDeliveryResponseDTO> getAllEppDeliveries(EppDeliveryFilterDTO filterDTO, Pageable pageable);
}

