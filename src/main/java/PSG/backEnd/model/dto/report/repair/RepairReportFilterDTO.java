package PSG.backEnd.model.dto.report.repair;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filters for the repair report")
public record RepairReportFilterDTO(

    @Schema(description = "Start date (inclusive)")
    LocalDate startDate,

    @Schema(description = "End date (inclusive)")
    LocalDate endDate,

    @Schema(description = "Filter by project area IDs")
    List<Long> projectAreaIds,

    @Schema(description = "Filter by vehicle ID")
    Long vehicleId,

    @Schema(description = "Filter by supplier ID (external repairs)")
    Long supplierId,

    @Schema(description = "Minimum amount (inclusive)")
    BigDecimal minAmount,

    @Schema(description = "Maximum amount (inclusive)")
    BigDecimal maxAmount,

    @Schema(description = "When true, include records with no project area assigned")
    Boolean includeUnassigned
) {}
