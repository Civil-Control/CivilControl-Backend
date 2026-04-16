package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.crewAssignment.*;
import PSG.backEnd.service.port.ICrewAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/crew-reports")
@RequiredArgsConstructor
@Tag(name = "Crew Reports", description = "API for managing daily crew reports (parte diario) with grouped assignments.")
public class DailyCrewReportController {

    private final ICrewAssignmentService iCrewAssignmentService;

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
}
