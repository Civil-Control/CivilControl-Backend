package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.crewAssignment.*;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ICrewAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/crew-assignments")
@RequiredArgsConstructor
@Tag(name = "Crew Assignments", description = "API for managing daily crew assignments (parte diario de salida).")
public class CrewAssignmentController {

    private final ICrewAssignmentService iCrewAssignmentService;

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a single crew assignment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Assignment created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Duplicate assignment or driver conflict")
    })
    public ResponseEntity<CrewAssignmentResponseDTO> createCrewAssignment(
            @Validated(OnCreate.class) @RequestBody CrewAssignmentDTO dto) {
        CrewAssignmentResponseDTO created = iCrewAssignmentService.createCrewAssignment(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_WRITE + "')")
    @PostMapping("/batch")
    @Operation(summary = "Create multiple crew assignments (batch from drag & drop)")
    @ApiResponse(responseCode = "201", description = "Batch assignments created with possible warnings")
    public ResponseEntity<CrewAssignmentBatchResponseDTO> createBatchCrewAssignments(
            @Validated @RequestBody CrewAssignmentBatchDTO batchDTO) {
        CrewAssignmentBatchResponseDTO result = iCrewAssignmentService.createBatchCrewAssignments(batchDTO);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_READ + "')")
    @GetMapping
    @Operation(summary = "Get all crew assignments with filters and pagination")
    public ResponseEntity<Page<CrewAssignmentResponseDTO>> getCrewAssignments(
            @Parameter(description = "Filter by employee ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Filter by employee name (partial)") @RequestParam(required = false) String employeeName,
            @Parameter(description = "Filter by employee last name (partial)") @RequestParam(required = false) String employeeLastName,
            @Parameter(description = "Filter by employee DNI") @RequestParam(required = false) String employeeDni,
            @Parameter(description = "Filter by vehicle ID") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Filter by license plate (partial)") @RequestParam(required = false) String vehicleLicensePlate,
            @Parameter(description = "Filter by project area ID") @RequestParam(required = false) Long projectAreaId,
            @Parameter(description = "Filter by min date") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter by max date") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by exact date") @RequestParam(required = false) LocalDate dateExact,
            @Parameter(description = "Filter by driver flag") @RequestParam(required = false) Boolean isDriver,
            @Parameter(description = "Generic search") @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        String mappedSortBy = mapSortField(sortBy);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        CrewAssignmentFilterDTO filterDTO = new CrewAssignmentFilterDTO(
                employeeId, employeeName, employeeLastName, employeeDni,
                vehicleId, vehicleLicensePlate, projectAreaId,
                dateFrom, dateTo, dateExact, isDriver, search
        );

        return ResponseEntity.ok(iCrewAssignmentService.getAllCrewAssignments(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get crew assignment by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment found"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<CrewAssignmentResponseDTO> getCrewAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(iCrewAssignmentService.getCrewAssignmentById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_READ + "')")
    @GetMapping("/daily")
    @Operation(summary = "Get daily crew summary grouped by vehicle")
    public ResponseEntity<DailyCrewSummaryDTO> getDailyCrewSummary(
            @Parameter(description = "Date to query (YYYY-MM-DD)", required = true)
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(iCrewAssignmentService.getDailyCrewSummary(date));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_READ + "')")
    @GetMapping("/calendar")
    @Operation(summary = "Get calendar summary for a month")
    public ResponseEntity<CrewCalendarDTO> getCalendarSummary(
            @Parameter(description = "Year", required = true) @RequestParam int year,
            @Parameter(description = "Month (1-12)", required = true) @RequestParam int month) {
        return ResponseEntity.ok(iCrewAssignmentService.getCalendarSummary(year, month));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of a crew assignment")
    public ResponseEntity<CrewAssignmentResponseDTO> updateCrewAssignment(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody CrewAssignmentDTO dto) {
        return ResponseEntity.ok(iCrewAssignmentService.updateCrewAssignment(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_WRITE + "')")
    @PatchMapping("/{id}/driver")
    @Operation(summary = "Toggle driver flag on an assignment")
    public ResponseEntity<CrewAssignmentResponseDTO> toggleDriver(@PathVariable Long id) {
        return ResponseEntity.ok(iCrewAssignmentService.toggleDriver(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a crew assignment")
    public ResponseEntity<Void> deleteCrewAssignment(@PathVariable Long id) {
        iCrewAssignmentService.deleteCrewAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.CREW_ASSIGNMENT_DELETE + "')")
    @DeleteMapping("/daily")
    @Operation(summary = "Soft-delete all crew assignments for a given date")
    public ResponseEntity<Void> deleteDailyAssignments(
            @Parameter(description = "Date to delete (YYYY-MM-DD)", required = true)
            @RequestParam LocalDate date) {
        iCrewAssignmentService.deleteDailyAssignments(date);
        return ResponseEntity.noContent().build();
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "employeeName" -> "employee.name";
            case "employeeLastName" -> "employee.lastName";
            case "employeeDni" -> "employee.dni";
            case "vehicleLicensePlate" -> "vehicle.licensePlate";
            case "projectAreaName" -> "projectArea.name";
            default -> sortBy;
        };
    }
}
