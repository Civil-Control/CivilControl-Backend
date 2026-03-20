package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SalaryPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    @Mapping(target = "projectArea", ignore = true)
    SalaryPayment toEntity(SalaryPaymentDTO salaryPaymentDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectArea.color", target = "projectAreaColor")
    SalaryPaymentResponseDTO toResponseDto(SalaryPayment salaryPayment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    @Mapping(target = "projectArea", ignore = true)
    void partialUpdate(SalaryPaymentDTO updateDTO, @MappingTarget SalaryPayment salaryPayment);
}

