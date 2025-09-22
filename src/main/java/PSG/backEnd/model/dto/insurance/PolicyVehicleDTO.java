package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyVehicleDTO(

        @NotNull(message = "Vehicle ID is required.", groups = OnCreate.class)
        Long vehicleId,

        @DecimalMin(value = "0.0", inclusive = false, message = "Sum insured must be greater than 0.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "Sum insured must have at most 13 integer digits and 2 decimal places.", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal sumInsured,

        @NotNull(message = "Effective from date is required.", groups = OnCreate.class)
        LocalDate effectiveFrom,

        @NotNull(message = "Effective to date is required.", groups = OnCreate.class)
        LocalDate effectiveTo,

        LocalDate cancellationDate,

        @Min(value = 1, message = "Number of installments must be at least 1.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "Number of installments must be at most 12.", groups = {OnCreate.class, OnUpdate.class})
        Integer numberOfInstallments
) {}
