package PSG.backEnd.model.enums.report;

/**
 * Layer-1 grouping mode for the Issued Payments Report (Feature 16).
 * <p>
 * Grouping options:
 * <ul>
 *   <li>{@link #METHOD}   — layer 1 = payment method, layer 2 = supplier (default)</li>
 *   <li>{@link #SUPPLIER} — layer 1 = supplier,        layer 2 = payment method</li>
 *   <li>{@link #NONE}     — flat list, no grouping</li>
 * </ul>
 */
public enum IssuedPaymentReportGroupBy {
    METHOD,
    SUPPLIER,
    NONE
}
