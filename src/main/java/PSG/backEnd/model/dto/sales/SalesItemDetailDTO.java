package PSG.backEnd.model.dto.sales;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "A single line item within a sales document.")
public record SalesItemDetailDTO(

    @Schema(description = "ID of the item detail row. Null on create; when present on update, " +
            "identifies the existing row to update in place instead of recreating it.",
            nullable = true)
    Long id,

    @Schema(description = "ID of the item/product.", example = "1")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    Long itemId,

    @Schema(description = "Unit price before IVA.", example = "100.00")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @DecimalMin(value = "0.0", inclusive = true, message = "{validation.decimalMin}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal unitAmount,

    @Schema(description = "Quantity of items.", example = "10")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @Min(value = 1, message = "{validation.min}", groups = {OnCreate.class, OnUpdate.class})
    Integer quantity,

    @Schema(description = "IVA percentage applied to this item.", example = "21.00")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @DecimalMin(value = "0.0", inclusive = true, message = "{validation.decimalMin}", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "100.0", inclusive = true, message = "{validation.decimalMax}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal ivaPercentage,

    @Schema(description = "Pre-calculated total (optional; recalculated by @PrePersist if null).", example = "1210.00")
    BigDecimal totalAmount
) {}
