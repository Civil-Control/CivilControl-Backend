package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object containing common payment details shared across all payment types. " +
        "Includes date, amount, supplier reference, and document associations. " +
        "This DTO is embedded in specific payment type DTOs (cash, transfer, check).")
public record PaymentDetailsDTO(

        @Schema(description = "Date when the payment was made. Must be today or in the past. " +
                "Used for accounting and audit purposes.",
                example = "2024-11-15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{payment.paymentDate.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{payment.paymentDate.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate paymentDate,

        @Schema(description = "ID of the supplier receiving the payment. " +
                "Must reference an existing supplier in the system.",
                example = "42",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{payment.supplierId.required}", groups = OnCreate.class)
        Long supplierId,

        @Schema(description = "Total payment amount. Must be greater than zero. " +
                "Maximum 12 integer digits and 2 decimal places. " +
                "Currency is assumed to be in Argentine Pesos (ARS) or the organization's default currency.",
                example = "15000.50",
                minimum = "0.01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{payment.amount.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{payment.amount.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "Additional notes or comments about the payment. " +
                "Can include payment reference, special conditions, or any relevant information. " +
                "Maximum 500 characters. Optional field.",
                example = "Pago correspondiente a factura B 00001-00012345. Primera cuota de 3.",
                maxLength = 500,
                nullable = true)
        @Size(max = 500, message = "{payment.comment.size}", groups = {OnCreate.class, OnUpdate.class})
        String comment,

        @Schema(description = "List of document IDs that this payment is paying for. " +
                "Used to link payments to invoices or other transactional documents. " +
                "Optional field. If provided, each ID must reference an existing document.",
                example = "[123, 456, 789]",
                nullable = true)
        List<Long> paidDocumentIds
) {}