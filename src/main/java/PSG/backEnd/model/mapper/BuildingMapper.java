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
    @Mapping(target = "projectArea.id", source = "projectAreaId")
    Building toEntity(BuildingDTO buildingDTO);

    @Mapping(target = "projectAreaId", source = "projectArea.id")
    @Mapping(target = "projectAreaName", source = "projectArea.name")
    BuildingResponseDTO toResponseDto(Building building);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "stockItems", ignore = true)
    @Mapping(target = "projectArea.id", source = "projectAreaId")
    void partialUpdate(BuildingDTO updateDTO, @MappingTarget Building building);
}

