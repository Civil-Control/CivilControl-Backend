package PSG.backEnd.model.enums.payment;

/**
 * Operational lifecycle of a {@link PSG.backEnd.model.entity.payment.CheckPayment}.
 * <p>
 * The {@link #VENCIDO} value is <b>derived at read-time</b> (a {@link #PENDIENTE}
 * check whose due date is in the past) and is therefore <b>never persisted</b>.
 * Only the operational values (PENDIENTE / COBRADO / RECHAZADO / CANCELADO) live in DB.
 */
public enum CheckStatus {
    /** Issued and circulating, waiting to be settled. Default value. */
    PENDIENTE,
    /** The bank debited it — the money actually left the account. */
    COBRADO,
    /** Bounced by the bank (no funds, signature mismatch, etc.). */
    RECHAZADO,
    /** Voided by the issuer before being settled. */
    CANCELADO,
    /**
     * Derived: a {@link #PENDIENTE} check whose due date is before today.
     * Never persisted.
     */
    VENCIDO;

    /** Whether transitions out of this status are forbidden. */
    public boolean isTerminal() {
        return this == COBRADO || this == CANCELADO;
    }
}
