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
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    Building toEntity(BuildingDTO buildingDTO);

    @Mapping(target = "projectAreaId", source = "projectArea.id")
    @Mapping(target = "projectAreaName", source = "projectArea.name")
    @Mapping(target = "projectAreaColor", source = "projectArea.color")
    @Mapping(target = "projectAreaTaskId", source = "projectAreaTask.id")
    @Mapping(target = "projectAreaTaskName", source = "projectAreaTask.name")
    BuildingResponseDTO toResponseDto(Building building);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "stockItems", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    void partialUpdate(BuildingDTO updateDTO, @MappingTarget Building building);
}

