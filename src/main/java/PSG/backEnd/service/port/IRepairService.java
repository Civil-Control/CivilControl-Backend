package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairFilterDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IRepairService {
    RepairResponseDTO createRepair(RepairDTO repairDTO);
    RepairResponseDTO getRepairById(Long id);
    RepairResponseDTO updateRepair(Long id, RepairDTO repairDTO);
    void deleteRepair(Long id);
    Page<RepairResponseDTO> getAllRepairs(RepairFilterDTO filterDTO, Pageable pageable);
    RepairResponseDTO linkToDocument(Long repairId, Long documentId);
    RepairResponseDTO unlinkFromDocument(Long repairId);
}

