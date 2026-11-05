package PSG.backEnd.model.dto.stock;

import PSG.backEnd.model.enums.StockCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response DTO containing complete information about a stock item, " +
        "including its unique identifier and all inventory details.")
public record StockResponseDTO(

    @Schema(description = "Unique identifier of the stock item.",
            example = "15")
    Long id,

    @Schema(description = "Name of the stock item.",
            example = "Hammer - 500g Claw")
    String name,

    @Schema(description = "Current quantity of the item in stock.",
            example = "25.50")
    BigDecimal quantity,

    @Schema(description = "Building where the stock item is stored. Contains building details.")
    BuildingStockDTO building,

    @Schema(description = "Category of the stock item.",
            example = "HERRAMIENTAS_MANUALES",
            allowableValues = {"HERRAMIENTAS_MANUALES", "HERRAMIENTAS_ELECTRICAS", "EQUIPOS_PESADOS",
                    "MAQUINARIA", "INSUMOS", "SEGURIDAD_PERSONAL", "ROPA_TRABAJO", "EQUIPAMIENTO_OBRA",
                    "ACCESORIO_VEHICULAR", "LIMPIEZA_MANTENIMIENTO", "REPUESTOS", "OTROS"})
    StockCategory stockCategory
) {}
