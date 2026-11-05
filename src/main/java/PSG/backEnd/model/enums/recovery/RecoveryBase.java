package PSG.backEnd.model.enums.recovery;

/**
 * Calculation base for the Value Recovery (Feature 18) percentage.
 *
 * <p>Each {@link PSG.backEnd.model.entity.recovery.RecoverySupplierConfig} declares which
 * base its {@code recoveryPercentage} is applied to. The base used at the moment of an
 * event is also frozen on {@link PSG.backEnd.model.entity.recovery.RecoveryEvent} as
 * {@code snapshotBase}, so later changes to the config never alter historic recoveries.
 *
 * <ul>
 *   <li>{@link #NET} — default. Recovers {@code (net * %) + iva}: the percentage applies
 *       only to the net portion of the invoice while the IVA is recovered at 100 %.
 *       Matches the Argentine fiscal-credit semantics on Factura A.</li>
 *   <li>{@link #TOTAL} — recovers {@code (net + iva) * %}: a flat percentage of the full
 *       invoice total, with no special IVA handling.</li>
 * </ul>
 */
public enum RecoveryBase {
    NET,
    TOTAL
}
