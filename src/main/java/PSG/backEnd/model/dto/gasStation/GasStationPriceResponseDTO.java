package PSG.backEnd.model.dto.gasStation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fuel price response with supplier and fuel type details.")
public record GasStationPriceResponseDTO(

        @Schema(description = "Name of the supplier offering this price.",
                example = "Shell Station Downtown")
        String supplierName,

        @Schema(description = "Type of fuel.",
                example = "NAFTA_SUPER",
                allowableValues = {"NAFTA_SUPER", "NAFTA_COMUN", "DIESEL", "GNC"})
        String fuelType,

        @Schema(description = "Price per liter of fuel.",
                example = "850.5500")
        Double price
) {}