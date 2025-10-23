package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryPaymentDTO(
    @NotNull(message = "Employee ID cannot be null", groups = OnCreate.class)
    Long employeeId,

    @NotNull(message = "Salary frequency cannot be null", groups = OnCreate.class)
    SalaryFrecuency salaryFrequency,

    @NotNull(message = "Payment date cannot be null", groups = OnCreate.class)
    @PastOrPresent(message = "Payment date cannot be in the future", groups = {OnCreate.class, OnUpdate.class})
    LocalDate paymentDate,

    @NotNull(message = "Amount cannot be null", groups = OnCreate.class)
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimal places", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal amount
) {}

