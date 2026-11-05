package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.RepairItemType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "DTO for creating or updating a repair item (material or labor).")
public record RepairItemDTO(

        @Schema(description = "ID of the item. Null on create, required on update to identify existing items.",
                nullable = true)
        Long id,

        @Schema(description = "Type of item: MATERIAL or MANO_DE_OBRA.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repairItem.itemType.required}", groups = OnCreate.class)
        RepairItemType itemType,

        @Schema(description = "Description of the item (part name or mechanic name).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 500)
        @NotBlank(message = "{repairItem.description.required}", groups = OnCreate.class)
        @Size(max = 500, message = "{repairItem.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Unit amount (price per unit) for this item. Optional, must be >= 0.00 if provided.",
                nullable = true)
        @DecimalMin(value = "0.00", inclusive = true, message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "{validation.digits}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "Quantity of units for this item. Defaults to 1 if omitted. Must be > 0.",
                nullable = true, example = "1.00")
        @DecimalMin(value = "0.01", message = "{repairItem.quantity.min}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "{validation.digits}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal quantity,

        @Schema(description = "IVA percentage (0-100). Defaults to 21 if omitted.", nullable = true, example = "21.00")
        @DecimalMin(value = "0.00", inclusive = true, message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
        @DecimalMax(value = "100.00", inclusive = true, message = "{validation.max}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 3, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal ivaPercentage,

        @Schema(description = "ID of linked transactional document (purchase order). Optional.",
                nullable = true)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long transactionalDocumentId
) {}
