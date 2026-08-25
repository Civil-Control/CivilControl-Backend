package PSG.backEnd.model.dto.gasStation;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Fuel price information for a specific fuel type at a gas station.")
public record GasStationPriceDTO(

        @Schema(description = "Type of fuel. Either a built-in FuelType enum constant name or a custom fuel type's key.",
                example = "INFINIA",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{gasStation.prices.fuelType.required}", groups = {OnCreate.class, OnUpdate.class})
        String fuelType,

        @Schema(description = "Price per liter of fuel. Must be greater than zero and have up to 12 integer digits and 4 decimal places.",
                example = "850.5500",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{gasStation.prices.price.required}", groups = {OnCreate.class, OnUpdate.class})
        @DecimalMin(value = "0.0001", message = "{gasStation.prices.price.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 4, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        Double price
) {}