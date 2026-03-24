package PSG.backEnd.model.dto.reference;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Extended reference item for gas stations, includes the station's fuel prices
 * so that the fuel-load form can auto-fill the price-per-liter field when a
 * fuel type is selected.
 *
 * <p>The {@code prices} map keys are {@link PSG.backEnd.model.enums.vehicle.FuelType}
 * enum names (e.g. {@code "SUPER"}, {@code "DIESEL"}) and values are the price per litre.</p>
 */
@Schema(description = "Gas station reference item with fuel prices for form auto-fill")
public record GasStationReferenceItem(
        @Schema(description = "Unique identifier", example = "3")
        Long id,
        @Schema(description = "Human-readable label for display", example = "YPF Km 12")
        String label,
        @Schema(description = "Map of fuel type name to price per litre. Key = FuelType enum name.")
        Map<String, BigDecimal> prices
) {}
