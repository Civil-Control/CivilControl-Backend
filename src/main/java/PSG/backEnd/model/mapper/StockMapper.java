package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.stock.StockDTO;
import PSG.backEnd.model.dto.stock.StockResponseDTO;
import PSG.backEnd.model.entity.Stock;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "deleted", constant = "false")
    Stock toEntity(StockDTO stockDTO);

    StockResponseDTO toResponseDto(Stock stock);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "deleted", ignore = true)
    void partialUpdate(StockDTO updateDTO, @MappingTarget Stock stock);
}
