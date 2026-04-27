package PSG.backEnd.service.implementation.treasury;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.model.entity.payment.CashPayment;
import PSG.backEnd.model.entity.payment.CheckPayment;
import PSG.backEnd.model.entity.payment.TransferPayment;
import PSG.backEnd.model.entity.treasury.BankAccount;
import PSG.backEnd.model.entity.treasury.BankAccountMovement;
import PSG.backEnd.model.entity.treasury.CashBox;
import PSG.backEnd.model.entity.treasury.Checkbook;
import PSG.backEnd.model.enums.treasury.BankAccountMovementType;
import PSG.backEnd.model.enums.treasury.CashBoxMovementType;
import PSG.backEnd.repository.treasury.BankAccountMovementRepository;
import PSG.backEnd.repository.treasury.CashBoxMovementRepository;
import PSG.backEnd.repository.treasury.CheckbookRepository;
import PSG.backEnd.service.port.IBankAccountService;
import PSG.backEnd.service.port.ICashBoxService;
import PSG.backEnd.service.port.ICheckbookService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Glue code that converts Payment lifecycle events into treasury movements.
 * Centralizes the Payment ↔ Treasury contract so {@code PaymentService} stays focused on
 * its own responsibilities.
 */
@Component
@RequiredArgsConstructor
public class TreasuryPaymentHook {

    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    private final IBankAccountService bankAccountService;
    private final ICashBoxService cashBoxService;
    private final ICheckbookService checkbookService;
    private final CheckbookRepository checkbookRepository;
    private final CashBoxMovementRepository cashBoxMovementRepository;
    private final BankAccountMovementRepository bankAccountMovementRepository;
    private final MessageSourceHelper messages;

    // ───────────────────────── Resolution ──────────────────────────

    public BankAccount resolveBankAccount(Long bankAccountId) {
        if (bankAccountId == null) {
            throw new IllegalArgumentException(messages.getMessage("treasury.bankAccount.required"));
        }
        return bankAccountService.getEntityById(bankAccountId);
    }

    public Checkbook resolveCheckbook(Long checkbookId) {
        if (checkbookId == null) return null;
        return checkbookService.getEntityById(checkbookId);
    }

    public CashBox resolveCashBox(Long cashBoxId) {
        if (cashBoxId == null) return null;
        return cashBoxService.getEntityById(cashBoxId);
    }

    /**
     * Validates the cash payment cash box rules: if the tenant has any active cash box,
     * the payment must reference one of them.
     */
    public void validateCashPaymentCashBox(Long cashBoxId) {
        if (cashBoxId != null) {
            CashBox b = cashBoxService.getEntityById(cashBoxId);
            if (Boolean.FALSE.equals(b.getActive())) {
                throw new IllegalStateException(messages.getMessage("treasury.cashBox.inactive", b.getName()));
            }
        } else if (cashBoxService.tenantHasActiveCashBox()) {
            throw new IllegalArgumentException(messages.getMessage("treasury.cashBox.required"));
        }
    }

