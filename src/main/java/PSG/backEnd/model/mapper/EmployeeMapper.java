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
    @Mapping(target = "projectAreaTask", ignore = true)
    Employee toEntity(EmployeeDTO employeeDTO);

    @Mapping(source = "projectArea", target = "projectArea")
    @Mapping(source = "projectAreaTask.id", target = "projectAreaTaskId")
    @Mapping(source = "projectAreaTask.name", target = "projectAreaTaskName")
    EmployeeResponseDTO toResponseDto(Employee employee);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    void partialUpdate(EmployeeDTO updateDTO, @MappingTarget Employee employee);
}

