package PSG.backEnd.controller;

import PSG.backEnd.model.dto.supplier.SupplierDTO;
import PSG.backEnd.model.dto.supplier.SupplierFilterDTO;
import PSG.backEnd.model.dto.supplier.SupplierResponseDTO;
import PSG.backEnd.model.dto.supplier.SupplierStatsDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ISupplierService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import PSG.backEnd.model.constants.AppPermissions;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Management", description = "API for managing suppliers. Handles supplier registration, contact information, payment methods, and business relationships with vendors that provide goods or services to the organization.")
public class SupplierController {

    private final ISupplierService iSupplierService;

    @PreAuthorize("hasAuthority('" + AppPermissions.SUPPLIER_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new supplier",
            description = "Registers a new supplier in the system. Includes validation for unique CUIT (tax ID), address details, contact information, and accepted payment methods. " +
                    "Automatically handles previously deleted suppliers with the same CUIT by reactivating them.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Supplier successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (invalid CUIT format, missing required fields, etc.)"),
            @ApiResponse(responseCode = "409", description = "Supplier already exists with the same CUIT")
    })
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Supplier data including CUIT, legal and trade names, address, contact info, payment methods, and default discount",
                    required = true
            )
            @Validated(OnCreate.class) @RequestBody SupplierDTO supplierDTO) {
        SupplierResponseDTO createdSupplier = iSupplierService.createSupplier(supplierDTO);
        return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SUPPLIER_READ + "')")
    @GetMapping
    @Operation(summary = "Get all suppliers with filters",
            description = "Retrieves a paginated list of suppliers with optional filtering by CUIT, legal name, trade name, city, discount percentage range, and active status. " +
                    "Supports sorting by any field. Useful for searching suppliers by various criteria.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved supplier list")
    public ResponseEntity<Page<SupplierResponseDTO>> getSuppliers(
            @Parameter(description = "Filter by CUIT (tax ID) - partial match", example = "30-12345678-9") @RequestParam(required = false) String cuit,
            @Parameter(description = "Filter by legal name - partial match", example = "García") @RequestParam(required = false) String legalName,
            @Parameter(description = "Filter by trade name - partial match", example = "Construcciones") @RequestParam(required = false) String tradeName,
            @Parameter(description = "Filter by alias/nickname - partial match", example = "Ferretería") @RequestParam(required = false) String alias,
            @Parameter(description = "Filter by city - partial match", example = "Córdoba") @RequestParam(required = false) String city,
            @Parameter(description = "Filter by street - partial match", example = "San Martín") @RequestParam(required = false) String street,
            @Parameter(description = "Filter by minimum discount percentage", example = "5.00") @RequestParam(required = false) BigDecimal minDiscountPercentage,
            @Parameter(description = "Filter by maximum discount percentage", example = "15.00") @RequestParam(required = false) BigDecimal maxDiscountPercentage,
            @Parameter(description = "Filter by active status - true for active suppliers only", example = "true") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter suppliers that have pending balance (unpaid invoices)") @RequestParam(required = false) Boolean hasPendingBalance,
            @Parameter(description = "Generic search across CUIT, legalName and tradeName (partial match)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., legalName, tradeName, cuit, defaultDiscountPercentage)", example = "legalName") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)", example = "asc") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        SupplierFilterDTO filterDTO = new SupplierFilterDTO(
                cuit, legalName, tradeName, alias, city, street,
                minDiscountPercentage, maxDiscountPercentage, active, hasPendingBalance, search
        );

        return ResponseEntity.ok(iSupplierService.getAllSuppliers(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SUPPLIER_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID",
            description = "Retrieves detailed information about a specific supplier by its unique identifier, including full address, contact information, payment methods, and default discount.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier found"),
            @ApiResponse(responseCode = "404", description = "Supplier not found or has been deleted")
    })
    public ResponseEntity<SupplierResponseDTO> getSupplierById(
            @Parameter(description = "Supplier unique identifier", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(iSupplierService.getSupplierById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SUPPLIER_READ + "')")
    @GetMapping("/{id}/stats")
    @Operation(summary = "Get invoicing statistics for a supplier")
    @ApiResponse(responseCode = "200", description = "Supplier statistics")
    public ResponseEntity<SupplierStatsDTO> getSupplierStats(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(iSupplierService.getSupplierStats(id, fromDate, toDate));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SUPPLIER_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update supplier",
            description = "Updates an existing supplier's information. Only provided fields will be updated. " +
                    "Can update legal name, trade name, address, contact information, payment methods, default discount, and comments. " +
                    "Validates that the supplier exists and isn't marked as deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Supplier not found or has been deleted"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate CUIT")
    })
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @Parameter(description = "Supplier unique identifier", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Supplier data to update. Only include fields you want to modify.",
                    required = true
            )
            @Validated(OnUpdate.class) @RequestBody SupplierDTO supplierDTO) {
        return ResponseEntity.ok(iSupplierService.updateSupplier(id, supplierDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.SUPPLIER_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete supplier",
            description = "Performs a soft delete of a supplier from the system. The supplier is marked as deleted but remains in the database for historical purposes and audit trails. " +
                    "Cannot delete suppliers that are referenced in active documents or transactions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Supplier successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete supplier with active references (documents, payments, etc.)"),
            @ApiResponse(responseCode = "404", description = "Supplier not found")
    })
    public ResponseEntity<Void> deleteSupplier(
            @Parameter(description = "Supplier unique identifier", required = true, example = "1") @PathVariable Long id) {
        iSupplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
