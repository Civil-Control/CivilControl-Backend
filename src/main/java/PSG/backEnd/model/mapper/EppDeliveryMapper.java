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

    @Mapping(source = "eppDelivery.employee.id", target = "employeeId")
    @Mapping(source = "eppDelivery.employee.name", target = "employeeName")
    @Mapping(source = "eppDelivery.employee.lastName", target = "employeeLastName")
    @Mapping(source = "eppDelivery.stockId", target = "stockId")
    @Mapping(source = "stockName", target = "stockName")
    EppDeliveryResponseDTO toResponseDto(EppDelivery eppDelivery, String stockName);

    /** Backwards-compatible overload (no linked stock name resolved). */
    default EppDeliveryResponseDTO toResponseDto(EppDelivery eppDelivery) {
        return toResponseDto(eppDelivery, null);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "stockId", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    void partialUpdate(EppDeliveryDTO updateDTO, @MappingTarget EppDelivery eppDelivery);
}

