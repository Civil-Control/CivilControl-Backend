package PSG.backEnd.controller;

import PSG.backEnd.model.dto.employee.EmployeeDTO;
import PSG.backEnd.model.dto.employee.EmployeeFilterDTO;
import PSG.backEnd.model.dto.employee.EmployeeResponseDTO;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IEmployeeService;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "API for managing employees in the organization. Handles employee registration, personal information, employment details, contact information, and tracking of employment status and role assignments.")
public class EmployeeController {

    private final IEmployeeService iEmployeeService;

    @PostMapping
    @Operation(summary = "Create a new employee",
            description = "Registers a new employee in the system. Includes personal information (name, DNI, CUIL, birth date), " +
                    "contact details (address, phone, email), emergency contact, employment information (type, status, role, hire date), " +
                    "and project area assignment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Project area not found"),
            @ApiResponse(responseCode = "409", description = "Employee with this DNI or CUIL already exists")
    })
    public ResponseEntity<EmployeeResponseDTO> createEmployee(
            @Validated(OnCreate.class) @RequestBody EmployeeDTO employeeDTO) {
        EmployeeResponseDTO createdEmployee = iEmployeeService.createEmployee(employeeDTO);
        return new ResponseEntity<>(createdEmployee, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all employees with filters",
            description = "Retrieves a paginated list of employees with optional filtering by name, last name, DNI, CUIL, project area, " +
                    "city, employment type (permanent, temporary, contractor), employee status (active, inactive, suspended), " +
                    "employee role (driver, mechanic, administrator, etc.), and hire date range. Supports sorting and pagination.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved employee list")
    public ResponseEntity<Page<EmployeeResponseDTO>> getEmployees(
            @Parameter(description = "Filter by employee name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by employee last name (partial match)") @RequestParam(required = false) String lastName,
            @Parameter(description = "Filter by DNI number (exact match)") @RequestParam(required = false) String dni,
            @Parameter(description = "Filter by CUIL number (exact match)") @RequestParam(required = false) String cuil,
            @Parameter(description = "Filter by project area ID") @RequestParam(required = false) Long projectAreaId,
            @Parameter(description = "Filter by city of residence (partial match)") @RequestParam(required = false) String city,
            @Parameter(description = "Filter by employment type (PERMANENT, TEMPORARY, CONTRACTOR)") @RequestParam(required = false) EmploymentType employmentType,
            @Parameter(description = "Filter by employee status (ACTIVE, INACTIVE, SUSPENDED)") @RequestParam(required = false) EmployeeStatus employeeStatus,
            @Parameter(description = "Filter by employee role (DRIVER, MECHANIC, ADMINISTRATOR, etc.)") @RequestParam(required = false) EmployeeRole employeeRole,
            @Parameter(description = "Filter by minimum hire date") @RequestParam(required = false) LocalDate hireDateFrom,
            @Parameter(description = "Filter by maximum hire date") @RequestParam(required = false) LocalDate hireDateTo,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, name, lastName, dni, cuil, birthDate, hireDate, endDate, " +
                    "phoneNumber, email, employmentType, employeeStatus, employeeRole. " +
                    "For project area use: projectArea.name. " +
                    "Example: sortBy=projectArea.name",
                    example = "lastName")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        EmployeeFilterDTO filterDTO = new EmployeeFilterDTO(
                name, lastName, dni, cuil, projectAreaId, city,
                employmentType, employeeStatus, employeeRole,
                hireDateFrom, hireDateTo
        );

        return ResponseEntity.ok(iEmployeeService.getAllEmployees(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID",
            description = "Retrieves detailed information about a specific employee by their unique identifier. " +
                    "Includes all personal, contact, and employment information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(
            @Parameter(description = "Employee unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iEmployeeService.getEmployeeById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update employee",
            description = "Updates an existing employee record. Only provided fields will be updated. Allows updating personal information, " +
                    "contact details, employment status, role, project area assignment, and other employment details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Employee or project area not found"),
            @ApiResponse(responseCode = "409", description = "DNI or CUIL already exists for another employee")
    })
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(
            @Parameter(description = "Employee unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody EmployeeDTO employeeDTO) {
        return ResponseEntity.ok(iEmployeeService.updateEmployee(id, employeeDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee",
            description = "Deletes an employee record from the system. This operation cannot be undone. " +
                    "Note: Employees with associated records (salary payments, disciplinary actions, etc.) cannot be deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Employee successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "409", description = "Employee has associated records and cannot be deleted")
    })
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(description = "Employee unique identifier", required = true) @PathVariable Long id) {
        iEmployeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}

