package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "DTO for creating or updating a policy payment.")
public record PolicyPaymentDTO(

        @Schema(description = "ID of the insurance policy this payment belongs to.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        Long insurancePolicyId,

        @Schema(description = "Date when the payment was made.",
                example = "2026-03-20",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        LocalDate paymentDate,

        @Schema(description = "Payment amount. Must be greater than zero.",
                example = "4500.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "Start date of the period this payment covers.",
                example = "2026-03-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        LocalDate periodFrom,

        @Schema(description = "End date of the period this payment covers.",
                example = "2026-03-31",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        LocalDate periodTo,

        @Schema(description = "Optional notes about this payment.",
                example = "Pago correspondiente a marzo 2026",
                nullable = true)
        @Size(max = 500, message = "{validation.maxLength}", groups = {OnCreate.class, OnUpdate.class})
        String notes
) {}
