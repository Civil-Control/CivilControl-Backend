package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.stock.StockDTO;
import PSG.backEnd.model.dto.stock.StockResponseDTO;
import PSG.backEnd.model.entity.Stock;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {BuildingStockMapper.class})
public interface StockMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "building", ignore = true)
    Stock toEntity(StockDTO stockDTO);

    StockResponseDTO toResponseDto(Stock stock);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "building", ignore = true)
    void partialUpdate(StockDTO updateDTO, @MappingTarget Stock stock);
}
