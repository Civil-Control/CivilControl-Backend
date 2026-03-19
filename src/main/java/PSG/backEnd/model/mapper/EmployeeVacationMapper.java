package PSG.backEnd.model.mapper;
import PSG.backEnd.model.dto.employee.EmployeeVacationDTO;
import PSG.backEnd.model.dto.employee.EmployeeVacationResponseDTO;
import PSG.backEnd.model.entity.employee.EmployeeVacation;
import org.mapstruct.*;
@Mapper(componentModel = "spring")
public interface EmployeeVacationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "employee", ignore = true)
    EmployeeVacation toEntity(EmployeeVacationDTO employeeVacationDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    EmployeeVacationResponseDTO toResponseDto(EmployeeVacation employeeVacation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "employee", ignore = true)
    void partialUpdate(EmployeeVacationDTO updateDTO, @MappingTarget EmployeeVacation employeeVacation);
}
