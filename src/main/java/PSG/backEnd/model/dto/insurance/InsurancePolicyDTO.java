package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InsurancePolicyDTO(

        @NotBlank(message = "Policy number is required.", groups = OnCreate.class)
        @Size(max = 50, message = "Policy number must be at most 50 characters.", groups = {OnCreate.class, OnUpdate.class})
        String policyNumber,

        @Size(max = 20, message = "Term number must be at most 20 characters.", groups = {OnCreate.class, OnUpdate.class})
        String termNumber,

        @Size(max = 20, message = "Endorsement sequence must be at most 20 characters.", groups = {OnCreate.class, OnUpdate.class})
        String endorsementSecuence,

        @NotNull(message = "Policy type is required.", groups = OnCreate.class)
        PolicyType policyType,

        @NotNull(message = "Policy status is required.", groups = OnCreate.class)
        PolicyStatus policyStatus,

        PaymentFrequency paymentFrequency,

        @DecimalMin(value = "0.0", inclusive = false, message = "Sum insured must be greater than 0.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "Sum insured must have at most 13 integer digits and 2 decimal places.", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal sumInsured,

        LocalDate issueDate,

        @NotNull(message = "Effective from date is required.", groups = OnCreate.class)
        @FutureOrPresent(message = "Effective from date must be today or in the future.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate effectiveFrom,

        @NotNull(message = "Effective to date is required.", groups = OnCreate.class)
        @Future(message = "Effective to date must be in the future.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate effectiveTo,

        LocalDate cancellationDate,

        @Min(value = 1, message = "Number of installments must be at least 1.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "Number of installments must be at most 12.", groups = {OnCreate.class, OnUpdate.class})
        Integer numberOfInstallments
) {}
