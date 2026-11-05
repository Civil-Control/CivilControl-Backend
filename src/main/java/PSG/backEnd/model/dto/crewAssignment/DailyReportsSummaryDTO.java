package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Summary of all crew reports for a given date.")
public record DailyReportsSummaryDTO(
    @Schema(description = "The date.", example = "2026-04-04")
    LocalDate date,

    @Schema(description = "List of reports for this date.")
    List<DailyReportSummaryItemDTO> reports
) {}
