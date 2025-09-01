package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.projectArea.ProjectAreaDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProjectAreaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "active", source = "active", defaultValue = "true")
    ProjectArea toEntity(ProjectAreaDTO projectAreaDTO);

    ProjectAreaResponseDTO toResponseDto(ProjectArea projectArea);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void partialUpdate(ProjectAreaDTO updateDTO, @MappingTarget ProjectArea projectArea);
}
