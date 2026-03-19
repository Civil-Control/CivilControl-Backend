package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.EmployeeDTO;
import PSG.backEnd.model.dto.employee.EmployeeResponseDTO;
import PSG.backEnd.model.entity.employee.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, EmergencyContactMapper.class, ProjectAreaMapper.class})
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "projectArea.id", source = "projectAreaId")
    Employee toEntity(EmployeeDTO employeeDTO);

    @Mapping(source = "projectArea", target = "projectArea")
    EmployeeResponseDTO toResponseDto(Employee employee);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    void partialUpdate(EmployeeDTO updateDTO, @MappingTarget Employee employee);
}

