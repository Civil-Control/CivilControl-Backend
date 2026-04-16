package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.crewAssignment.*;
import PSG.backEnd.repository.CrewScheduleDefaultRepository;
import PSG.backEnd.model.entity.vehicle.CrewScheduleDefault;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.service.port.ICrewAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/crew-reports")
@RequiredArgsConstructor
@Tag(name = "Crew Reports", description = "API for managing daily crew reports (parte diario) with grouped assignments.")
public class DailyCrewReportController {

    private final ICrewAssignmentService iCrewAssignmentService;
    private final CrewScheduleDefaultRepository scheduleDefaultRepo;
    private final ProjectAreaRepository projectAreaRepo;

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new daily crew report with assignments")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Report created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Driver conflict")
    })
    public ResponseEntity<DailyCrewReportResponseDTO> createReport(
            @Validated @RequestBody DailyCrewReportSaveDTO dto) {
        DailyCrewReportResponseDTO result = iCrewAssignmentService.saveReport(dto);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_WRITE + "')")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing daily crew report (replaces all assignments)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report updated"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<DailyCrewReportResponseDTO> updateReport(
            @PathVariable Long id,
            @Validated @RequestBody DailyCrewReportSaveDTO dto) {
        return ResponseEntity.ok(iCrewAssignmentService.updateReport(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get a crew report by ID with full details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report found"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<DailyCrewReportResponseDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(iCrewAssignmentService.getReportById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_READ + "')")
    @GetMapping("/daily")
    @Operation(summary = "Get summary of all crew reports for a date")
    public ResponseEntity<DailyReportsSummaryDTO> getDailyReportsSummary(
            @Parameter(description = "Date to query (YYYY-MM-DD)", required = true)
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(iCrewAssignmentService.getDailyReportsSummary(date));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a crew report and all its assignments")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        iCrewAssignmentService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    // ── Schedule Defaults ─────────────────────────────────────────

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_READ + "')")
    @GetMapping("/schedule-defaults")
    @Operation(summary = "Get default departure/return times for all sectors")
    public ResponseEntity<List<CrewScheduleDefaultDTO>> getScheduleDefaults() {
        Map<Long, CrewScheduleDefault> byArea = scheduleDefaultRepo.findAllWithProjectArea().stream()
                .collect(Collectors.toMap(d -> d.getProjectArea().getId(), d -> d));

        List<CrewScheduleDefaultDTO> result = projectAreaRepo
                .findAllWithFilters(null, true, null, Pageable.unpaged())
                .stream()
                .map(pa -> {
                    CrewScheduleDefault d = byArea.get(pa.getId());
                    return new CrewScheduleDefaultDTO(
                            d != null ? d.getId() : null,
                            pa.getId(),
                            pa.getName(),
                            pa.getColor(),
                            d != null ? d.getDepartureTime() : null,
                            d != null ? d.getReturnTime() : null
                    );
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_WRITE + "')")
    @PutMapping("/schedule-defaults")
    @Operation(summary = "Batch upsert default times for sectors")
    public ResponseEntity<List<CrewScheduleDefaultDTO>> saveScheduleDefaults(
            @RequestBody List<CrewScheduleDefaultSaveDTO> dtos) {
        List<CrewScheduleDefaultDTO> result = dtos.stream().map(dto -> {
            CrewScheduleDefault entity = scheduleDefaultRepo.findByProjectAreaId(dto.projectAreaId())
                    .orElseGet(() -> {
                        CrewScheduleDefault d = new CrewScheduleDefault();
                        ProjectArea pa = projectAreaRepo.getReferenceById(dto.projectAreaId());
                        d.setProjectArea(pa);
                        return d;
                    });
            entity.setDepartureTime(dto.departureTime());
            entity.setReturnTime(dto.returnTime());
            CrewScheduleDefault saved = scheduleDefaultRepo.save(entity);
            ProjectArea pa = saved.getProjectArea();
            return new CrewScheduleDefaultDTO(
                    saved.getId(), pa.getId(), pa.getName(), pa.getColor(),
                    saved.getDepartureTime(), saved.getReturnTime());
        }).toList();
        return ResponseEntity.ok(result);
    }
}
