package PSG.backEnd.model.dto.gasStation;

import PSG.backEnd.model.enums.vehicle.FuelType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

public record GasStationPriceDTO(

        @NotNull(message = "Fuel type is required.", groups = {OnCreate.class})
        FuelType fuelType,

        @NotNull(message = "Price is required.", groups = {OnCreate.class})
        @DecimalMin(value = "0.0001", message = "Price must be greater than 0.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 4, message = "Price must have up to 12 integer digits and 4 decimals.", groups = {OnCreate.class, OnUpdate.class})
        Double price
) {}