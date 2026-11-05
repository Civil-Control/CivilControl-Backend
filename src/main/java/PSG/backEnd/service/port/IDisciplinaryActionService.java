package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.DisciplinaryActionDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionFilterDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IDisciplinaryActionService {
    DisciplinaryActionResponseDTO createDisciplinaryAction(DisciplinaryActionDTO disciplinaryActionDTO);
    DisciplinaryActionResponseDTO getDisciplinaryActionById(Long id);
    DisciplinaryActionResponseDTO updateDisciplinaryAction(Long id, DisciplinaryActionDTO disciplinaryActionDTO);
    void deleteDisciplinaryAction(Long id);
    Page<DisciplinaryActionResponseDTO> getAllDisciplinaryActions(DisciplinaryActionFilterDTO filterDTO, Pageable pageable);
}

