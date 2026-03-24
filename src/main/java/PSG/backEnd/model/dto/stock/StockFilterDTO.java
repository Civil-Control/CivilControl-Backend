package PSG.backEnd.model.dto.stock;

import PSG.backEnd.model.enums.StockCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Filter DTO for searching and filtering stock items based on various criteria.")
public record StockFilterDTO(

    @Schema(description = "Filter by stock item name. Partial match search.",
            example = "Hammer")
    String name,

    @Schema(description = "Filter by building ID where the stock is stored.",
            example = "1")
    Long buildingId,

    @Schema(description = "Filter by stock category.",
            example = "HERRAMIENTAS_MANUALES",
            allowableValues = {"HERRAMIENTAS_MANUALES", "HERRAMIENTAS_ELECTRICAS", "EQUIPOS_PESADOS",
                    "MAQUINARIA", "INSUMOS", "SEGURIDAD_PERSONAL", "ROPA_TRABAJO", "EQUIPAMIENTO_OBRA",
                    "ACCESORIO_VEHICULAR", "LIMPIEZA_MANTENIMIENTO", "REPUESTOS", "OTROS"})
    StockCategory stockCategory,

    @Schema(description = "Filter by minimum quantity. Returns items with quantity greater than or equal to this value.",
            example = "10.00")
    BigDecimal minQuantity,

    @Schema(description = "Filter by maximum quantity. Returns items with quantity less than or equal to this value.",
            example = "100.00")
    BigDecimal maxQuantity,

    @Schema(description = "Generic search across stock name and category (case-insensitive partial match).",
            example = "Hammer")
    String search
) {}
