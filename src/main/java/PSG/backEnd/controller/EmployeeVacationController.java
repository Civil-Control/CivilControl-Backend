package PSG.backEnd.controller;

import PSG.backEnd.model.dto.employee.EmployeeVacationDTO;
import PSG.backEnd.model.dto.employee.EmployeeVacationFilterDTO;
import PSG.backEnd.model.dto.employee.EmployeeVacationResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IEmployeeVacationService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/employee-vacations")
@RequiredArgsConstructor
@Tag(name = "Employee Vacations", description = "API for managing employee vacation periods. Handles vacation requests, records, tracking of vacation dates, duration, and ensuring no overlapping vacation periods for the same employee.")
public class EmployeeVacationController {

    private final IEmployeeVacationService iEmployeeVacationService;

    @PostMapping
    @Operation(summary = "Create a new employee vacation",
            description = "Registers a new vacation period for an employee. Includes start date, end date, total days, " +
                    "and optional observations. Validates that dates are reasonable (not too far in the past or future), " +
                    "total days matches the date range, and there are no overlapping vacation periods for the same employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee vacation successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (e.g., end date before start date, total days mismatch, overlapping vacations)"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeVacationResponseDTO> createEmployeeVacation(
            @Validated(OnCreate.class) @RequestBody EmployeeVacationDTO employeeVacationDTO) {
        EmployeeVacationResponseDTO createdVacation = iEmployeeVacationService.createEmployeeVacation(employeeVacationDTO);
        return new ResponseEntity<>(createdVacation, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all employee vacations with filters",
            description = "Retrieves a paginated list of employee vacations with optional filtering by employee ID, " +
                    "start date range, end date range, and minimum/maximum total days. Supports sorting and pagination. " +
                    "Only returns non-deleted vacation records.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved employee vacation list")
    public ResponseEntity<Page<EmployeeVacationResponseDTO>> getEmployeeVacations(
            @Parameter(description = "Filter by employee ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Filter by minimum start date") @RequestParam(required = false) LocalDate startDateFrom,
            @Parameter(description = "Filter by maximum start date") @RequestParam(required = false) LocalDate startDateTo,
            @Parameter(description = "Filter by minimum end date") @RequestParam(required = false) LocalDate endDateFrom,
            @Parameter(description = "Filter by maximum end date") @RequestParam(required = false) LocalDate endDateTo,
            @Parameter(description = "Filter by minimum total days") @RequestParam(required = false) Integer minTotalDays,
            @Parameter(description = "Filter by maximum total days") @RequestParam(required = false) Integer maxTotalDays,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        EmployeeVacationFilterDTO filterDTO = new EmployeeVacationFilterDTO(
                employeeId,
                startDateFrom,
                startDateTo,
                endDateFrom,
                endDateTo,
                minTotalDays,
                maxTotalDays
        );

        return ResponseEntity.ok(iEmployeeVacationService.getAllEmployeeVacations(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee vacation by ID",
            description = "Retrieves detailed information about a specific employee vacation by its unique identifier. " +
                    "Includes employee details, vacation dates, total days, and observations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee vacation found"),
            @ApiResponse(responseCode = "404", description = "Employee vacation not found")
    })
    public ResponseEntity<EmployeeVacationResponseDTO> getEmployeeVacationById(
            @Parameter(description = "Employee vacation unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iEmployeeVacationService.getEmployeeVacationById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update employee vacation",
            description = "Updates an existing employee vacation record. Only provided fields will be updated. " +
                    "Allows updating employee, dates, total days, and observations. Validates business rules including " +
                    "date ranges, total days consistency, and no overlapping vacations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee vacation successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Employee vacation or employee not found")
    })
    public ResponseEntity<EmployeeVacationResponseDTO> updateEmployeeVacation(
            @Parameter(description = "Employee vacation unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody EmployeeVacationDTO employeeVacationDTO) {
        return ResponseEntity.ok(iEmployeeVacationService.updateEmployeeVacation(id, employeeVacationDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee vacation",
            description = "Soft deletes an employee vacation record from the system. The vacation record is marked as deleted " +
                    "but remains in the database for historical tracking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Employee vacation successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Employee vacation not found")
    })
    public ResponseEntity<Void> deleteEmployeeVacation(
            @Parameter(description = "Employee vacation unique identifier", required = true) @PathVariable Long id) {
        iEmployeeVacationService.deleteEmployeeVacation(id);
        return ResponseEntity.noContent().build();
    }
}

