package PSG.backEnd.model.dto.report.salary;

import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Group of salary payments for a project area (sector)")
public record SalaryReportAreaGroupDTO(

    @Schema(description = "Project area ID. Null for employees with no area assigned.", nullable = true)
    Long projectAreaId,

    @Schema(description = "Project area name", example = "Obras Norte")
    String projectAreaName,

    @Schema(description = "Project area color hex code", example = "#3B82F6", nullable = true)
    String projectAreaColor,

    @Schema(description = "Total amount for this area in the period", example = "1250000.00")
    BigDecimal subtotalAmount,

    @Schema(description = "Total number of payments in this area")
    int paymentCount,

    @Schema(description = "Subtotals by salary frequency for this area. Only frequencies with payments are included.")
    Map<SalaryFrecuency, BigDecimal> subtotalsByFrequency,

    @Schema(description = "Employee groups within this area, ordered alphabetically by last name then first name")
    List<SalaryReportEmployeeGroupDTO> employeeGroups
) {}
