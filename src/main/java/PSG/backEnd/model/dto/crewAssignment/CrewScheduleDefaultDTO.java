package PSG.backEnd.model.dto.crewAssignment;

import java.time.LocalTime;

public record CrewScheduleDefaultDTO(
        Long id,
        Long projectAreaId,
        String projectAreaName,
        String projectAreaColor,
        LocalTime departureTime,
        LocalTime returnTime
) {}
