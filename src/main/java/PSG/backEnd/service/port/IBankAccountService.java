package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.treasury.BankAccountDTO;
import PSG.backEnd.model.dto.treasury.BankAccountMovementDTO;
import PSG.backEnd.model.dto.treasury.BankAccountMovementResponseDTO;
import PSG.backEnd.model.dto.treasury.BankAccountResponseDTO;
import PSG.backEnd.model.entity.treasury.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IBankAccountService {

    BankAccountResponseDTO create(BankAccountDTO dto);

    BankAccountResponseDTO update(Long id, BankAccountDTO dto);

    void delete(Long id);

    BankAccountResponseDTO getById(Long id);

    Page<BankAccountResponseDTO> findAll(Boolean active, String search, Pageable pageable);

    BankAccountMovementResponseDTO registerManualMovement(BankAccountMovementDTO dto);

    Page<BankAccountMovementResponseDTO> findMovements(Long bankAccountId, Pageable pageable);

    BankAccount getEntityById(Long id);
}
