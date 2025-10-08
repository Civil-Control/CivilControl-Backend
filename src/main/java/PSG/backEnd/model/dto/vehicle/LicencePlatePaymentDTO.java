package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.JurisdictionType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a vehicle licence plate payment. " +
        "Represents taxes and fees required for vehicle registration in different jurisdictions.")
public record LicencePlatePaymentDTO(

        @Schema(description = "Date when the payment was made. Must be today or in the past.",
                example = "2024-05-15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Date is required.", groups = OnCreate.class)
        @PastOrPresent(message = "Date must be today or in the past.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "ID of the vehicle for which the payment is being made.",
                example = "25",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Vehicle ID is required.", groups = OnCreate.class)
        @Positive(message = "Vehicle ID must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "Total amount paid for the licence plate. Must be greater than zero.",
                example = "3500.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Amount is required.", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places.", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "Year for which the payment applies. Valid range: 1950-2100.",
                example = "2024",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1950",
                maximum = "2100")
        @NotNull(message = "Year is required.", groups = OnCreate.class)
        @Min(value = 1950, message = "Year must be greater than or equal to 1950.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 2100, message = "Year must be less than or equal to 2100.", groups = {OnCreate.class, OnUpdate.class})
        Integer year,

        @Schema(description = "Period (month) for which the payment applies. Valid range: 1-12, where 1 = January, 12 = December.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1",
                maximum = "12")
        @NotNull(message = "Period is required.", groups = OnCreate.class)
        @Min(value = 1, message = "Period must be at least 1.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "Period must be at most 12.", groups = {OnCreate.class, OnUpdate.class})
        Integer period,

        @Schema(description = "Type of jurisdiction where the payment was made. Valid values: PROVINCIAL (provincial tax), " +
                "MUNICIPAL (municipal tax), NATIONAL (national tax).",
                example = "PROVINCIAL",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PROVINCIAL", "MUNICIPAL", "NATIONAL"})
        @NotNull(message = "Jurisdiction type is required.", groups = OnCreate.class)
        JurisdictionType jurisdictionType
) {}
