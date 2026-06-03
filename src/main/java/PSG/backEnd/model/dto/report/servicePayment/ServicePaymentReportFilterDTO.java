package PSG.backEnd.model.dto.report.servicePayment;

import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filters for the service payment report")
public record ServicePaymentReportFilterDTO(

    @Schema(description = "Start date (inclusive)")
    LocalDate startDate,

    @Schema(description = "End date (inclusive)")
    LocalDate endDate,

    @Schema(description = "Filter by project area IDs")
    List<Long> projectAreaIds,

    @Schema(description = "Filter by service type")
    ServiceType serviceType,

    @Schema(description = "Filter by subject type (BUILDING or VEHICLE)")
    SubjectType subjectType,

    @Schema(description = "Filter by payment method")
    PaymentMethod paymentMethod,

    @Schema(description = "Filter by year")
    Integer year,

    @Schema(description = "Filter by period/month (1-12)")
    Integer period,

    @Schema(description = "Minimum amount (inclusive)")
    BigDecimal minAmount,

    @Schema(description = "Maximum amount (inclusive)")
    BigDecimal maxAmount,

    @Schema(description = "When true, include records with no project area assigned")
    Boolean includeUnassigned
) {}
