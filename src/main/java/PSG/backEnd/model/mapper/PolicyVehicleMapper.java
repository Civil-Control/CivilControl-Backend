package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.insurance.PolicyVehicleDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleResponseDTO;
import PSG.backEnd.model.entity.insurance.PolicyVehicle;
import PSG.backEnd.model.entity.insurance.AutoPolicy;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PolicyVehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "autoPolicy", ignore = true)
    PolicyVehicle toEntity(PolicyVehicleDTO policyVehicleDTO);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleBrand", source = "vehicle.brand")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "vehicleYear", source = "vehicle.year")
    @Mapping(target = "autoPolicyId", source = "autoPolicy.id")
    PolicyVehicleResponseDTO toResponseDto(PolicyVehicle policyVehicle);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "autoPolicy", ignore = true)
    void partialUpdate(PolicyVehicleDTO updateDTO, @MappingTarget PolicyVehicle policyVehicle);

    @Named("vehicleIdToEntity")
    default Vehicle vehicleIdToEntity(Long vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        return vehicle;
    }

    @Named("autoPolicyIdToEntity")
    default AutoPolicy autoPolicyIdToEntity(Long autoPolicyId) {
        if (autoPolicyId == null) {
            return null;
        }
        AutoPolicy autoPolicy = new AutoPolicy();
        autoPolicy.setId(autoPolicyId);
        return autoPolicy;
    }
}
