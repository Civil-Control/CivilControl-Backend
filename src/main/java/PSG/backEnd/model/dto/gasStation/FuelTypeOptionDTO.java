package PSG.backEnd.model.dto.gasStation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A selectable fuel type: either a built-in FuelType enum constant or a tenant-defined custom type.")
public record FuelTypeOptionDTO(

        @Schema(description = "Key stored on GasStationPrice.fuelType / FuelLoad.fuelType. " +
                "For built-in types this is the FuelType enum constant name (e.g. INFINIA).",
                example = "INFINIA")
        String key,

        @Schema(description = "Display label.", example = "Nafta Infinia")
        String label,

        @Schema(description = "True when this is a tenant-defined custom fuel type rather than a built-in FuelType constant.")
        boolean custom,

        @Schema(description = "True when this custom fuel type was deleted. Always false for built-ins. " +
                "Excluded from selectable lists by default; included (via includeDeleted=true) so callers can still " +
                "resolve the label of a value already assigned to an existing price or fuel load.")
        boolean deleted
) {}
