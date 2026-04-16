package PSG.backEnd.model.dto.report.salary;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single salary payment entry within the report")
public record SalaryReportPaymentDTO(

    @Schema(description = "Salary payment ID", example = "78")
    Long id,

    @Schema(description = "Date of the payment", example = "2026-03-15")
    LocalDate paymentDate,

    @Schema(description = "Payment amount", example = "150000.50")
    BigDecimal amount,

    @Schema(description = "Salary frequency", example = "MENSUAL")
    SalaryFrecuency salaryFrequency,

    @Schema(description = "Payment method used", nullable = true)
    PaymentMethod paymentMethod,

    @Schema(description = "Project area task ID", nullable = true)
    Long projectAreaTaskId,

    @Schema(description = "Project area task name", nullable = true)
    String projectAreaTaskName,

    @Schema(description = "Employee first name (for flat view)", nullable = true)
    String employeeName,

    @Schema(description = "Employee last name (for flat view)", nullable = true)
    String employeeLastName
) {}
