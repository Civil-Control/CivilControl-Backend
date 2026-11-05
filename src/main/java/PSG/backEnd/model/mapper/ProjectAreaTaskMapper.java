package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskResponseDTO;
import PSG.backEnd.model.entity.ProjectAreaTask;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProjectAreaTaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    ProjectAreaTask toEntity(ProjectAreaTaskDTO dto);

    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    ProjectAreaTaskResponseDTO toResponseDto(ProjectAreaTask entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void partialUpdate(ProjectAreaTaskDTO dto, @MappingTarget ProjectAreaTask entity);
}