    /**
     * Validates that the check number falls within the checkbook range and has not been consumed yet.
     * The bank account on the payment must match the checkbook's bank account.
     */
    public void validateCheckbookConsistency(Checkbook checkbook,
                                             BankAccount paymentBankAccount,
                                             String checkNumberStr) {
        if (checkbook == null) return;
        if (Boolean.FALSE.equals(checkbook.getActive())) {
            throw new IllegalStateException(messages.getMessage("treasury.checkbook.inactive", checkbook.getCheckbookNumber()));
        }
        if (paymentBankAccount != null
                && !paymentBankAccount.getId().equals(checkbook.getBankAccount().getId())) {
            throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.bankAccountMismatch"));
        }
        if (checkNumberStr == null || !NUMERIC.matcher(checkNumberStr).matches()) {
            throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.invalidCheckNumber", checkNumberStr));
        }
        long n = Long.parseLong(checkNumberStr);
        if (n < checkbook.getRangeFrom() || n > checkbook.getRangeTo()) {
            throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.numberOutOfRange",
                    n, checkbook.getRangeFrom(), checkbook.getRangeTo()));
        }
        boolean alreadyUsed = checkbookRepository.findUsedCheckNumberStrings(checkbook.getId()).stream()
                .filter(s -> s != null && NUMERIC.matcher(s).matches())
                .map(Long::parseLong)
                .anyMatch(used -> used == n);
        if (alreadyUsed) {
            throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.numberAlreadyUsed", n));
        }
    }

    public void autoDeactivateCheckbookIfExhausted(Checkbook cb) {
        if (cb == null) return;
        long total = cb.getRangeTo() - cb.getRangeFrom() + 1;
        long used = checkbookRepository.findUsedCheckNumberStrings(cb.getId()).stream()
                .filter(s -> s != null && NUMERIC.matcher(s).matches())
                .map(Long::parseLong)
                .filter(n -> n >= cb.getRangeFrom() && n <= cb.getRangeTo())
                .distinct()
                .count();
        if (used >= total) {
            cb.setActive(false);
        }
    }

    // ───────────────────────── Movement registration ─────────────────────────

    public BankAccountMovement onCheckCreated(CheckPayment cp) {
        if (cp.getBankAccount() == null) return null;
        BankAccountMovement m = ((BankAccountService) bankAccountService).applyMovement(
                cp.getBankAccount(),
                BankAccountMovementType.CHEQUE_EMITIDO,
                cp.getPaymentDetails().getAmount(),
                cp.getPaymentDetails().getPaymentDate(),
                buildCheckComment(cp),
                cp,
                null);
        autoDeactivateCheckbookIfExhausted(cp.getCheckbook());
        return m;
    }

    public BankAccountMovement onTransferCreated(TransferPayment tp) {
        if (tp.getBankAccount() == null) return null;
        return ((BankAccountService) bankAccountService).applyMovement(
                tp.getBankAccount(),
                BankAccountMovementType.TRANSFERENCIA_EMITIDA,
                tp.getPaymentDetails().getAmount(),
                tp.getPaymentDetails().getPaymentDate(),
                buildTransferComment(tp),
                null,
                tp);
    }

    public void onCashCreated(CashPayment cp) {
        if (cp.getCashBox() == null) return;
        ((CashBoxService) cashBoxService).applyMovement(
                cp.getCashBox(),
                CashBoxMovementType.PAGO_EMITIDO,
                cp.getPaymentDetails().getAmount(),
                cp.getPaymentDetails().getPaymentDate(),
                buildCashComment(cp),
                cp);
    }

    /**
     * Reverses the bank account movement that was created on payment creation. Used by update/delete.
     */
    public void revertCheckMovement(CheckPayment cp) {
        BankAccount acc = cp.getBankAccount();
        if (acc == null) return;
        ((BankAccountService) bankAccountService).applyMovement(
                acc,
                BankAccountMovementType.CHEQUE_CANCELADO,
                cp.getPaymentDetails().getAmount(),
                LocalDate.now(),
                messages.getMessageOrDefault("treasury.bankAccount.movement.revertCheck",
                        "Reversa de cheque emitido (pago modificado/eliminado)"),
                cp,
                null);
    }

    public void revertTransferMovement(TransferPayment tp) {
        BankAccount acc = tp.getBankAccount();
        if (acc == null) return;
        ((BankAccountService) bankAccountService).applyMovement(
                acc,
                BankAccountMovementType.INCREMENTO_MANUAL, // explicit reversal as positive manual increase
                tp.getPaymentDetails().getAmount(),
                LocalDate.now(),
                messages.getMessageOrDefault("treasury.bankAccount.movement.revertTransfer",
                        "Reversa de transferencia (pago modificado/eliminado)"),
                null,
                tp);
    }

    public void revertCashMovement(CashPayment cp) {
        if (cp.getCashBox() == null) return;
        ((CashBoxService) cashBoxService).applyMovement(
                cp.getCashBox(),
                CashBoxMovementType.INCREMENTO_MANUAL,
                cp.getPaymentDetails().getAmount(),
                LocalDate.now(),
                messages.getMessageOrDefault("treasury.cashBox.movement.revertCash",
                        "Reversa de pago en efectivo (modificado/eliminado)"),
                cp);
    }

    private String buildCheckComment(CheckPayment cp) {
        StringBuilder sb = new StringBuilder("Cheque emitido");
        if (cp.getCheckNumber() != null) sb.append(" ").append(cp.getCheckNumber());
        if (cp.getPaymentDetails() != null && cp.getPaymentDetails().getSupplier() != null) {
            sb.append(" — ").append(cp.getPaymentDetails().getSupplier().getLegalName());
        }
        return sb.toString();
    }

    private String buildTransferComment(TransferPayment tp) {
        StringBuilder sb = new StringBuilder("Transferencia emitida");
        if (tp.getTransactionNumber() != null) sb.append(" ").append(tp.getTransactionNumber());
        if (tp.getPaymentDetails() != null && tp.getPaymentDetails().getSupplier() != null) {
            sb.append(" — ").append(tp.getPaymentDetails().getSupplier().getLegalName());
        }
        return sb.toString();
    }

    private String buildCashComment(CashPayment cp) {
        StringBuilder sb = new StringBuilder("Pago en efectivo");
        if (cp.getPaymentDetails() != null && cp.getPaymentDetails().getSupplier() != null) {
            sb.append(" — ").append(cp.getPaymentDetails().getSupplier().getLegalName());
        }
        return sb.toString();
    }

    // suppress unused warnings in case repos become useful later
    @SuppressWarnings("unused")
    private BigDecimal noop() { return BigDecimal.ZERO; }
}
