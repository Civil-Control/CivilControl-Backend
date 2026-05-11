package PSG.backEnd.model.dto.purchaseOrder;

import PSG.backEnd.model.enums.PurchaseOrderCategory;
import PSG.backEnd.model.enums.PurchaseOrderPriority;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating a purchase order.")
public record PurchaseOrderRequestDTO(

        @Schema(description = "Date of the request. Must be today or in the past.",
                example = "2026-05-11",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{purchaseOrder.date.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{purchaseOrder.date.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "Category of the requested items.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{purchaseOrder.category.required}", groups = OnCreate.class)
        PurchaseOrderCategory category,

        @Schema(description = "Description of the need or reason for the purchase. Maximum 1000 characters.",
                example = "Se necesitan cascos y guantes para el nuevo personal.",
                maxLength = 1000,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{purchaseOrder.description.required}", groups = OnCreate.class)
        @Size(max = 1000, message = "{purchaseOrder.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "List of requested items. At least one is required.",
                example = "[\"10 cascos de seguridad\", \"20 pares de guantes\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "{purchaseOrder.items.required}", groups = OnCreate.class)
        List<@NotBlank @Size(max = 200) String> items,

        @Schema(description = "Name of the person requesting. Maximum 100 characters.",
                example = "Juan Perez",
                nullable = true)
        @Size(max = 100, message = "{purchaseOrder.requestedBy.size}", groups = {OnCreate.class, OnUpdate.class})
        String requestedBy,

        @Schema(description = "Estimated amount for the purchase. Optional.",
                example = "15000.00",
                nullable = true)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        @DecimalMax(value = "9999999999.99", message = "{purchaseOrder.estimatedAmount.max}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal estimatedAmount,

        @Schema(description = "Priority of the purchase order.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{purchaseOrder.priority.required}", groups = OnCreate.class)
        PurchaseOrderPriority priority
) {}
