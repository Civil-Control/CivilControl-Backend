package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.vehicle.*;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IRepairOrderService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;

@RestController
@RequestMapping("/api/v1/repair-orders")
@RequiredArgsConstructor
@Tag(name = "Repair Orders", description = "API for managing vehicle repair orders. Field operators report failures and workshop staff manage the repair workflow.")
public class RepairOrderController {

    private final IRepairOrderService repairOrderService;

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_CREATE + "')")
    @PostMapping
    @Operation(summary = "Create a new repair order",
               description = "Creates a new repair order with status PENDIENTE. The order is automatically linked to the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Repair order successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<RepairOrderResponseDTO> createRepairOrder(
            @Validated(OnCreate.class) @RequestBody RepairOrderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repairOrderService.createRepairOrder(dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_CREATE + "')")
    @GetMapping("/mine")
    @Operation(summary = "Get my repair orders",
               description = "Returns only the repair orders created by the authenticated user, with optional filters.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved repair order list")
    public ResponseEntity<Page<RepairOrderResponseDTO>> getMyRepairOrders(
            @Parameter(description = "Filter orders from this date (inclusive)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter orders to this date (inclusive)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by vehicle ID") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Filter by vehicle license plate") @RequestParam(required = false) String vehicleLicensePlate,
            @Parameter(description = "Filter by order status") @RequestParam(required = false) RepairOrderStatus status,
            @Parameter(description = "Search in description or reported-by") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        RepairOrderFilterDTO filterDTO = new RepairOrderFilterDTO(dateFrom, dateTo, vehicleId, vehicleLicensePlate, status, search);
        return ResponseEntity.ok(repairOrderService.getMyRepairOrders(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_READ + "')")
    @GetMapping
    @Operation(summary = "Get all repair orders",
               description = "Returns all repair orders in the tenant with optional filters. Requires workshop staff permission.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved repair order list")
    public ResponseEntity<Page<RepairOrderResponseDTO>> getAllRepairOrders(
            @Parameter(description = "Filter orders from this date (inclusive)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter orders to this date (inclusive)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by vehicle ID") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Filter by vehicle license plate") @RequestParam(required = false) String vehicleLicensePlate,
            @Parameter(description = "Filter by order status") @RequestParam(required = false) RepairOrderStatus status,
            @Parameter(description = "Search in description or reported-by") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        RepairOrderFilterDTO filterDTO = new RepairOrderFilterDTO(dateFrom, dateTo, vehicleId, vehicleLicensePlate, status, search);
        return ResponseEntity.ok(repairOrderService.getAllRepairOrders(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_CREATE + "') or hasAuthority('" + AppPermissions.REPAIR_ORDER_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get repair order by ID",
               description = "Returns a repair order by ID. Users with only REPAIR_ORDER_CREATE can only access their own orders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repair order found"),
            @ApiResponse(responseCode = "400", description = "Access denied — not the owner"),
            @ApiResponse(responseCode = "404", description = "Repair order not found")
    })
    public ResponseEntity<RepairOrderResponseDTO> getRepairOrderById(
            @Parameter(description = "Repair order unique identifier") @PathVariable Long id) {
        return ResponseEntity.ok(repairOrderService.getRepairOrderById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_CREATE + "') or hasAuthority('" + AppPermissions.REPAIR_ORDER_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update a repair order",
               description = "Updates description or date of a repair order. Only allowed when status is PENDIENTE or EN_PROCESO. Field operators can only edit their own orders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repair order successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data, not owner, or status is not PENDIENTE"),
            @ApiResponse(responseCode = "404", description = "Repair order not found")
    })
    public ResponseEntity<RepairOrderResponseDTO> updateRepairOrder(
            @Parameter(description = "Repair order unique identifier") @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody RepairOrderRequestDTO dto) {
        return ResponseEntity.ok(repairOrderService.updateRepairOrder(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_CREATE + "') or hasAuthority('" + AppPermissions.REPAIR_ORDER_WRITE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a repair order",
               description = "Soft-deletes a repair order. Only allowed when status is PENDIENTE. Field operators can only delete their own orders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Repair order successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Not owner or status is not PENDIENTE"),
            @ApiResponse(responseCode = "404", description = "Repair order not found")
    })
    public ResponseEntity<Void> deleteRepairOrder(
            @Parameter(description = "Repair order unique identifier") @PathVariable Long id) {
        repairOrderService.deleteRepairOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_WRITE + "')")
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change repair order status",
               description = "Transitions a repair order from PENDIENTE to EN_PROCESO. No other transitions are allowed via this endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Repair order not found")
    })
    public ResponseEntity<RepairOrderResponseDTO> changeStatus(
            @Parameter(description = "Repair order unique identifier") @PathVariable Long id,
            @Validated @RequestBody RepairOrderStatusDTO statusDTO) {
        return ResponseEntity.ok(repairOrderService.changeStatus(id, statusDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_WRITE + "')")
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a repair order",
               description = "Closes the repair order by creating a Repair record with the provided details. Order must be in EN_PROCESO status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repair order completed and Repair record created"),
            @ApiResponse(responseCode = "400", description = "Order is not in EN_PROCESO status"),
            @ApiResponse(responseCode = "404", description = "Repair order not found")
    })
    public ResponseEntity<RepairOrderResponseDTO> completeRepairOrder(
            @Parameter(description = "Repair order unique identifier") @PathVariable Long id,
            @Validated @RequestBody RepairOrderCompleteDTO completeDTO) {
        return ResponseEntity.ok(repairOrderService.completeRepairOrder(id, completeDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPAIR_ORDER_CREATE + "') or hasAuthority('" + AppPermissions.REPAIR_ORDER_READ + "')")
    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate repair order PDF",
               description = "Generates a printable A4 PDF of the repair order (large type, checklist, signature area) for the workshop.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generated successfully"),
            @ApiResponse(responseCode = "404", description = "Repair order not found")
    })
    public ResponseEntity<byte[]> generateRepairOrderPdf(
            @Parameter(description = "Repair order unique identifier") @PathVariable Long id) {
        byte[] pdf = repairOrderService.generateRepairOrderPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "orden-de-reparacion-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        String mappedSortBy = mapSortField(sortBy);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        return PageRequest.of(page, size, sort);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "vehicleLicensePlate" -> "vehicle.licensePlate";
            case "vehicleId" -> "vehicle.id";
            case "createdByUserName" -> "createdByUser.firstName";
            default -> sortBy;
        };
    }
}
