package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.vehicle.RepairOrderRequestDTO;
import PSG.backEnd.model.dto.vehicle.RepairOrderResponseDTO;
import PSG.backEnd.model.entity.vehicle.RepairOrder;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RepairOrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    RepairOrder toEntity(RepairOrderRequestDTO dto);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "createdByUserName", expression = "java(order.getCreatedByUser() != null ? order.getCreatedByUser().getFirstName() + \" \" + order.getCreatedByUser().getLastName() : null)")
    RepairOrderResponseDTO toResponseDto(RepairOrder order);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void partialUpdate(RepairOrderRequestDTO dto, @MappingTarget RepairOrder order);

    @Named("vehicleIdToEntity")
    default Vehicle vehicleIdToEntity(Long vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        return vehicle;
    }
}
