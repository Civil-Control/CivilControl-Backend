package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadResponseDTO;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.entity.ProjectArea;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FuelLoadMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pricePerLiter", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "projectArea", source = "projectAreaId", qualifiedByName = "projectAreaIdToEntity")
    @Mapping(target = "gasStation", source = "gasStationId", qualifiedByName = "gasStationIdToEntity")
    @Mapping(target = "fuelType", source = "fuelType")
    FuelLoad toEntity(FuelLoadDTO fuelLoadDTO);

    @Mapping(target = "date", source = "date", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "fuelType", source = "fuelType")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "projectAreaId", source = "projectArea.id")
    @Mapping(target = "projectAreaName", source = "projectArea.name")
    @Mapping(target = "gasStationId", source = "gasStation.id")
    @Mapping(target = "gasStationName", expression = "java(fuelLoad.getGasStation() != null ? fuelLoad.getGasStation().getSupplier().getLegalName() : null)")
    FuelLoadResponseDTO toResponseDto(FuelLoad fuelLoad);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pricePerLiter", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "projectArea", source = "projectAreaId", qualifiedByName = "projectAreaIdToEntity")
    @Mapping(target = "gasStation", source = "gasStationId", qualifiedByName = "gasStationIdToEntity")
    void partialUpdate(FuelLoadDTO updateDTO, @MappingTarget FuelLoad fuelLoad);

    @Named("vehicleIdToEntity")
    default Vehicle vehicleIdToEntity(Long vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        return vehicle;
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

    @Named("gasStationIdToEntity")
    default GasStation gasStationIdToEntity(Long gasStationId) {
        if (gasStationId == null) {
            return null;
        }
        GasStation gasStation = new GasStation();
        gasStation.setId(gasStationId);
        return gasStation;
    }
}
