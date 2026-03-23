package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.vehicle.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IRepairOrderService {

    RepairOrderResponseDTO createRepairOrder(RepairOrderRequestDTO dto);

    RepairOrderResponseDTO getRepairOrderById(Long id);

    Page<RepairOrderResponseDTO> getAllRepairOrders(RepairOrderFilterDTO filterDTO, Pageable pageable);

    Page<RepairOrderResponseDTO> getMyRepairOrders(RepairOrderFilterDTO filterDTO, Pageable pageable);

    RepairOrderResponseDTO updateRepairOrder(Long id, RepairOrderRequestDTO dto);

    void deleteRepairOrder(Long id);

    RepairOrderResponseDTO changeStatus(Long id, RepairOrderStatusDTO statusDTO);

    RepairOrderResponseDTO completeRepairOrder(Long id, RepairOrderCompleteDTO completeDTO);
}
