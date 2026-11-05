package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.enums.payment.CheckStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CheckPaymentResponseDTO(
        Long id,
        PaymentDetailsResponseDTO paymentDetails,
        String type,
        LocalDate dueDate,
        String checkNumber,
        Long bankAccountId,
        String bankAccountName,
        String bankName,
        Long checkbookId,
        String checkbookName,
        String checkbookNumber,

        // ── Lifecycle status (Feature 16, Part A) ──
        /** Effective status: equals {@link #persistedStatus} except for PENDIENTE checks past due date,
         *  which are reported as {@link CheckStatus#VENCIDO}. */
        CheckStatus status,
        /** Persisted operational status — never includes the derived VENCIDO value. */
        CheckStatus persistedStatus,
        LocalDate settledDate,
        String statusComment,
        LocalDateTime statusChangedAt,
        Long statusChangedByUserId
) implements PaymentResponseDTO {
}
