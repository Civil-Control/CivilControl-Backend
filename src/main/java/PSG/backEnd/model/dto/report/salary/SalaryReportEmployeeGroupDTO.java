package PSG.backEnd.model.dto.report.salary;

import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Group of salary payments for a single employee within an area")
public record SalaryReportEmployeeGroupDTO(

    @Schema(description = "Employee ID", example = "25")
    Long employeeId,

    @Schema(description = "Employee first name", example = "Juan Carlos")
    String employeeName,

    @Schema(description = "Employee last name", example = "García Pérez")
    String employeeLastName,

    @Schema(description = "Total amount paid to this employee in the period", example = "450000.00")
    BigDecimal totalAmount,

    @Schema(description = "Total number of payments for this employee")
    int paymentCount,

    @Schema(description = "Subtotals by salary frequency. Only frequencies with payments are included.")
    Map<SalaryFrecuency, BigDecimal> subtotalsByFrequency,

    @Schema(description = "Individual payments for this employee, ordered by date descending")
    List<SalaryReportPaymentDTO> payments
) {}
