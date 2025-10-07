package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.RepairType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepairDTO(

        @NotNull(message = "Date is required.", groups = OnCreate.class)
        @PastOrPresent(message = "Date must be today or in the past.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @NotNull(message = "Vehicle ID is required.", groups = OnCreate.class)
        @Positive(message = "Vehicle ID must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @DecimalMin(value = "0.01", message = "Cost must be greater than zero.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "Cost must have at most 10 integer digits and 2 decimal places.", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal cost,

        @Size(max = 1000, message = "Description must be at most 1000 characters.", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Size(max = 100, message = "Employee name must be at most 100 characters.", groups = {OnCreate.class, OnUpdate.class})
        String employee,

        @Positive(message = "Supplier ID must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long supplierId,

        @NotNull(message = "Repair type is required.", groups = OnCreate.class)
        RepairType repairType
) {}
