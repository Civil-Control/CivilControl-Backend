package PSG.backEnd.model.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only projection of a {@link PSG.backEnd.model.entity.payment.PaymentApplication} as seen
 * from the document side: enriches the raw application row with the parent payment's primary
 * fields so the UI can render a single self-contained mini-table of "imputaciones recibidas"
 * without issuing N+1 calls to fetch each payment individually.
 */
@Schema(description = "Per-payment imputation against a transactional document, including the " +
        "parent payment's identifying fields (date, method, references) and the amount applied. " +
        "Returned by GET /api/v1/payments/applications/by-document/{documentId}.")
public record DocumentPaymentApplicationDTO(
        Long applicationId,
        Long paymentId,
        LocalDate paymentDate,
        String paymentMethod,        // "CASH" | "TRANSFER" | "CHECK"
        BigDecimal amountApplied,
        BigDecimal paymentTotal,
        String bankName,
        String transactionNumber,
        String checkNumber,
        LocalDate dueDate,
        String comment
) {}
