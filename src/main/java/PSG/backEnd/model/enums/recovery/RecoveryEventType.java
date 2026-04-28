package PSG.backEnd.model.enums.recovery;

/**
 * Lifecycle of a {@link PSG.backEnd.model.entity.recovery.RecoveryEvent}.
 *
 * <ul>
 *   <li>{@link #GENERATED}: positive recovery created when a Factura A is associated to the
 *       hidden recovery sector (or any later edit recomputes a new positive amount).</li>
 *   <li>{@link #REVERSED}: full reverse of a previous {@link #GENERATED} event because the
 *       triggering document was edited (sector changed, supplier changed, totals changed)
 *       or soft-deleted.</li>
 *   <li>{@link #ADJUSTED_CREDIT_NOTE}: partial reverse caused by a credit note linked to a
 *       previously recovered Factura A. Uses the original event's snapshot percentage.</li>
 * </ul>
 */
public enum RecoveryEventType {
    GENERATED,
    REVERSED,
    ADJUSTED_CREDIT_NOTE
}
