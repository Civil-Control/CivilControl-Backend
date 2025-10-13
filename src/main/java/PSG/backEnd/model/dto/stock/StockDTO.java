package PSG.backEnd.model.dto.stock;

import PSG.backEnd.model.enums.StockCategory;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Data Transfer Object for creating or updating stock items. " +
        "Represents inventory items used in construction projects, including tools, equipment, supplies, and materials.")
public record StockDTO(

    @Schema(description = "Name of the stock item. Must be descriptive and unique.",
            example = "Hammer - 500g Claw",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 100)
    @NotBlank(groups = OnCreate.class, message = "Name is required")
    @Size(max = 100, groups = {OnCreate.class, OnUpdate.class}, message = "Name must not exceed 100 characters")
    String name,

    @Schema(description = "Quantity of the item in stock. Must be zero or greater.",
            example = "25.50",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(groups = OnCreate.class, message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "Quantity must be greater than or equal to 0")
    @Digits(integer = 8, fraction = 2, groups = {OnCreate.class, OnUpdate.class}, message = "Quantity must have at most 8 integer digits and 2 decimal places")
    BigDecimal quantity,

    @Schema(description = "Physical location where the item is stored. Optional field for warehouse organization.",
            example = "Warehouse A - Shelf 5",
            maxLength = 200)
    @Size(max = 200, groups = {OnCreate.class, OnUpdate.class}, message = "Location must not exceed 200 characters")
    String location,

    @Schema(description = "Category of the stock item for classification purposes. Valid values: " +
            "HERRAMIENTAS_MANUALES (Hand tools), HERRAMIENTAS_ELECTRICAS (Power tools), EQUIPOS_PESADOS (Heavy equipment), " +
            "MAQUINARIA (Machinery), INSUMOS (Supplies), SEGURIDAD_PERSONAL (Personal safety equipment), " +
            "ROPA_TRABAJO (Work clothing), EQUIPAMIENTO_OBRA (Construction site equipment), " +
            "ACCESORIO_VEHICULAR (Vehicle accessories), LIMPIEZA_MANTENIMIENTO (Cleaning and maintenance), " +
            "REPUESTOS (Spare parts and components), OTROS (Others).",
            example = "HERRAMIENTAS_MANUALES",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"HERRAMIENTAS_MANUALES", "HERRAMIENTAS_ELECTRICAS", "EQUIPOS_PESADOS",
                    "MAQUINARIA", "INSUMOS", "SEGURIDAD_PERSONAL", "ROPA_TRABAJO", "EQUIPAMIENTO_OBRA",
                    "ACCESORIO_VEHICULAR", "LIMPIEZA_MANTENIMIENTO", "REPUESTOS", "OTROS"})
    @NotNull(groups = OnCreate.class, message = "Stock category is required")
    StockCategory stockCategory
) {}
