package PSG.backEnd.model.dto.report.issuedPayment;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.payment.CheckStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Layer-3 row of the Issued Payments Report. Represents a single
 * {@code PaymentDetails} record enriched with method-specific fields.
 *
 * <p>Method-specific blocks (check / transfer / cash) are non-null only when
 * {@link #method()} matches; otherwise their fields are {@code null}.
 */
public record IssuedPaymentReportItemDTO(
    Long id,
    LocalDate paymentDate,
    PaymentMethod method,
    Long supplierId,
    String supplierLegalName,
    String supplierTradeName,
    String supplierCuit,
    BigDecimal amount,
    String comment,
    int linkedDocumentCount,
    String paymentMethodReference,

    // Treasury
    Long bankAccountId,
    String bankAccountName,
    String bankName,
    Long cashBoxId,
    String cashBoxName,

    // Check-specific
    String checkNumber,
    LocalDate checkDueDate,
    CheckStatus checkStatus,
    CheckStatus checkPersistedStatus,
    LocalDate checkSettledDate,
    String checkStatusComment,
    Long checkbookId,
    String checkbookName,
    String checkbookNumber,

    // Transfer-specific
    String transferTransactionNumber
) {}
