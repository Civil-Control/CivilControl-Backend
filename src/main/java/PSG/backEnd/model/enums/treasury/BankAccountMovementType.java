package PSG.backEnd.model.enums.treasury;

import java.math.BigDecimal;

/**
 * Movement types applicable to a {@link PSG.backEnd.model.entity.treasury.BankAccount}.
 * Bank account balance reflects "committed money" — checks reserve their amount when emitted
 * and only release it when reverted (REJECTED/CANCELLED).
 */
public enum BankAccountMovementType {
    INCREMENTO_MANUAL(1),
    DECREMENTO_MANUAL(-1),
    AJUSTE(0),
    TRANSFERENCIA_EMITIDA(-1),     // outflow by a TransferPayment
    CHEQUE_EMITIDO(-1),            // reservation by a CheckPayment at creation
    CHEQUE_COBRADO(0),             // informative — already reserved
    CHEQUE_RECHAZADO(1),           // releases reservation back to balance
    CHEQUE_CANCELADO(1),           // releases reservation back to balance
    DEPOSITO(1),
    TRANSFERENCIA_INTERNA(0);

    private final int sign;

    BankAccountMovementType(int sign) {
        this.sign = sign;
    }

    public int sign() {
        return sign;
    }

    public BigDecimal applySign(BigDecimal amount) {
        if (sign == 0 || amount == null) return amount;
        return sign > 0 ? amount : amount.negate();
    }
}
