package PSG.backEnd.model.dto.gasStation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response DTO containing complete information about a gas station, " +
        "including supplier details and available fuel types with their prices.")
public record GasStationResponseDTO(

        @Schema(description = "Unique identifier of the gas station.",
                example = "42")
        Long id,

        @Schema(description = "ID of the supplier that owns this gas station.",
                example = "15")
        Long supplierId,

        @Schema(description = "Razón social (legal name) of the supplier.",
                example = "Shell S.A.")
        String supplierName,

        @Schema(description = "Nombre comercial (trade name) of the supplier.",
                example = "Shell")
        String supplierTradeName,

        @Schema(description = "CUIT of the supplier.",
                example = "30-12345678-9")
        String supplierCuit,

        @Schema(description = "Whether the supplier is active.",
                example = "true")
        Boolean supplierActive,

        @Schema(description = "List of available fuel types at this gas station. Calculated from the price list.",
                example = "[\"NAFTA_SUPER\", \"DIESEL\"]")
        List<String> fuelTypes,

        @Schema(description = "List of fuel prices offered at this gas station with supplier and fuel type details.")
        List<GasStationPriceResponseDTO> prices,

        @Schema(description = "Soft deletion flag. When true, the gas station is marked as deleted.",
                example = "false")
        boolean deleted
) {}
