package PSG.backEnd.model.dto.report.repair;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Root DTO for the repair report")
public record RepairReportDTO(

    @Schema(description = "Filters used to generate this report")
    RepairReportFilterDTO filters,

    @Schema(description = "Area groups (layer 1)")
    List<RepairReportAreaGroupDTO> areaGroups,

    @Schema(description = "Grand total amount")
    BigDecimal totalAmount,

    @Schema(description = "Total number of repairs")
    int totalCount,

    @Schema(description = "Total material cost")
    BigDecimal totalMaterialCost,

    @Schema(description = "Total labor cost")
    BigDecimal totalLaborCost,

    @Schema(description = "Totals by item type (MATERIAL, MANO_DE_OBRA)")
    Map<String, BigDecimal> totalsByItemType,

    @Schema(description = "Report generation timestamp")
    LocalDateTime generatedAt,

    @Schema(description = "Report name")
    String reportName,

    @Schema(description = "Human-readable period description")
    String periodDescription
) {}
