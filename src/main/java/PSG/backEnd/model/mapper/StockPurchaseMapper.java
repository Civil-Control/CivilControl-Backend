package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.stockPurchase.StockPurchaseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseResponseDTO;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.StockPurchase;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StockPurchaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documentSortOrder", defaultExpression = "java(0)")
    StockPurchase toEntity(StockPurchaseDTO dto);

    @Mapping(target = "id", source = "purchase.id")
    @Mapping(target = "date", source = "purchase.date")
    @Mapping(target = "stockId", source = "purchase.stockId")
    @Mapping(target = "stockName", source = "stock.name")
    @Mapping(target = "stockCategory", expression = "java(stock.getStockCategory() != null ? stock.getStockCategory().name() : null)")
    @Mapping(target = "quantity", source = "purchase.quantity")
    @Mapping(target = "unitPrice", source = "purchase.unitPrice")
    @Mapping(target = "totalAmount", source = "purchase.totalAmount")
    @Mapping(target = "notes", source = "purchase.notes")
    @Mapping(target = "transactionalDocumentId", source = "purchase.transactionalDocumentId")
    StockPurchaseResponseDTO toResponseDto(StockPurchase purchase, Stock stock);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionalDocumentId", ignore = true)
    void partialUpdate(StockPurchaseDTO dto, @MappingTarget StockPurchase purchase);
}
