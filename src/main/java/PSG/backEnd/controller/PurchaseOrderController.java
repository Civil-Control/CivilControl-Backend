package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.purchaseOrder.*;
import PSG.backEnd.model.enums.PurchaseOrderCategory;
import PSG.backEnd.model.enums.PurchaseOrderPriority;
import PSG.backEnd.model.enums.PurchaseOrderStatus;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IPurchaseOrderService;
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

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "API for managing purchase orders. Operators request items and supervisors manage the approval workflow.")
public class PurchaseOrderController {

    private final IPurchaseOrderService purchaseOrderService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("date", "status", "priority", "category", "requestedBy");

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_CREATE + "')")
    @PostMapping
    @Operation(summary = "Create a new purchase order",
               description = "Creates a new purchase order with status PENDIENTE. Automatically linked to the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Purchase order successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<PurchaseOrderResponseDTO> createPurchaseOrder(
            @Validated(OnCreate.class) @RequestBody PurchaseOrderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.createPurchaseOrder(dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_CREATE + "')")
    @GetMapping("/mine")
    @Operation(summary = "Get my purchase orders",
               description = "Returns only the purchase orders created by the authenticated user. Excludes COMPRADA status.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved purchase order list")
    public ResponseEntity<Page<PurchaseOrderResponseDTO>> getMyPurchaseOrders(
            @Parameter(description = "Filter by status") @RequestParam(required = false) PurchaseOrderStatus status,
            @Parameter(description = "Filter by category") @RequestParam(required = false) PurchaseOrderCategory category,
            @Parameter(description = "Search in description or requestedBy") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        PurchaseOrderFilterDTO filterDTO = new PurchaseOrderFilterDTO(null, null, status, category, null, null, search);
        return ResponseEntity.ok(purchaseOrderService.getMyPurchaseOrders(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_READ + "')")
    @GetMapping
    @Operation(summary = "Get all purchase orders",
               description = "Returns all purchase orders in the tenant with optional filters. Requires supervisor permission.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved purchase order list")
    public ResponseEntity<Page<PurchaseOrderResponseDTO>> getAllPurchaseOrders(
            @Parameter(description = "Filter orders from this date (inclusive)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter orders to this date (inclusive)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by status") @RequestParam(required = false) PurchaseOrderStatus status,
            @Parameter(description = "Filter by category") @RequestParam(required = false) PurchaseOrderCategory category,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) PurchaseOrderPriority priority,
            @Parameter(description = "Filter by linked transactional document ID") @RequestParam(required = false) Long transactionalDocumentId,
            @Parameter(description = "Search in description or requestedBy") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        PurchaseOrderFilterDTO filterDTO = new PurchaseOrderFilterDTO(dateFrom, dateTo, status, category, priority, transactionalDocumentId, search);
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_CREATE + "') or hasAuthority('" + AppPermissions.PURCHASE_ORDER_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID",
               description = "Returns a purchase order by ID. Users with only PURCHASE_ORDER_CREATE can only access their own orders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Purchase order found"),
            @ApiResponse(responseCode = "400", description = "Access denied — not the owner"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public ResponseEntity<PurchaseOrderResponseDTO> getPurchaseOrderById(
            @Parameter(description = "Purchase order unique identifier") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_CREATE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update a purchase order",
               description = "Updates fields of a purchase order. Only allowed when status is PENDIENTE. Only the creator can edit.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Purchase order successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data, not owner, or status is not PENDIENTE"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public ResponseEntity<PurchaseOrderResponseDTO> updatePurchaseOrder(
            @Parameter(description = "Purchase order unique identifier") @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody PurchaseOrderRequestDTO dto) {
        return ResponseEntity.ok(purchaseOrderService.updatePurchaseOrder(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_CREATE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a purchase order",
               description = "Soft-deletes a purchase order. Only allowed when status is PENDIENTE. Only the creator can delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Purchase order successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Not owner or status is not PENDIENTE"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public ResponseEntity<Void> deletePurchaseOrder(
            @Parameter(description = "Purchase order unique identifier") @PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_WRITE + "')")
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change purchase order status",
               description = "Advances or rolls back the order status according to valid transitions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public ResponseEntity<PurchaseOrderResponseDTO> changeStatus(
            @Parameter(description = "Purchase order unique identifier") @PathVariable Long id,
            @Validated @RequestBody PurchaseOrderStatusDTO statusDTO) {
        return ResponseEntity.ok(purchaseOrderService.changeStatus(id, statusDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PURCHASE_ORDER_WRITE + "')")
    @PatchMapping("/{id}/link-document")
    @Operation(summary = "Link a transactional document to a purchase order",
               description = "Links a fiscal document to the purchase order. Only allowed when status is COMPRADA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document successfully linked"),
            @ApiResponse(responseCode = "400", description = "Order is not COMPRADA or document not found"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public ResponseEntity<PurchaseOrderResponseDTO> linkTransactionalDocument(
            @Parameter(description = "Purchase order unique identifier") @PathVariable Long id,
            @Validated @RequestBody PurchaseOrderLinkDocumentDTO dto) {
        return ResponseEntity.ok(purchaseOrderService.linkTransactionalDocument(id, dto));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        String mappedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "date";
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        return PageRequest.of(page, size, sort);
    }
}
