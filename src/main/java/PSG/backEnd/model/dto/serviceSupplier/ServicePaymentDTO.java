package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a service payment. " +
        "Always references a service assignment.")
public record ServicePaymentDTO(

        @Schema(description = "ID of the service assignment.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.serviceAssignment.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long serviceAssignmentId,

        @Schema(description = "ID of the project area for cost attribution.",
                example = "2")
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long projectAreaId,

        @Schema(description = "ID of the project area task (sub-task). Optional.",
                nullable = true)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long projectAreaTaskId,

        @Schema(description = "Date when the service payment was made.",
                example = "2025-11-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.paymentDate.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{servicePayment.paymentDate.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate paymentDate,

        @Schema(description = "Amount paid. Must be greater than zero.",
                example = "15750.50",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.amount.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{servicePayment.amount.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 8, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "Year the payment applies to.", example = "2026")
        @Min(value = 2000, message = "{servicePayment.year.min}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 2100, message = "{servicePayment.year.max}", groups = {OnCreate.class, OnUpdate.class})
        Integer year,

        @Schema(description = "Period (month) the payment applies to. 1 = January, 12 = December.", example = "3")
        @Min(value = 1, message = "{servicePayment.period.min}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "{servicePayment.period.max}", groups = {OnCreate.class, OnUpdate.class})
        Integer period,

        @Schema(description = "Reference number or invoice number.",
                example = "INV-2025-001234")
        @Size(max = 100, message = "{servicePayment.referenceNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String referenceNumber,

        @Schema(description = "Additional comments or notes.",
                example = "Payment for November 2025 electricity bill")
        @Size(max = 500, message = "{servicePayment.comment.size}", groups = {OnCreate.class, OnUpdate.class})
        String comment,

        @Schema(description = "Payment method used.",
                nullable = true)
        PaymentMethod paymentMethod
) {}

