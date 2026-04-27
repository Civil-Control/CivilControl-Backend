package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.treasury.*;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IBankAccountService;
import PSG.backEnd.service.port.ICashBoxService;
import PSG.backEnd.service.port.ICheckbookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/treasury")
@Tag(name = "Treasury", description = "Tesorería: cajas, cuentas bancarias y chequeras")
@RequiredArgsConstructor
public class TreasuryController {

    private final ICashBoxService cashBoxService;
    private final IBankAccountService bankAccountService;
    private final ICheckbookService checkbookService;

    // ───────────────────── Cash Boxes ─────────────────────

    @GetMapping("/cash-boxes")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public Page<CashBoxResponseDTO> listCashBoxes(@RequestParam(required = false) Boolean active,
                                                  @RequestParam(required = false) String search,
                                                  Pageable pageable) {
        return cashBoxService.findAll(active, search, pageable);
    }

    @GetMapping("/cash-boxes/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public CashBoxResponseDTO getCashBox(@PathVariable Long id) {
        return cashBoxService.getById(id);
    }

    @PostMapping("/cash-boxes")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public ResponseEntity<CashBoxResponseDTO> createCashBox(@Validated(OnCreate.class) @RequestBody CashBoxDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashBoxService.create(dto));
    }

    @PutMapping("/cash-boxes/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public CashBoxResponseDTO updateCashBox(@PathVariable Long id,
                                            @Validated(OnUpdate.class) @RequestBody CashBoxDTO dto) {
        return cashBoxService.update(id, dto);
    }

    @DeleteMapping("/cash-boxes/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_DELETE + "')")
    public ResponseEntity<Void> deleteCashBox(@PathVariable Long id) {
        cashBoxService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cash-boxes/{id}/movements")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public Page<CashBoxMovementResponseDTO> listCashBoxMovements(@PathVariable Long id, Pageable pageable) {
        return cashBoxService.findMovements(id, pageable);
    }

    @PostMapping("/cash-boxes/{id}/movements")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public ResponseEntity<CashBoxMovementResponseDTO> registerCashBoxMovement(
            @PathVariable Long id,
            @Validated(OnCreate.class) @RequestBody CashBoxMovementDTO dto) {
        // The id in the path takes precedence over any value carried by the DTO.
        CashBoxMovementDTO normalized = new CashBoxMovementDTO(id, dto.type(), dto.amount(), dto.movementDate(), dto.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(cashBoxService.registerManualMovement(normalized));
    }

    // ───────────────────── Bank Accounts ─────────────────────

    @GetMapping("/bank-accounts")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public Page<BankAccountResponseDTO> listBankAccounts(@RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) String search,
                                                         Pageable pageable) {
        return bankAccountService.findAll(active, search, pageable);
    }

    @GetMapping("/bank-accounts/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public BankAccountResponseDTO getBankAccount(@PathVariable Long id) {
        return bankAccountService.getById(id);
    }

    @PostMapping("/bank-accounts")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public ResponseEntity<BankAccountResponseDTO> createBankAccount(@Validated(OnCreate.class) @RequestBody BankAccountDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.create(dto));
    }

    @PutMapping("/bank-accounts/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public BankAccountResponseDTO updateBankAccount(@PathVariable Long id,
                                                    @Validated(OnUpdate.class) @RequestBody BankAccountDTO dto) {
        return bankAccountService.update(id, dto);
    }

    @DeleteMapping("/bank-accounts/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_DELETE + "')")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        bankAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bank-accounts/{id}/movements")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public Page<BankAccountMovementResponseDTO> listBankAccountMovements(@PathVariable Long id, Pageable pageable) {
        return bankAccountService.findMovements(id, pageable);
    }

    @PostMapping("/bank-accounts/{id}/movements")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public ResponseEntity<BankAccountMovementResponseDTO> registerBankAccountMovement(
            @PathVariable Long id,
            @Validated(OnCreate.class) @RequestBody BankAccountMovementDTO dto) {
        BankAccountMovementDTO normalized = new BankAccountMovementDTO(id, dto.type(), dto.amount(), dto.movementDate(), dto.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.registerManualMovement(normalized));
    }

    // ───────────────────── Checkbooks ─────────────────────

    @GetMapping("/checkbooks")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public Page<CheckbookResponseDTO> listCheckbooks(@RequestParam(required = false) Long bankAccountId,
                                                     @RequestParam(required = false) Boolean active,
                                                     @RequestParam(required = false) String search,
                                                     Pageable pageable) {
        return checkbookService.findAll(bankAccountId, active, search, pageable);
    }

    @GetMapping("/checkbooks/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_READ + "')")
    public CheckbookResponseDTO getCheckbook(@PathVariable Long id) {
        return checkbookService.getById(id);
    }

    @PostMapping("/checkbooks")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public ResponseEntity<CheckbookResponseDTO> createCheckbook(@Validated(OnCreate.class) @RequestBody CheckbookDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkbookService.create(dto));
    }

    @PutMapping("/checkbooks/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_WRITE + "')")
    public CheckbookResponseDTO updateCheckbook(@PathVariable Long id,
                                                @Validated(OnUpdate.class) @RequestBody CheckbookDTO dto) {
        return checkbookService.update(id, dto);
    }

    @DeleteMapping("/checkbooks/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.TREASURY_DELETE + "')")
    public ResponseEntity<Void> deleteCheckbook(@PathVariable Long id) {
        checkbookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
