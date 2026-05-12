package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.laborIncident.LaborIncidentDTO;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentResponseDTO;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.employee.LaborIncident;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LaborIncidentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    LaborIncident toEntity(LaborIncidentDTO dto);

    @Mapping(target = "employees", source = "employees")
    LaborIncidentResponseDTO toResponseDto(LaborIncident entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "employees", ignore = true)
    void partialUpdate(LaborIncidentDTO dto, @MappingTarget LaborIncident entity);

    default LaborIncidentResponseDTO.EmployeeRef toEmployeeRef(Employee employee) {
        if (employee == null) return null;
        return new LaborIncidentResponseDTO.EmployeeRef(employee.getId(), employee.getName(), employee.getLastName());
    }
}
