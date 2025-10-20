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
        @NotBlank(message = "Policy number is required.", groups = OnCreate.class)
        @Size(max = 50, message = "Policy number must be at most 50 characters.", groups = {OnCreate.class, OnUpdate.class})
        String policyNumber,

        @Schema(description = "Term or renewal number for the policy period. Maximum 20 characters.",
                example = "2024-01",
                maxLength = 20,
                nullable = true)
        @Size(max = 20, message = "Term number must be at most 20 characters.", groups = {OnCreate.class, OnUpdate.class})
        String termNumber,

        @Schema(description = "Endorsement sequence number for policy modifications. Maximum 20 characters.",
                example = "END-001",
                maxLength = 20,
                nullable = true)
        @Size(max = 20, message = "Endorsement sequence must be at most 20 characters.", groups = {OnCreate.class, OnUpdate.class})
        String endorsementSecuence,

        @Schema(description = "Type of insurance policy. Valid values: AUTO (vehicle insurance), PROPERTY (property insurance), " +
                "LIABILITY (liability insurance), HEALTH (health insurance), LIFE (life insurance).",
                example = "AUTO",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"AUTO", "PROPERTY", "LIABILITY", "HEALTH", "LIFE"})
        @NotNull(message = "Policy type is required.", groups = OnCreate.class)
        PolicyType policyType,

        @Schema(description = "Current status of the policy. Valid values: ACTIVE (policy in force), EXPIRED (policy ended), " +
                "CANCELLED (policy cancelled), SUSPENDED (policy temporarily suspended), PENDING (awaiting activation).",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"ACTIVE", "EXPIRED", "CANCELLED", "SUSPENDED", "PENDING"})
        @NotNull(message = "Policy status is required.", groups = OnCreate.class)
        PolicyStatus policyStatus,

        @Schema(description = "Frequency of premium payments. Valid values: MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL.",
                example = "MONTHLY",
                allowableValues = {"MONTHLY", "QUARTERLY", "SEMI_ANNUAL", "ANNUAL"},
                nullable = true)
        PaymentFrequency paymentFrequency,

        @Schema(description = "Total sum insured or coverage amount. Must be greater than zero and have up to 13 integer digits and 2 decimal places.",
                example = "1500000.00",
                nullable = true)
        @DecimalMin(value = "0.0", inclusive = false, message = "Sum insured must be greater than 0.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "Sum insured must have at most 13 integer digits and 2 decimal places.", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal sumInsured,

        @Schema(description = "Date when the policy was issued by the insurance company.",
                example = "2024-01-10",
                nullable = true)
        LocalDate issueDate,

        @Schema(description = "Date when the policy coverage begins. Must be before effectiveTo date.",
                example = "2024-01-15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Effective from date is required.", groups = OnCreate.class)
        LocalDate effectiveFrom,

        @Schema(description = "Date when the policy coverage ends. Must be after effectiveFrom date.",
                example = "2025-01-14",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Effective to date is required.", groups = OnCreate.class)
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
        @Min(value = 1, message = "Number of installments must be at least 1.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "Number of installments must be at most 12.", groups = {OnCreate.class, OnUpdate.class})
        Integer numberOfInstallments
) {}