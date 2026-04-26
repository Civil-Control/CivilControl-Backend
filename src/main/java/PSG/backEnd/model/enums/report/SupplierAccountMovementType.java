package PSG.backEnd.model.enums.report;

/**
 * Classification of a single movement in a supplier's current-account timeline.
 *
 * <p>{@link #INVOICE} and {@link #DEBIT_NOTE} increase the supplier's balance (debit);
 * {@link #CREDIT_NOTE} and {@link #PAYMENT} decrease it (credit).
 */
public enum SupplierAccountMovementType {
    INVOICE,
    DEBIT_NOTE,
    CREDIT_NOTE,
    PAYMENT
}
