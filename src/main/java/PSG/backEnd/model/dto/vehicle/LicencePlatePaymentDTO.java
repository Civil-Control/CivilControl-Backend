package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.JurisdictionType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LicencePlatePaymentDTO(

        @NotNull(message = "Date is required.", groups = OnCreate.class)
        @PastOrPresent(message = "Date must be today or in the past.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @NotNull(message = "Vehicle ID is required.", groups = OnCreate.class)
        @Positive(message = "Vehicle ID must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @NotNull(message = "Amount is required.", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places.", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @NotNull(message = "Year is required.", groups = OnCreate.class)
        @Min(value = 1950, message = "Year must be greater than or equal to 1950.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 2100, message = "Year must be less than or equal to 2100.", groups = {OnCreate.class, OnUpdate.class})
        Integer year,

        @NotNull(message = "Period is required.", groups = OnCreate.class)
        @Min(value = 1, message = "Period must be at least 1.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "Period must be at most 12.", groups = {OnCreate.class, OnUpdate.class})
        Integer period,

        @NotNull(message = "Jurisdiction type is required.", groups = OnCreate.class)
        JurisdictionType jurisdictionType
) {}

