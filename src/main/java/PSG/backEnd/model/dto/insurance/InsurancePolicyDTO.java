package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import PSG.backEnd.model.validation.ValidEffectiveDates;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating an insurance policy. " +
        "Represents the master insurance policy information including coverage details, dates, and payment terms.")
@ValidEffectiveDates
public record InsurancePolicyDTO(

        @Schema(description = "Unique policy number assigned by the insurance company. Maximum 50 characters.",
                example = "POL-2024-001234",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 50)
        @NotBlank(message = "{insurancePolicy.policyNumber.required}", groups = OnCreate.class)
        @Size(max = 50, message = "{insurancePolicy.policyNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String policyNumber,

        @Schema(description = "Term or renewal number for the policy period. Maximum 20 characters.",
                example = "2024-01",
                maxLength = 20,
                nullable = true)
        @Size(max = 20, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
        String termNumber,

        @Schema(description = "Endorsement sequence number for policy modifications. Maximum 20 characters.",
                example = "END-001",
                maxLength = 20,
                nullable = true)
        @Size(max = 20, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
        String endorsementSecuence,

        @Schema(description = "Type of insurance policy. Valid values: AUTOMOTOR (auto insurance).",
                example = "AUTOMOTOR",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"AUTOMOTOR"})
        @NotNull(message = "{insurancePolicy.policyType.required}", groups = OnCreate.class)
        PolicyType policyType,

        @Schema(description = "Current status of the policy. Valid values: COTIZADO (quoted), ACTIVO (active policy in force), " +
                "VENCIDO (expired), CANCELADO (cancelled), EXPIRADO (expired permanently).",
                example = "ACTIVO",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"COTIZADO", "ACTIVO", "VENCIDO", "CANCELADO", "EXPIRADO"})
        @NotNull(message = "{insurancePolicy.policyStatus.required}", groups = OnCreate.class)
        PolicyStatus policyStatus,

        @Schema(description = "Frequency of premium payments. Valid values: MENSUAL, BIMESTRAL, TRIMESTRAL, SEMI_ANUAL, ANUAL, PAGO_UNICO.",
                example = "MENSUAL",
                allowableValues = {"MENSUAL", "BIMESTRAL", "TRIMESTRAL", "SEMI_ANUAL", "ANUAL", "PAGO_UNICO"},
                nullable = true)
        PaymentFrequency paymentFrequency,

        @Schema(description = "Total sum insured or coverage amount. Must be greater than zero and have up to 13 integer digits and 2 decimal places.",
                example = "1500000.00",
                nullable = true)
        @DecimalMin(value = "0.0", inclusive = false, message = "{insurancePolicy.sumInsured.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal sumInsured,

        @Schema(description = "Date when the policy was issued by the insurance company.",
                example = "2024-01-10",
                nullable = true)
        LocalDate issueDate,

        @Schema(description = "Date when the policy coverage begins. Must be before effectiveTo date.",
                example = "2024-01-15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{insurancePolicy.effectiveFrom.required}", groups = OnCreate.class)
        LocalDate effectiveFrom,

        @Schema(description = "Date when the policy coverage ends. Must be after effectiveFrom date.",
                example = "2025-01-14",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{insurancePolicy.effectiveTo.required}", groups = OnCreate.class)
        LocalDate effectiveTo,

        @Schema(description = "Date when the policy was cancelled, if applicable.",
                example = "2024-06-30",
                nullable = true)
        LocalDate cancellationDate,

        @Schema(description = "Number of payment installments for the policy premium. Valid range: 1-12.",
                example = "12",
                minimum = "1",
                maximum = "12",
                nullable = true)
        @Min(value = 1, message = "{insurancePolicy.numberOfInstallments.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "{insurancePolicy.numberOfInstallments.positive}", groups = {OnCreate.class, OnUpdate.class})
        Integer numberOfInstallments,

        @Schema(description = "Total premium amount for the policy. Must be zero or greater.",
                example = "50000.00",
                nullable = true)
        @DecimalMin(value = "0.0", inclusive = true, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal premioTotal,

        @Schema(description = "Monthly premium amount, calculated as premioTotal / numberOfInstallments.",
                example = "4166.67",
                nullable = true)
        @DecimalMin(value = "0.0", inclusive = true, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal premioMensual,

        @Schema(description = "Day of the month for periodic payment due date (1-28). Not required for PAGO_UNICO frequency.",
                example = "20",
                minimum = "1",
                maximum = "28",
                nullable = true)
        @Min(value = 1, message = "{insurancePolicy.periodicDueDay.range}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 28, message = "{insurancePolicy.periodicDueDay.range}", groups = {OnCreate.class, OnUpdate.class})
        Integer periodicDueDay,

        @Schema(description = "Whether the payment due date falls at the start of the period (true) or at the end (false, default).",
                example = "false",
                nullable = true)
        Boolean dueAtStartOfPeriod,

        @Schema(description = "ID of the supplier (insurance company) associated with this policy.",
                example = "5",
                nullable = true)
        Long supplierId
) {}