package PSG.backEnd.model.enums;

/**
 * Represents the usage type of an Item.
 *
 * <ul>
 *   <li>{@link #COMPRA} – Item used in purchase documents (transactional documents).</li>
 *   <li>{@link #VENTA}  – Item used in sales documents.</li>
 * </ul>
 *
 * An Item can have one or both types. For example, a product may be both
 * purchased from suppliers and sold to clients.
 */
public enum ItemType {
    COMPRA,
    VENTA
}
