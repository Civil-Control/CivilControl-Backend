package PSG.backEnd.model.dto.crewAssignment;

import java.time.LocalTime;

public record CrewScheduleDefaultSaveDTO(
        Long projectAreaId,
        LocalTime departureTime,
        LocalTime returnTime
) {}
