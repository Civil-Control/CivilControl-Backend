package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Calendar summary for a month showing daily crew counts.")
public record CrewCalendarDTO(
    @Schema(description = "Year.", example = "2026")
    int year,
    @Schema(description = "Month (1-12).", example = "4")
    int month,
    @Schema(description = "Days with crew data.")
    List<CrewCalendarDayDTO> days
) {}
