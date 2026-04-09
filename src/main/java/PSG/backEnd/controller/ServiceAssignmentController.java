package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentResponseDTO;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IServiceAssignmentService;
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

@RestController
@RequestMapping("/api/v1/service-assignments")
@RequiredArgsConstructor
@Tag(name = "Service Assignments", description = "API for managing service assignments. " +
        "Represents the binding of a specific service from a supplier to a building, " +
        "including account information, due dates, and cost attribution.")
public class ServiceAssignmentController {

    private final IServiceAssignmentService serviceAssignmentService;

    @PreAuthorize("hasAuthority('" + AppPermissions.SERVICE_ASSIGNMENT_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new service assignment",
            description = "Registers a new service assignment binding a supplier's service to a building. " +
                    "Validates that the service type is provided by the specified supplier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Service assignment successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or service type not provided by supplier"),
            @ApiResponse(responseCode = "404", description = "Service supplier, building, or project area not found"),
            @ApiResponse(responseCode = "409", description = "Service assignment already exists for this combination")
    })
    public ResponseEntity<ServiceAssignmentResponseDTO> createServiceAssignment(
            @Validated(OnCreate.class) @RequestBody ServiceAssignmentDTO dto) {
        ServiceAssignmentResponseDTO created = serviceAssignmentService.createServiceAssignment(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SERVICE_ASSIGNMENT_READ + "')")
    @GetMapping
    @Operation(summary = "Get all service assignments with filters",
            description = "Retrieves a paginated list of service assignments with optional filtering.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved service assignments list")
    public ResponseEntity<Page<ServiceAssignmentResponseDTO>> getServiceAssignments(
            @Parameter(description = "Filter by subject type (BUILDING or VEHICLE)")
            @RequestParam(required = false) SubjectType subjectType,

            @Parameter(description = "Filter by service supplier ID")
            @RequestParam(required = false) Long serviceSupplierId,

            @Parameter(description = "Filter by building ID")
            @RequestParam(required = false) Long buildingId,

            @Parameter(description = "Filter by vehicle ID")
            @RequestParam(required = false) Long vehicleId,

            @Parameter(description = "Filter by service type")
            @RequestParam(required = false) ServiceType serviceType,

            @Parameter(description = "Generic search across supplier name, building name, vehicle plate, account holder")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Sort direction (asc or desc)")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        String mappedSortBy = mapSortField(sortBy);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        ServiceAssignmentFilterDTO filterDTO = new ServiceAssignmentFilterDTO(
                subjectType, serviceSupplierId, buildingId, vehicleId, serviceType, search
        );

        return ResponseEntity.ok(serviceAssignmentService.getAllServiceAssignments(filterDTO, pageable));
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "supplierName" -> "serviceSupplier.supplier.legalName";
            case "supplierTradeName" -> "serviceSupplier.supplier.tradeName";
            case "buildingName" -> "building.name";
            case "vehicleLicensePlate" -> "vehicle.licensePlate";
            case "paymentLocationName" -> "paymentLocation.name";
            default -> sortBy;
        };
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SERVICE_ASSIGNMENT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get service assignment by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service assignment found"),
            @ApiResponse(responseCode = "404", description = "Service assignment not found")
    })
    public ResponseEntity<ServiceAssignmentResponseDTO> getServiceAssignmentById(
            @PathVariable Long id) {
        return ResponseEntity.ok(serviceAssignmentService.getServiceAssignmentById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SERVICE_ASSIGNMENT_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update service assignment",
            description = "Updates an existing service assignment. Only provided fields will be updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service assignment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Service assignment not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate assignment for new combination")
    })
    public ResponseEntity<ServiceAssignmentResponseDTO> updateServiceAssignment(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody ServiceAssignmentDTO dto) {
        return ResponseEntity.ok(serviceAssignmentService.updateServiceAssignment(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SERVICE_ASSIGNMENT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete service assignment",
            description = "Performs a soft deletion of a service assignment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Service assignment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Service assignment not found")
    })
    public ResponseEntity<Void> deleteServiceAssignment(@PathVariable Long id) {
        serviceAssignmentService.deleteServiceAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
