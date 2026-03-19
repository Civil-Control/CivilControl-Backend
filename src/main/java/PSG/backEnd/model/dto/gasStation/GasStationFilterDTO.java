package PSG.backEnd.model.dto.gasStation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Filter criteria for querying gas stations. All fields are optional and can be combined.")
public record GasStationFilterDTO(

        @Schema(description = "Filter by supplier ID.",
                example = "15",
                nullable = true)
        Long supplierId,

        @Schema(description = "Filter by supplier name (legal name or trade name). Partial match search.",
                example = "YPF",
                nullable = true)
        String supplierName,

        @Schema(description = "Filter by available fuel types. Gas stations offering any of these fuel types will be returned.",
                example = "[\"NAFTA_SUPER\", \"DIESEL\"]",
                nullable = true)
        List<String> fuelTypes
) {}
