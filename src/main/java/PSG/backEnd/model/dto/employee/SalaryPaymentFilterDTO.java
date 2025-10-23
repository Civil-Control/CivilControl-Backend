package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.SalaryFrecuency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryPaymentFilterDTO(
    Long employeeId,
    SalaryFrecuency salaryFrequency,
    LocalDate paymentDateFrom,
    LocalDate paymentDateTo,
    BigDecimal minAmount,
    BigDecimal maxAmount
) {}

