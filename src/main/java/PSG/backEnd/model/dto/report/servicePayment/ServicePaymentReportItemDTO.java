package PSG.backEnd.model.dto.report.servicePayment;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single service payment entry within the report")
public record ServicePaymentReportItemDTO(

    @Schema(description = "Service payment ID", example = "78")
    Long id,

    @Schema(description = "Date of the payment", example = "2026-03-15")
    LocalDate paymentDate,

    @Schema(description = "Payment amount", example = "12500.00")
    BigDecimal amount,

    @Schema(description = "Year of the service period", example = "2026")
    Integer year,

    @Schema(description = "Month of the service period (1-12)", example = "3")
    Integer period,

    @Schema(description = "Service type (e.g. LUZ, GAS, AGUA)")
    String serviceType,

    @Schema(description = "Supplier legal name")
    String supplierName,

    @Schema(description = "Supplier trade name", nullable = true)
    String supplierTradeName,

    @Schema(description = "Payment reference number", nullable = true)
    String referenceNumber,

    @Schema(description = "Payment method used", nullable = true)
    PaymentMethod paymentMethod,

    @Schema(description = "Project area task ID", nullable = true)
    Long projectAreaTaskId,

    @Schema(description = "Project area task name", nullable = true)
    String projectAreaTaskName,

    @Schema(description = "Building name (for flat view)", nullable = true)
    String buildingName,

    @Schema(description = "Subject type: BUILDING or VEHICLE")
    String subjectType,

    @Schema(description = "Project area name")
    String projectAreaName
) {}
