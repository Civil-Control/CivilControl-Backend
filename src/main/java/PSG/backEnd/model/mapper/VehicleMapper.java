package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.dto.vehicle.VehicleResponseDTO;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.entity.vehicle.VehicleType;
import PSG.backEnd.model.entity.ProjectArea;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "projectArea", source = "projectAreaId", qualifiedByName = "projectAreaIdToEntity")
    @Mapping(target = "vehicleType", source = "vehicleTypeId", qualifiedByName = "vehicleTypeIdToEntity")
    @Mapping(target = "storedInBuilding", source = "buildingId", qualifiedByName = "buildingIdToEntity")
    Vehicle toEntity(VehicleDTO vehicleDTO);

    @Mapping(target = "projectAreaId", source = "projectArea.id")
    @Mapping(target = "projectAreaName", source = "projectArea.name")
    @Mapping(target = "projectAreaColor", source = "projectArea.color")
    @Mapping(target = "vehicleTypeId", source = "vehicleType.id")
    @Mapping(target = "vehicleTypeName", source = "vehicleType.name")
    @Mapping(target = "buildingId", source = "storedInBuilding.id")
    @Mapping(target = "buildingName", source = "storedInBuilding.name")
    @Mapping(target = "jurisdictionType", source = "jurisdictionType")
    @Mapping(target = "truckEquipment", source = "truckEquipment")
    VehicleResponseDTO toResponseDto(Vehicle vehicle);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "projectArea", source = "projectAreaId", qualifiedByName = "projectAreaIdToEntity")
    @Mapping(target = "vehicleType", source = "vehicleTypeId", qualifiedByName = "vehicleTypeIdToEntity")
    @Mapping(target = "storedInBuilding", source = "buildingId", qualifiedByName = "buildingIdToEntity")
    void partialUpdate(VehicleDTO updateDTO, @MappingTarget Vehicle vehicle);

    @Named("buildingIdToEntity")
    default Building buildingIdToEntity(Long buildingId) {
        if (buildingId == null) {
            return null;
        }
        Building building = new Building();
        building.setId(buildingId);
        return building;
    }

    @Named("projectAreaIdToEntity")
    default ProjectArea projectAreaIdToEntity(Long projectAreaId) {
        if (projectAreaId == null) {
            return null;
        }
        ProjectArea projectArea = new ProjectArea();
        projectArea.setId(projectAreaId);
        return projectArea;
    }

    @Named("vehicleTypeIdToEntity")
    default VehicleType vehicleTypeIdToEntity(Long vehicleTypeId) {
        if (vehicleTypeId == null) {
            return null;
        }
        VehicleType vehicleType = new VehicleType();
        vehicleType.setId(vehicleTypeId);
        return vehicleType;
    }
}
