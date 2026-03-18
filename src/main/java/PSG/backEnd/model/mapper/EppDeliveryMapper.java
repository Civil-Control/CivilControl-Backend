package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.EppDeliveryDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryResponseDTO;
import PSG.backEnd.model.entity.employee.EppDelivery;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EppDeliveryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "employee.id", source = "employeeId")
    EppDelivery toEntity(EppDeliveryDTO eppDeliveryDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    EppDeliveryResponseDTO toResponseDto(EppDelivery eppDelivery);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    void partialUpdate(EppDeliveryDTO updateDTO, @MappingTarget EppDelivery eppDelivery);
}

