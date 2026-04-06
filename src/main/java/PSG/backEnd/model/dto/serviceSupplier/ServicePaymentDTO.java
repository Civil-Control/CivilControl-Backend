package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a service payment. " +
        "Represents a payment made for a service assignment (service bound to a building).")
public record ServicePaymentDTO(

        @Schema(description = "ID of the service assignment this payment is for.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.serviceAssignmentId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long serviceAssignmentId,

        @Schema(description = "ID of the project area for cost attribution. " +
                "Defaults from the service assignment's project area if not specified.",
                example = "2")
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long projectAreaId,

        @Schema(description = "Date when the service payment was made. " +
                "Cannot be in the future. Must be today or a past date.",
                example = "2025-11-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.paymentDate.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{servicePayment.paymentDate.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate paymentDate,

        @Schema(description = "Amount paid for the service. Must be greater than zero. " +
                "Format: maximum 8 integer digits and 2 decimal places (e.g., 99999999.99).",
                example = "15750.50",
                minimum = "0.01",
                requiredMode = Schema.RequiredMode.REQUIRED,
                type = "number",
                format = "decimal")
        @NotNull(message = "{servicePayment.amount.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{servicePayment.amount.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 8, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "Reference number or invoice number of the service payment.",
                example = "INV-2025-001234")
        @Size(max = 100, message = "{servicePayment.referenceNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String referenceNumber,

        @Schema(description = "Additional comments or notes about the service payment.",
                example = "Payment for November 2025 electricity bill")
        @Size(max = 500, message = "{servicePayment.comment.size}", groups = {OnCreate.class, OnUpdate.class})
        String comment
) {}

