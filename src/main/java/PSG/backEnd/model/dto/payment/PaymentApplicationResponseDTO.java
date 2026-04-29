package PSG.backEnd.model.dto.payment;

import java.math.BigDecimal;

/**
 * Response DTO describing how a payment was distributed across a single document.
 * Returned inside {@link PaymentDetailsResponseDTO#applications()}.
 */
public record PaymentApplicationResponseDTO(
        Long id,
        Long documentId,
        String documentReference,   // e.g. "FACTURA A 0001-00012345"
        BigDecimal documentTotal,
        BigDecimal amountApplied
) {}
