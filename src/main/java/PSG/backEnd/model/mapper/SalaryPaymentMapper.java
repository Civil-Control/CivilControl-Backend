package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SalaryPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    SalaryPayment toEntity(SalaryPaymentDTO salaryPaymentDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    @Mapping(source = "employee.projectArea.name", target = "projectAreaName")
    SalaryPaymentResponseDTO toResponseDto(SalaryPayment salaryPayment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    void partialUpdate(SalaryPaymentDTO updateDTO, @MappingTarget SalaryPayment salaryPayment);
}

