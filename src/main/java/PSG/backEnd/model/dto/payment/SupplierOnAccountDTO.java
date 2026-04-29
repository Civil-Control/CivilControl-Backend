package PSG.backEnd.model.dto.payment;

import java.math.BigDecimal;

/**
 * Read-only summary of the supplier's on-account credit (saldo a favor del proveedor).
 *
 * <p>{@code totalOnAccount} is the gross sum of every payment's {@code onAccountAmount}
 * for the supplier; in the current model this also represents the balance available to
 * apply against future invoices, since on-account consumption is tracked as new payment
 * applications on subsequent payments rather than as a debit on the original payment.
 */
public record SupplierOnAccountDTO(
        Long supplierId,
        BigDecimal totalOnAccount
) {}
