package PSG.backEnd.controller;

import PSG.backEnd.model.dto.employee.EppDeliveryBatchDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryFilterDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IEppDeliveryService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/epp-deliveries")
@RequiredArgsConstructor
@Tag(name = "EPP Deliveries", description = "API for managing EPP (Personal Protective Equipment) deliveries to employees. " +
        "Handles recording, tracking, and managing safety equipment distribution including helmets, boots, vests, gloves, and other protective gear.")
public class EppDeliveryController {

    private final IEppDeliveryService iEppDeliveryService;

    @PostMapping
    @Operation(summary = "Create a new EPP delivery",
            description = "Registers a new EPP delivery to an employee. Records the delivery date, item details (name, type, brand), " +
                    "and quantity. Used for tracking safety equipment distribution and compliance with occupational safety regulations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "EPP delivery successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EppDeliveryResponseDTO> createEppDelivery(
            @Validated(OnCreate.class) @RequestBody EppDeliveryDTO eppDeliveryDTO) {
        EppDeliveryResponseDTO createdEppDelivery = iEppDeliveryService.createEppDelivery(eppDeliveryDTO);
        return new ResponseEntity<>(createdEppDelivery, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple EPP deliveries in batch",
            description = "Creates multiple EPP delivery records in a single request. Each delivery is processed individually. " +
                    "Useful for recording distribution of safety equipment to multiple employees at once.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "EPP deliveries successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "One or more employees not found")
    })
    public ResponseEntity<List<EppDeliveryResponseDTO>> createBatchEppDeliveries(
            @Validated @RequestBody EppDeliveryBatchDTO batchDTO) {
        List<EppDeliveryResponseDTO> created = iEppDeliveryService.createBatchEppDeliveries(batchDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all EPP deliveries with filters",
            description = "Retrieves a paginated list of EPP deliveries with optional filtering by employee, delivery date range, " +
                    "item name, item type, and brand. Supports sorting and pagination. Useful for generating reports on safety equipment " +
                    "distribution, tracking compliance, and managing inventory of protective gear.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved EPP deliveries list")
    public ResponseEntity<Page<EppDeliveryResponseDTO>> getEppDeliveries(
            @Parameter(description = "Filter by employee ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Search by employee name or last name (case-insensitive search)") @RequestParam(required = false) String employeeSearch,
            @Parameter(description = "Filter by minimum delivery date") @RequestParam(required = false) LocalDate deliveryDateFrom,
            @Parameter(description = "Filter by maximum delivery date") @RequestParam(required = false) LocalDate deliveryDateTo,
            @Parameter(description = "Filter by item name (case-insensitive search)") @RequestParam(required = false) String itemName,
            @Parameter(description = "Filter by item type (case-insensitive search)") @RequestParam(required = false) String itemType,
            @Parameter(description = "Filter by brand (case-insensitive search)") @RequestParam(required = false) String brand,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, deliveryDate, itemName, itemType, brand, quantity. " +
                    "For employee fields use: employeeName, employeeLastName, employeeDni, employeeCuil, employeeId. " +
                    "Example: sortBy=employeeLastName",
                    example = "deliveryDate")
            @RequestParam(defaultValue = "deliveryDate") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        EppDeliveryFilterDTO filterDTO = new EppDeliveryFilterDTO(
                employeeId, employeeSearch, deliveryDateFrom, deliveryDateTo,
                itemName, itemType, brand
        );

        return ResponseEntity.ok(iEppDeliveryService.getAllEppDeliveries(filterDTO, pageable));
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
            default -> sortBy; // For 'id', 'deliveryDate', 'itemName', 'itemType', 'brand', 'quantity', etc.
        };
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get EPP delivery by ID",
            description = "Retrieves detailed information about a specific EPP delivery by its unique identifier. " +
                    "Includes employee information, delivery date, item details (name, type, brand), and quantity delivered.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EPP delivery found"),
            @ApiResponse(responseCode = "404", description = "EPP delivery not found")
    })
    public ResponseEntity<EppDeliveryResponseDTO> getEppDeliveryById(
            @Parameter(description = "EPP delivery unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iEppDeliveryService.getEppDeliveryById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update EPP delivery",
            description = "Updates an existing EPP delivery record. Only provided fields will be updated. Allows updating delivery date, " +
                    "item details, quantity, and employee assignment. Useful for correcting errors in delivery records.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EPP delivery successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "EPP delivery or employee not found")
    })
    public ResponseEntity<EppDeliveryResponseDTO> updateEppDelivery(
            @Parameter(description = "EPP delivery unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody EppDeliveryDTO eppDeliveryDTO) {
        EppDeliveryResponseDTO updatedEppDelivery = iEppDeliveryService.updateEppDelivery(id, eppDeliveryDTO);
        return ResponseEntity.ok(updatedEppDelivery);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete EPP delivery",
            description = "Soft deletes an EPP delivery record. The record is marked as deleted but remains in the database for audit purposes. " +
                    "Use with caution as this affects safety equipment tracking records.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "EPP delivery successfully deleted"),
            @ApiResponse(responseCode = "404", description = "EPP delivery not found")
    })
    public ResponseEntity<Void> deleteEppDelivery(
            @Parameter(description = "EPP delivery unique identifier", required = true) @PathVariable Long id) {
        iEppDeliveryService.deleteEppDelivery(id);
        return ResponseEntity.noContent().build();
    }
}

