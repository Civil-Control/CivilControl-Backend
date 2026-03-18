package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.vehicle.VehicleTypeDTO;
import PSG.backEnd.model.dto.vehicle.VehicleTypeResponseDTO;
import PSG.backEnd.model.entity.vehicle.VehicleType;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    VehicleType toEntity(VehicleTypeDTO vehicleTypeDTO);

    VehicleTypeResponseDTO toResponseDto(VehicleType vehicleType);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void partialUpdate(VehicleTypeDTO vehicleTypeDTO, @MappingTarget VehicleType vehicleType);
}

