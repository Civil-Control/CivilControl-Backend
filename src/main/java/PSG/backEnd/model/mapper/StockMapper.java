package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.stock.StockDTO;
import PSG.backEnd.model.dto.stock.StockResponseDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.Stock;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {BuildingStockMapper.class})
public interface StockMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "building", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    Stock toEntity(StockDTO stockDTO);

    @Mapping(target = "transactionalDocument", source = "transactionalDocument", qualifiedByName = "documentToSummaryDto")
    StockResponseDTO toResponseDto(Stock stock);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "building", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    void partialUpdate(StockDTO updateDTO, @MappingTarget Stock stock);

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
