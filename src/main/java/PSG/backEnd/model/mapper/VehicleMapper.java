package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.dto.vehicle.VehicleResponseDTO;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.entity.ProjectArea;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "projectArea", source = "projectAreaId", qualifiedByName = "projectAreaIdToEntity")
    Vehicle toEntity(VehicleDTO vehicleDTO);

    @Mapping(target = "projectAreaName", source = "projectArea.name")
    @Mapping(target = "vehicleType", source = "vehicleType")
    @Mapping(target = "jurisdictionType", source = "jurisdictionType")
    VehicleResponseDTO toResponseDto(Vehicle vehicle);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "projectArea", source = "projectAreaId", qualifiedByName = "projectAreaIdToEntity")
    void partialUpdate(VehicleDTO updateDTO, @MappingTarget Vehicle vehicle);

    @Named("projectAreaIdToEntity")
    default ProjectArea projectAreaIdToEntity(Long projectAreaId) {
        if (projectAreaId == null) {
            return null;
        }
        ProjectArea projectArea = new ProjectArea();
        projectArea.setId(projectAreaId);
        return projectArea;
    }
}
