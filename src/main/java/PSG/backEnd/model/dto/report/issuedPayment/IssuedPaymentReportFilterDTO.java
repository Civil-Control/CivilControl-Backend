package PSG.backEnd.model.dto.report.issuedPayment;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.payment.CheckStatus;
import PSG.backEnd.model.enums.report.IssuedPaymentReportGroupBy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filters for the Issued Payments Report (Feature 16).
 *
 * <p>Date range is mandatory; everything else is optional. Multi-valued filters
 * use OR semantics within the same field and AND semantics across fields.
 */
@Schema(description = "Filters for the Issued Payments Report")
public record IssuedPaymentReportFilterDTO(

    @Schema(description = "Period start date — inclusive. Required.")
    LocalDate startDate,

    @Schema(description = "Period end date — inclusive. Required.")
    LocalDate endDate,

    @Schema(description = "Payment methods to include (CASH/TRANSFER/CHECK). Default: all.")
    List<PaymentMethod> paymentMethods,

    @Schema(description = "Supplier IDs to include")
    List<Long> supplierIds,

    @Schema(description = "Project area IDs (filter by area of imputed documents)")
    List<Long> projectAreaIds,

    @Schema(description = "Check statuses to include (includes derived VENCIDO). " +
            "Only applied to checks; other methods always pass.")
    List<CheckStatus> checkStatuses,

    @Schema(description = "Bank account IDs (used by transfers and checks)")
    List<Long> bankAccountIds,

    @Schema(description = "Cash box IDs (used by cash payments)")
    List<Long> cashBoxIds,

    @Schema(description = "Checkbook IDs (used by checks)")
    List<Long> checkbookIds,

    @Schema(description = "Minimum amount, inclusive")
    BigDecimal minAmount,

    @Schema(description = "Maximum amount, inclusive")
    BigDecimal maxAmount,

    @Schema(description = "Shortcut: only PENDIENTE checks whose dueDate < today (i.e. VENCIDO)")
    Boolean onlyOverdueChecks,

    @Schema(description = "Layer-1 grouping mode: METHOD (default) or SUPPLIER")
    IssuedPaymentReportGroupBy groupBy
) {}
