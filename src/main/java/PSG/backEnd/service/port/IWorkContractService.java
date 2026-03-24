package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.contracts.WorkContractDTO;
import PSG.backEnd.model.dto.contracts.WorkContractFilterDTO;
import PSG.backEnd.model.dto.contracts.WorkContractResponseDTO;
import PSG.backEnd.model.dto.contracts.WorkContractStatsDTO;
import PSG.backEnd.model.entity.contracts.WorkContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IWorkContractService {

    WorkContractResponseDTO createWorkContract(WorkContractDTO dto);

    Page<WorkContractResponseDTO> getAllWorkContracts(WorkContractFilterDTO filterDTO, Pageable pageable);

    WorkContractResponseDTO getWorkContractById(Long id);

    WorkContractStatsDTO getWorkContractStats(Long id);

    WorkContractResponseDTO updateWorkContract(Long id, WorkContractDTO dto);

    void deleteWorkContract(Long id);

    WorkContract getEntityById(Long id);
}
