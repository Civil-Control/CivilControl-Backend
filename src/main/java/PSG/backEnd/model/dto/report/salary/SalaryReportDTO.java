package PSG.backEnd.model.dto.report.salary;

import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Complete salary report with hierarchical grouping: Area → Employee → Payments")
public record SalaryReportDTO(

    @Schema(description = "Filters applied to generate this report")
    SalaryReportFilterDTO filters,

    @Schema(description = "Data grouped by project area, then by employee")
    List<SalaryReportAreaGroupDTO> areaGroups,

    @Schema(description = "Grand total of all salary payments in the period", example = "5200000.00")
    BigDecimal totalAmount,

    @Schema(description = "Total number of salary payments", example = "87")
    int totalCount,

    @Schema(description = "Grand totals by salary frequency. Only frequencies with payments are included.")
    Map<SalaryFrecuency, BigDecimal> totalsByFrequency,

    @Schema(description = "Timestamp when this report was generated")
    LocalDateTime generatedAt,

    @Schema(description = "Display name for the report", example = "Reporte de Salarios")
    String reportName,

    @Schema(description = "Description of the report period", example = "Período: 01/01/2026 - 31/03/2026")
    String periodDescription
) {}
