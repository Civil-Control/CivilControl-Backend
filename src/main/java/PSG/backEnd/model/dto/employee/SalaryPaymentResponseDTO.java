package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.SalaryFrecuency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryPaymentResponseDTO(
    Long id,
    Long employeeId,
    String employeeName,
    String employeeLastName,
    SalaryFrecuency salaryFrequency,
    LocalDate paymentDate,
    BigDecimal amount
) {}

