package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.treasury.CashBoxDTO;
import PSG.backEnd.model.dto.treasury.CashBoxMovementDTO;
import PSG.backEnd.model.dto.treasury.CashBoxMovementResponseDTO;
import PSG.backEnd.model.dto.treasury.CashBoxResponseDTO;
import PSG.backEnd.model.entity.treasury.CashBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICashBoxService {

    CashBoxResponseDTO create(CashBoxDTO dto);

    CashBoxResponseDTO update(Long id, CashBoxDTO dto);

    void delete(Long id);

    CashBoxResponseDTO getById(Long id);

    Page<CashBoxResponseDTO> findAll(Boolean active, String search, Pageable pageable);

    CashBoxMovementResponseDTO registerManualMovement(CashBoxMovementDTO dto);

    Page<CashBoxMovementResponseDTO> findMovements(Long cashBoxId, Pageable pageable);

    CashBox getEntityById(Long id);

    boolean tenantHasActiveCashBox();
}
