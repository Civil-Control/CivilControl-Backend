package PSG.backEnd.controller;

import PSG.backEnd.model.dto.employee.DisciplinaryActionDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionFilterDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionResponseDTO;
import PSG.backEnd.model.enums.employee.ActionType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IDisciplinaryActionService;
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
@RequestMapping("/api/v1/disciplinary-actions")
@RequiredArgsConstructor
@Tag(name = "Disciplinary Actions", description = "API for managing employee disciplinary actions. Handles recording, tracking, and managing disciplinary measures such as warnings, suspensions, and terminations applied to employees.")
public class DisciplinaryActionController {

    private final IDisciplinaryActionService iDisciplinaryActionService;

    @PostMapping
    @Operation(summary = "Create a new disciplinary action",
            description = "Registers a new disciplinary action for an employee. Includes action type (warning, suspension, termination), " +
                    "reason, action date, optional end date for temporary measures, and additional notes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Disciplinary action successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "409", description = "Conflict with existing disciplinary action")
    })
    public ResponseEntity<DisciplinaryActionResponseDTO> createDisciplinaryAction(
            @Validated(OnCreate.class) @RequestBody DisciplinaryActionDTO disciplinaryActionDTO) {
        DisciplinaryActionResponseDTO createdAction = iDisciplinaryActionService.createDisciplinaryAction(disciplinaryActionDTO);
        return new ResponseEntity<>(createdAction, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all disciplinary actions with filters",
            description = "Retrieves a paginated list of disciplinary actions with optional filtering by employee, action type, " +
                    "action date range, and end date range. Supports sorting and pagination. Useful for employee disciplinary history tracking.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved disciplinary actions list")
    public ResponseEntity<Page<DisciplinaryActionResponseDTO>> getDisciplinaryActions(
            @Parameter(description = "Filter by employee ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Filter by action type (WARNING, SUSPENSION, TERMINATION)") @RequestParam(required = false) ActionType actionType,
            @Parameter(description = "Filter by minimum action date") @RequestParam(required = false) LocalDate actionDateFrom,
            @Parameter(description = "Filter by maximum action date") @RequestParam(required = false) LocalDate actionDateTo,
            @Parameter(description = "Filter by minimum end date") @RequestParam(required = false) LocalDate endDateFrom,
            @Parameter(description = "Filter by maximum end date") @RequestParam(required = false) LocalDate endDateTo,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, actionDate, endDate, actionType, reason. " +
                    "For employee fields use: employeeName, employeeLastName, employeeDni, employeeCuil, employeeId. " +
                    "Example: sortBy=employeeLastName",
                    example = "actionDate")
            @RequestParam(defaultValue = "actionDate") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        DisciplinaryActionFilterDTO filterDTO = new DisciplinaryActionFilterDTO(
                employeeId, actionType, actionDateFrom, actionDateTo,
                endDateFrom, endDateTo
        );

        return ResponseEntity.ok(iDisciplinaryActionService.getAllDisciplinaryActions(filterDTO, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "employeeName" -> "employee.name";
            case "employeeLastName" -> "employee.lastName";
            case "employeeDni" -> "employee.dni";
            case "employeeCuil" -> "employee.cuil";
            case "employeeId" -> "employee.id";
            default -> sortBy; // For 'id', 'actionDate', 'endDate', 'actionType', 'reason', etc.
        };
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get disciplinary action by ID",
            description = "Retrieves detailed information about a specific disciplinary action by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disciplinary action found"),
            @ApiResponse(responseCode = "404", description = "Disciplinary action not found")
    })
    public ResponseEntity<DisciplinaryActionResponseDTO> getDisciplinaryActionById(
            @Parameter(description = "Disciplinary action unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iDisciplinaryActionService.getDisciplinaryActionById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update disciplinary action",
            description = "Updates an existing disciplinary action record. Only provided fields will be updated. Allows updating action type, " +
                    "reason, dates, and notes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disciplinary action successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Disciplinary action or employee not found"),
            @ApiResponse(responseCode = "409", description = "Update conflict")
    })
    public ResponseEntity<DisciplinaryActionResponseDTO> updateDisciplinaryAction(
            @Parameter(description = "Disciplinary action unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody DisciplinaryActionDTO disciplinaryActionDTO) {
        return ResponseEntity.ok(iDisciplinaryActionService.updateDisciplinaryAction(id, disciplinaryActionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete disciplinary action",
            description = "Deletes a disciplinary action record from the system. This operation cannot be undone. " +
                    "Use with caution as it removes the disciplinary history record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Disciplinary action successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Disciplinary action not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete disciplinary action due to constraints")
    })
    public ResponseEntity<Void> deleteDisciplinaryAction(
            @Parameter(description = "Disciplinary action unique identifier", required = true) @PathVariable Long id) {
        iDisciplinaryActionService.deleteDisciplinaryAction(id);
        return ResponseEntity.noContent().build();
    }
}

