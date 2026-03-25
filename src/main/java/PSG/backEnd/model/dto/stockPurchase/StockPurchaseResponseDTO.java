package PSG.backEnd.model.dto.stockPurchase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response DTO for a stock purchase record, including stock item details.")
public record StockPurchaseResponseDTO(

        @Schema(description = "Unique identifier of the stock purchase.", example = "12")
        Long id,

        @Schema(description = "Purchase date.", example = "2025-01-15")
        LocalDate date,

        @Schema(description = "ID of the stock item purchased.", example = "5")
        Long stockId,

        @Schema(description = "Name of the stock item purchased.", example = "Casco de Seguridad")
        String stockName,

        @Schema(description = "Category of the stock item.", example = "SEGURIDAD_PERSONAL")
        String stockCategory,

        @Schema(description = "Quantity purchased.", example = "10.00")
        BigDecimal quantity,

        @Schema(description = "Unit price.", example = "250.00")
        BigDecimal unitPrice,

        @Schema(description = "Total purchase amount.", example = "2500.00")
        BigDecimal totalAmount,

        @Schema(description = "Optional notes.", example = "Compra urgente para obra Norte")
        String notes,

        @Schema(description = "ID of the linked transactional document.", example = "42")
        Long transactionalDocumentId,

        Integer documentSortOrder
) {}
