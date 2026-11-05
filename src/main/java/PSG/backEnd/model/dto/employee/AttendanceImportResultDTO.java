package PSG.backEnd.model.dto.employee;

import java.util.List;

public record AttendanceImportResultDTO(
    int totalRows,
    int validRows,
    int warningRows,
    int errorRows,
    boolean imported,
    List<AttendanceImportRowResultDTO> rows
) {}
