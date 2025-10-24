package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a salary payment. " +
        "Represents a salary transaction for an employee, including payment details, frequency, and amount.")
public record SalaryPaymentDTO(
    @Schema(description = "Unique identifier of the employee receiving the salary payment. " +
            "Must reference an existing employee in the system.",
            example = "25",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Employee ID cannot be null", groups = OnCreate.class)
    Long employeeId,

    @Schema(description = "Frequency of the salary payment. Defines how often the employee is paid. Valid values: " +
            "MONTHLY (paid once per month), " +
            "BIWEEKLY (paid every two weeks, 26 times per year), " +
            "WEEKLY (paid once per week, 52 times per year).",
            example = "MONTHLY",
            allowableValues = {"MONTHLY", "BIWEEKLY", "WEEKLY"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Salary frequency cannot be null", groups = OnCreate.class)
    SalaryFrecuency salaryFrequency,

    @Schema(description = "Date when the salary payment was made or is scheduled to be made. " +
            "Cannot be in the future. Must be today or a past date for accurate payroll tracking.",
            example = "2025-10-20",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Payment date cannot be null", groups = OnCreate.class)
    @PastOrPresent(message = "Payment date cannot be in the future", groups = {OnCreate.class, OnUpdate.class})
    LocalDate paymentDate,

    @Schema(description = "Gross salary amount paid to the employee. Must be greater than zero. " +
            "Format: maximum 8 integer digits and 2 decimal places (e.g., 99999999.99). " +
            "Currency is assumed to be in Argentine Pesos (ARS) or the organization's default currency.",
            example = "150000.50",
            minimum = "0.01",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal")
    @NotNull(message = "Amount cannot be null", groups = OnCreate.class)
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimal places", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal amount
) {}

