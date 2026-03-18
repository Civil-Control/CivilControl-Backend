package PSG.backEnd.controller;

import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierResponseDTO;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IServiceSupplierService;
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
@RequestMapping("/api/v1/service-suppliers")
@RequiredArgsConstructor
@Tag(name = "Service Suppliers", description = "API for managing service suppliers. Handles suppliers that provide utility services such as electricity, water, gas, internet, and other municipal or provincial services.")
public class ServiceSupplierController {

    private final IServiceSupplierService serviceSupplierService;

    @PostMapping
    @Operation(summary = "Create a new service supplier",
            description = "Registers a new service supplier with the provided service types. Each supplier can only have one service supplier record. " +
                    "If a deleted service supplier exists for the same supplier, it will be reactivated with the new information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Service supplier successfully created or reactivated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (duplicate service types, empty service list)"),
            @ApiResponse(responseCode = "404", description = "Supplier not found"),
            @ApiResponse(responseCode = "409", description = "Service supplier already exists for this supplier")
    })
    public ResponseEntity<ServiceSupplierResponseDTO> createServiceSupplier(
            @Validated(OnCreate.class) @RequestBody ServiceSupplierDTO serviceSupplierDTO) {
        ServiceSupplierResponseDTO createdServiceSupplier =
                serviceSupplierService.createServiceSupplier(serviceSupplierDTO);
        return new ResponseEntity<>(createdServiceSupplier, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all service suppliers with filters",
            description = "Retrieves a paginated list of service suppliers with optional filtering by supplier name, CUIT, or service type. " +
                    "Supports sorting by any field and pagination. Only returns non-deleted service suppliers.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved service supplier list")
    public ResponseEntity<Page<ServiceSupplierResponseDTO>> getServiceSuppliers(
            @Parameter(description = "Filter by supplier name (searches in both legal name and trade name)")
            @RequestParam(required = false) String supplierName,

            @Parameter(description = "Filter by specific service type provided")
            @RequestParam(required = false) ServiceType serviceType,

            @Parameter(description = "Filter by supplier CUIT")
            @RequestParam(required = false) String cuit,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by. Available fields: id, legalName, tradeName, cuit, defaultDiscountPercentage",
                    example = "legalName")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Sort direction (asc or desc)")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        ServiceSupplierFilterDTO filterDTO = new ServiceSupplierFilterDTO(
                supplierName, serviceType, cuit
        );

        return ResponseEntity.ok(serviceSupplierService.getAllServiceSuppliers(filterDTO, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "legalName", "supplierName" -> "supplier.legalName";
            case "tradeName", "supplierTradeName" -> "supplier.tradeName";
            case "cuit", "supplierCuit" -> "supplier.cuit";
            case "defaultDiscountPercentage" -> "supplier.defaultDiscountPercentage";
            case "supplierId" -> "supplier.id";
            default -> sortBy; // For 'id' and any other fields
        };
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service supplier by ID",
            description = "Retrieves detailed information about a specific service supplier by its unique identifier, " +
                    "including the supplier details and all provided service types.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service supplier found"),
            @ApiResponse(responseCode = "404", description = "Service supplier not found or has been deleted")
    })
    public ResponseEntity<ServiceSupplierResponseDTO> getServiceSupplierById(
            @Parameter(description = "Service supplier unique identifier", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(serviceSupplierService.getServiceSupplierById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update service supplier",
            description = "Updates an existing service supplier record. Only provided fields will be updated. " +
                    "Can update the supplier association and the list of provided services. " +
                    "Validates that no duplicate service types are present and that the new supplier doesn't already have a service supplier record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service supplier successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (duplicate service types)"),
            @ApiResponse(responseCode = "404", description = "Service supplier or new supplier not found"),
            @ApiResponse(responseCode = "409", description = "Another service supplier already exists for the new supplier")
    })
    public ResponseEntity<ServiceSupplierResponseDTO> updateServiceSupplier(
            @Parameter(description = "Service supplier unique identifier", required = true)
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody ServiceSupplierDTO serviceSupplierDTO) {
        return ResponseEntity.ok(serviceSupplierService.updateServiceSupplier(id, serviceSupplierDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete service supplier",
            description = "Performs a soft deletion of a service supplier. The record is marked as deleted but remains in the database. " +
                    "This allows for reactivation if the same supplier needs to be registered again as a service supplier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Service supplier successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Service supplier not found or already deleted")
    })
    public ResponseEntity<Void> deleteServiceSupplier(
            @Parameter(description = "Service supplier unique identifier", required = true)
            @PathVariable Long id) {
        serviceSupplierService.deleteServiceSupplier(id);
        return ResponseEntity.noContent().build();
    }
}

