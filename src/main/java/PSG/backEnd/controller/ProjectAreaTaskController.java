package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IProjectAreaTaskService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-area-tasks")
@RequiredArgsConstructor
@Tag(name = "Project Area Tasks", description = "API for managing sub-tasks within project areas.")
public class ProjectAreaTaskController {

    private final IProjectAreaTaskService taskService;

    @PreAuthorize("hasAuthority('" + AppPermissions.PROJECT_AREA_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new project area task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Project area not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate task name")
    })
    public ResponseEntity<ProjectAreaTaskResponseDTO> createTask(
            @Validated(OnCreate.class) @RequestBody ProjectAreaTaskDTO dto) {
        return new ResponseEntity<>(taskService.createTask(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PROJECT_AREA_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<ProjectAreaTaskResponseDTO> getTaskById(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.PROJECT_AREA_READ + "',"
        + "'" + AppPermissions.VEHICLE_WRITE + "',"
        + "'" + AppPermissions.FUEL_LOAD_WRITE + "',"
        + "'" + AppPermissions.EMPLOYEE_WRITE + "',"
        + "'" + AppPermissions.BUILDING_WRITE + "',"
        + "'" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "',"
        + "'" + AppPermissions.SALARY_PAYMENT_WRITE + "',"
        + "'" + AppPermissions.SERVICE_ASSIGNMENT_WRITE + "',"
        + "'" + AppPermissions.SERVICE_PAYMENT_WRITE + "',"
        + "'" + AppPermissions.SALES_DOCUMENT_WRITE + "',"
        + "'" + AppPermissions.WORK_CONTRACT_WRITE + "',"
        + "'" + AppPermissions.EPP_DELIVERY_WRITE + "'"
        + ")"
    )
    @GetMapping("/by-project-area/{projectAreaId}")
    @Operation(summary = "Get all tasks for a project area")
    @ApiResponse(responseCode = "200", description = "Task list retrieved")
    public ResponseEntity<List<ProjectAreaTaskResponseDTO>> getTasksByProjectArea(
            @Parameter(description = "Project area ID") @PathVariable Long projectAreaId) {
        return ResponseEntity.ok(taskService.getTasksByProjectArea(projectAreaId));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PROJECT_AREA_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update a project area task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<ProjectAreaTaskResponseDTO> updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody ProjectAreaTaskDTO dto) {
        return ResponseEntity.ok(taskService.updateTask(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PROJECT_AREA_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project area task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
