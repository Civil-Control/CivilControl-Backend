package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.crewAssignment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ICrewAssignmentService {
    CrewAssignmentResponseDTO createCrewAssignment(CrewAssignmentDTO dto);
    CrewAssignmentBatchResponseDTO createBatchCrewAssignments(CrewAssignmentBatchDTO batchDTO);
    Page<CrewAssignmentResponseDTO> getAllCrewAssignments(CrewAssignmentFilterDTO filterDTO, Pageable pageable);
    CrewAssignmentResponseDTO getCrewAssignmentById(Long id);
    DailyCrewSummaryDTO getDailyCrewSummary(LocalDate date);
    CrewCalendarDTO getCalendarSummary(int year, int month);
    CrewAssignmentResponseDTO updateCrewAssignment(Long id, CrewAssignmentDTO dto);
    CrewAssignmentResponseDTO toggleDriver(Long id);
    void deleteCrewAssignment(Long id);
    int deleteDailyAssignments(LocalDate date);

    // ── Report-level operations ────────────────────────────────────
    DailyCrewReportResponseDTO saveReport(DailyCrewReportSaveDTO dto);
    DailyCrewReportResponseDTO updateReport(Long reportId, DailyCrewReportSaveDTO dto);
    DailyCrewReportResponseDTO getReportById(Long reportId);
    DailyReportsSummaryDTO getDailyReportsSummary(LocalDate date);
    void deleteReport(Long reportId);
}
