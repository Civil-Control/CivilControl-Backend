package PSG.backEnd.model.dto.stockPurchase;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "DTO for creating or updating a stock purchase record.")
public record StockPurchaseDTO(

        @Schema(description = "Purchase date. Must be today or in the past.", example = "2025-01-15")
        @NotNull(message = "{stockPurchase.date.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{stockPurchase.date.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "ID of the stock item being purchased.", example = "5")
        @NotNull(message = "{stockPurchase.stockId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long stockId,

        @Schema(description = "Quantity of stock purchased. Must be greater than zero.", example = "10.00")
        @NotNull(message = "{stockPurchase.quantity.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{stockPurchase.quantity.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal quantity,

        @Schema(description = "Unit price of the stock item. Optional.", example = "250.00")
        @DecimalMin(value = "0.01", message = "{stockPurchase.unitPrice.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal unitPrice,

        @Schema(description = "Total purchase amount. Optional; if not provided, computed from quantity × unitPrice.", example = "2500.00")
        @DecimalMin(value = "0.01", message = "{stockPurchase.totalAmount.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal totalAmount,

        @Schema(description = "Optional notes for this purchase.", example = "Compra urgente para obra Norte")
        @Size(max = 500, message = "{stockPurchase.notes.maxLength}", groups = {OnCreate.class, OnUpdate.class})
        String notes,

        @Schema(description = "Optional transactional document (invoice/receipt) ID.", example = "42")
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long transactionalDocumentId
) {}
