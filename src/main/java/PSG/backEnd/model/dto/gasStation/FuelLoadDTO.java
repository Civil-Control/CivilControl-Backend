package PSG.backEnd.model.dto.gasStation;

import PSG.backEnd.model.enums.vehicle.FuelType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record FuelLoadDTO(

        @NotNull(message = "Date is required.", groups = OnCreate.class)
        @PastOrPresent(message = "Date must be today or in the past.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @NotBlank(message = "Branch code is required", groups = OnCreate.class)
        @Pattern(regexp = "\\d{5}", message = "Branch code must be exactly 5 digits", groups = {OnCreate.class, OnUpdate.class})
        String branchCode,

        @NotBlank(message = "Ticket number is required", groups = OnCreate.class)
        @Pattern(regexp = "\\d{8}", message = "Ticket number must be exactly 8 digits", groups = {OnCreate.class, OnUpdate.class})
        String ticketNumber,

        @NotNull(message = "Fuel type is required.", groups = OnCreate.class)
        FuelType fuelType,

        @NotNull(message = "Liters is required.", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "Liters must be greater than 0.", groups = {OnCreate.class, OnUpdate.class})
        @DecimalMax(value = "5000", message = "Liters must be less than or equal to 5000.", groups = {OnCreate.class, OnUpdate.class})
        Double liters,

        @NotNull(message = "Vehicle id is required.", groups = OnCreate.class)
        @Positive(message = "Vehicle id must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Positive(message = "Project area id must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long projectAreaId,

        @NotNull(message = "Gas station id is required.", groups = OnCreate.class)
        @Positive(message = "Gas station id must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long gasStationId

) {}
