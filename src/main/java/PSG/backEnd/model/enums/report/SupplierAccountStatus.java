package PSG.backEnd.model.enums.report;

/**
 * Status of a supplier's current-account balance for the report period.
 */
public enum SupplierAccountStatus {
    /** Final balance &gt; 0: supplier has outstanding debt. */
    PENDIENTE,
    /** Final balance == 0: account is settled at the end of the period. */
    CANCELADO
}
