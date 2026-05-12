package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.DisciplinaryActionDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionResponseDTO;
import PSG.backEnd.model.entity.employee.DisciplinaryAction;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DisciplinaryActionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    @Mapping(target = "laborIncident", ignore = true)
    DisciplinaryAction toEntity(DisciplinaryActionDTO disciplinaryActionDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    @Mapping(source = "laborIncident.id", target = "laborIncidentId")
    @Mapping(source = "laborIncident.incidentType", target = "laborIncidentType")
    @Mapping(source = "laborIncident.incidentDate", target = "laborIncidentDate")
    DisciplinaryActionResponseDTO toResponseDto(DisciplinaryAction disciplinaryAction);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    @Mapping(target = "laborIncident", ignore = true)
    void partialUpdate(DisciplinaryActionDTO updateDTO, @MappingTarget DisciplinaryAction disciplinaryAction);
}

