package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.treasury.CheckbookDTO;
import PSG.backEnd.model.dto.treasury.CheckbookResponseDTO;
import PSG.backEnd.model.entity.treasury.Checkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICheckbookService {

    CheckbookResponseDTO create(CheckbookDTO dto);

    CheckbookResponseDTO update(Long id, CheckbookDTO dto);

    void delete(Long id);

    CheckbookResponseDTO getById(Long id);

    Page<CheckbookResponseDTO> findAll(Long bankAccountId, Boolean active, String search, Pageable pageable);

    Checkbook getEntityById(Long id);
}
