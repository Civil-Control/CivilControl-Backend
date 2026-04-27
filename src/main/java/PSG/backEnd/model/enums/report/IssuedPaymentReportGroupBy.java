package PSG.backEnd.model.enums.report;

/**
 * Layer-1 grouping mode for the Issued Payments Report (Feature 16).
 * <p>
 * The report is always a 3-layer hierarchy. This enum decides which dimension
 * occupies layer 1 (and consequently layer 2):
 * <ul>
 *   <li>{@link #METHOD}   — layer 1 = payment method, layer 2 = supplier (default)</li>
 *   <li>{@link #SUPPLIER} — layer 1 = supplier,        layer 2 = payment method</li>
 * </ul>
 */
public enum IssuedPaymentReportGroupBy {
    METHOD,
    SUPPLIER
}
