package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.stockPurchase.StockPurchaseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseResponseDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.StockPurchase;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StockPurchaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documentSortOrder", defaultExpression = "java(0)")
    @Mapping(target = "ivaPercentage", source = "ivaPercentage", defaultExpression = "java(new java.math.BigDecimal(\"21.00\"))")
    StockPurchase toEntity(StockPurchaseDTO dto);

    @Mapping(target = "id", source = "purchase.id")
    @Mapping(target = "date", source = "purchase.date")
    @Mapping(target = "stockId", source = "purchase.stockId")
    @Mapping(target = "stockName", source = "stock.name")
    @Mapping(target = "stockCategory", expression = "java(stock != null && stock.getStockCategory() != null ? stock.getStockCategory().name() : null)")
    @Mapping(target = "quantity", source = "purchase.quantity")
    @Mapping(target = "unitPrice", source = "purchase.unitPrice")
    @Mapping(target = "totalAmount", source = "purchase.totalAmount")
    @Mapping(target = "description", source = "purchase.description")
    @Mapping(target = "notes", source = "purchase.notes")
    @Mapping(target = "transactionalDocumentId", source = "purchase.transactionalDocumentId")
    @Mapping(target = "transactionalDocument", expression = "java(documentToSummaryDto(document))")
    @Mapping(target = "ivaPercentage", source = "purchase.ivaPercentage")
    @Mapping(target = "totalWithIva", expression = "java(computeTotalWithIva(purchase, document))")
    StockPurchaseResponseDTO toResponseDto(StockPurchase purchase, Stock stock, TransactionalDocument document);

    default java.math.BigDecimal computeTotalWithIva(StockPurchase purchase, TransactionalDocument document) {
        if (document == null || purchase.getTotalAmount() == null) return null;
        java.math.BigDecimal iva = purchase.getIvaPercentage();
        if (iva == null || iva.compareTo(java.math.BigDecimal.ZERO) <= 0) return purchase.getTotalAmount();
        return purchase.getTotalAmount()
                .multiply(iva.divide(new java.math.BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP)
                        .add(java.math.BigDecimal.ONE))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Backwards-compatible overload (no linked document). */
    default StockPurchaseResponseDTO toResponseDto(StockPurchase purchase, Stock stock) {
        return toResponseDto(purchase, stock, null);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionalDocumentId", ignore = true)
    @Mapping(target = "ivaPercentage", source = "ivaPercentage")
    void partialUpdate(StockPurchaseDTO dto, @MappingTarget StockPurchase purchase);

    @Named("documentToSummaryDto")
    default TransactionalDocumentSummaryDTO documentToSummaryDto(TransactionalDocument doc) {
        if (doc == null) return null;
        return new TransactionalDocumentSummaryDTO(
                doc.getId(),
                doc.getDocumentType() != null ? doc.getDocumentType().name() : null,
                doc.getBranchCode(),
                doc.getDocumentNumber(),
                doc.getSupplier() != null ? doc.getSupplier().getLegalName() : null,
                doc.getTotal(),
                doc.getDate()
        );
    }
}
