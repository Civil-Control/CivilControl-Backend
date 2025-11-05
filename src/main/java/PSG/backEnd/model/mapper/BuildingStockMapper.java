package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.stock.BuildingStockDTO;
import PSG.backEnd.model.entity.Building;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BuildingStockMapper {

    BuildingStockDTO toDto(Building building);
}

