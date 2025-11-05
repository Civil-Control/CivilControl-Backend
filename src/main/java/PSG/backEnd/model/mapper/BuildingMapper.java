package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.building.BuildingDTO;
import PSG.backEnd.model.dto.building.BuildingResponseDTO;
import PSG.backEnd.model.entity.Building;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface BuildingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "active", source = "active", defaultValue = "true")
    @Mapping(target = "stockItems", ignore = true)
    Building toEntity(BuildingDTO buildingDTO);

    BuildingResponseDTO toResponseDto(Building building);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "stockItems", ignore = true)
    void partialUpdate(BuildingDTO updateDTO, @MappingTarget Building building);
}

