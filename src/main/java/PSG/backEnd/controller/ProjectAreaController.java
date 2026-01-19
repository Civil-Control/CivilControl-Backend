package PSG.backEnd.controller;

import PSG.backEnd.model.dto.projectArea.ProjectAreaDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaFilterDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IProjectAreaService;
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

@RestController
@RequestMapping("/api/v1/project-areas")
@RequiredArgsConstructor
@Tag(name = "Project Area Management", description = "API for managing project areas. " +
        "Project areas represent organizational divisions, departments, or operational zones within the company, " +
        "such as construction sites, business units, or geographical regions. " +
        "They are used to organize and track employees, vehicles, expenses, and other resources.")
public class ProjectAreaController {

    private final IProjectAreaService iProjectAreaService;

    @PostMapping
    @Operation(summary = "Create a new project area",
            description = "Creates a new project area in the system. Project areas help organize resources, employees, and vehicles by division, department, or location. " +
                    "Includes validation for unique names and optional description and active status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project area successfully created. Returns the created area with its assigned ID."),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (missing required fields, name too short/long, etc.)"),
            @ApiResponse(responseCode = "409", description = "Project area already exists with the same name")
    })
    public ResponseEntity<ProjectAreaResponseDTO> createProjectArea(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Project area data including name, optional description, and active status",
                    required = true
            )
            @Validated(OnCreate.class) @RequestBody ProjectAreaDTO projectAreaDTO) {
        ProjectAreaResponseDTO createdProjectArea = iProjectAreaService.createProjectArea(projectAreaDTO);
        return new ResponseEntity<>(createdProjectArea, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all project areas with filters",
            description = "Retrieves a paginated list of project areas with optional filtering by name and active status. " +
                    "Supports sorting by any field. Useful for browsing organizational structure and filtering active/inactive areas.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved project areas list")
    public ResponseEntity<Page<ProjectAreaResponseDTO>> getProjectAreas(
            @Parameter(description = "Filter by project area name (partial match, case-insensitive)", example = "Obras Norte") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by active status - true for active areas only, false for inactive, omit for all", example = "true") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., name, active, id)", example = "name") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)", example = "asc") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        ProjectAreaFilterDTO filterDTO = new ProjectAreaFilterDTO(name, active);

        return ResponseEntity.ok(iProjectAreaService.getAllProjectAreas(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project area by ID",
            description = "Retrieves detailed information about a specific project area by its unique identifier, including name, description, and active status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project area found"),
            @ApiResponse(responseCode = "404", description = "Project area not found or has been deleted")
    })
    public ResponseEntity<ProjectAreaResponseDTO> getProjectAreaById(
            @Parameter(description = "Project area unique identifier", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(iProjectAreaService.getProjectAreaById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update project area",
            description = "Updates an existing project area. Only provided fields will be updated. " +
                    "Can update name, description, and active status. Validates that the area exists and isn't marked as deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project area successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Project area not found or has been deleted"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate project area name")
    })
    public ResponseEntity<ProjectAreaResponseDTO> updateProjectArea(
            @Parameter(description = "Project area unique identifier", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Project area data to update. Only include fields you want to modify.",
                    required = true
            )
            @Validated(OnUpdate.class) @RequestBody ProjectAreaDTO projectAreaDTO) {
        return ResponseEntity.ok(iProjectAreaService.updateProjectArea(id, projectAreaDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project area",
            description = "Performs a soft delete of a project area from the system. The area is marked as deleted but remains in the database for historical purposes and audit trails. " +
                    "Cannot delete project areas that have active assignments (employees, vehicles, etc.).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project area successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete project area with active assignments (employees, vehicles, expenses, etc.)"),
            @ApiResponse(responseCode = "404", description = "Project area not found")
    })
    public ResponseEntity<Void> deleteProjectArea(
            @Parameter(description = "Project area unique identifier", required = true, example = "1") @PathVariable Long id) {
        iProjectAreaService.deleteProjectArea(id);
        return ResponseEntity.noContent().build();
    }
}
