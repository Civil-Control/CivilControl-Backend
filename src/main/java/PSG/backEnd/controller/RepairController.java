package PSG.backEnd.controller;

import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairFilterDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IRepairService;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import PSG.backEnd.model.constants.AppPermissions;

@RestController
@RequestMapping("/api/v1/repairs")
@RequiredArgsConstructor
@Tag(name = "Repairs", description = "API for managing vehicle repairs. Handles both internal repairs performed by employees and external repairs performed by suppliers.")
public class RepairController {

    private final IRepairService repairService;

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new repair",
               description = "Creates a new vehicle repair record. The repair can be performed either by an internal employee or an external supplier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Repair successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Vehicle or Supplier not found")
    })
    public ResponseEntity<RepairResponseDTO> createRepair(
            @Validated(OnCreate.class) @RequestBody RepairDTO repairDTO) {
        RepairResponseDTO createdRepair = repairService.createRepair(repairDTO);
        return new ResponseEntity<>(createdRepair, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_READ + "')")
    @GetMapping
    @Operation(summary = "Get all repairs with filters",
               description = "Retrieves a paginated list of repairs with optional filtering.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved repair list")
    public ResponseEntity<Page<RepairResponseDTO>> getRepairs(
            @Parameter(description = "Filter repairs from this date (inclusive)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter repairs to this date (inclusive)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by vehicle ID") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Filter by vehicle license plate") @RequestParam(required = false) String vehicleLicensePlate,
            @Parameter(description = "Filter by project area ID") @RequestParam(required = false) Long projectAreaId,
            @Parameter(description = "Minimum total cost") @RequestParam(required = false) BigDecimal minCost,
            @Parameter(description = "Maximum total cost") @RequestParam(required = false) BigDecimal maxCost,
            @Parameter(description = "Filter by supplier ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by supplier legal name") @RequestParam(required = false) String supplierLegalName,
            @Parameter(description = "Filter by repair description") @RequestParam(required = false) String description,
            @Parameter(description = "Filter by item description (materials, labor)") @RequestParam(required = false) String itemDescription,
            @Parameter(description = "Minimum mileage") @RequestParam(required = false) Integer minMileage,
            @Parameter(description = "Maximum mileage") @RequestParam(required = false) Integer maxMileage,
            @Parameter(description = "Generic search across vehicle license plate, supplier and item descriptions") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by linked transactional document ID on any item") @RequestParam(required = false) Long transactionalDocumentId,
            @Parameter(description = "When true, return only repairs with all items unlinked") @RequestParam(required = false) Boolean unlinked,
            @Parameter(description = "Filter by exact repair date") @RequestParam(required = false) LocalDate date,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by.") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        RepairFilterDTO filterDTO = new RepairFilterDTO(
                dateFrom, dateTo, vehicleId, vehicleLicensePlate, projectAreaId,
                minCost, maxCost, supplierId, supplierLegalName, description, itemDescription,
                minMileage, maxMileage, search, transactionalDocumentId, unlinked, date
        );

        return ResponseEntity.ok(repairService.getAllRepairs(filterDTO, pageable));
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "vehicleLicensePlate" -> "vehicle.licensePlate";
            case "vehicleBrand" -> "vehicle.brand";
            case "vehicleModel" -> "vehicle.model";
            case "vehicleId" -> "vehicle.id";
            case "supplierLegalName" -> "supplier.legalName";
            case "supplierTradeName" -> "supplier.tradeName";
            case "supplierCuit" -> "supplier.cuit";
            case "supplierId" -> "supplier.id";
            default -> sortBy;
        };
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get repair by ID",
               description = "Retrieves detailed information about a specific repair.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repair found"),
            @ApiResponse(responseCode = "404", description = "Repair not found")
    })
    public ResponseEntity<RepairResponseDTO> getRepairById(
            @Parameter(description = "Repair unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(repairService.getRepairById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update repair",
               description = "Updates an existing repair record. Only provided fields will be updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repair successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Repair not found")
    })
    public ResponseEntity<RepairResponseDTO> updateRepair(
            @Parameter(description = "Repair unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody RepairDTO repairDTO) {
        return ResponseEntity.ok(repairService.updateRepair(id, repairDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete repair",
               description = "Deletes a repair record from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Repair successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Repair not found")
    })
    public ResponseEntity<Void> deleteRepair(
            @Parameter(description = "Repair unique identifier", required = true) @PathVariable Long id) {
        repairService.deleteRepair(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_WRITE + "')")
    @PatchMapping("/items/{itemId}/link/{documentId}")
    @Operation(summary = "Link a single repair item to a transactional document",
               description = "Sets the transactional document on a specific repair item.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item successfully linked"),
            @ApiResponse(responseCode = "404", description = "Item or document not found")
    })
    public ResponseEntity<Void> linkItemToDocument(
            @PathVariable Long itemId,
            @PathVariable Long documentId,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        java.math.BigDecimal ivaPercentage = null;
        Integer sortOrder = null;
        if (body != null) {
            Object iva = body.get("ivaPercentage");
            if (iva != null) {
                ivaPercentage = new java.math.BigDecimal(iva.toString());
            }
            Object so = body.get("sortOrder");
            if (so == null) {
                so = body.get("documentSortOrder");
            }
            if (so != null) {
                sortOrder = Integer.valueOf(so.toString());
            }
        }
        repairService.linkItemToDocument(itemId, documentId, ivaPercentage, sortOrder);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_WRITE + "')")
    @PatchMapping("/items/{itemId}/unlink")
    @Operation(summary = "Unlink a single repair item from its transactional document",
               description = "Removes the transactional document from a specific repair item.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item successfully unlinked"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<Void> unlinkItem(
            @PathVariable Long itemId) {
        repairService.unlinkItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_WRITE + "')")
    @PatchMapping("/items/{itemId}/amount")
    @Operation(summary = "Update amount of a repair item",
               description = "Updates the amount of a specific repair item and recalculates linked document totals.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Amount successfully updated"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<Void> updateItemAmount(
            @PathVariable Long itemId,
            @RequestBody java.util.Map<String, BigDecimal> body) {
        repairService.updateItemAmount(itemId, body.get("amount"));
        return ResponseEntity.noContent().build();
    }
}
