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

        @Schema(description = "Amount for this item. Optional, must be >= 0.01 if provided.",
                nullable = true)
        @DecimalMin(value = "0.01", message = "{repairItem.amount.min}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "{validation.digits}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "ID of linked transactional document (purchase order). Optional.",
                nullable = true)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long transactionalDocumentId
) {}
