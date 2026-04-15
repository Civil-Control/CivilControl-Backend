package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.batch.BatchResponseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseBatchDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseFilterDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IStockPurchaseService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-purchases")
@RequiredArgsConstructor
@Tag(name = "Stock Purchases", description = "API for managing stock purchase records. When a purchase is registered, the corresponding stock quantity is automatically increased.")
public class StockPurchaseController {

    private final IStockPurchaseService stockPurchaseService;

    @PreAuthorize("hasAuthority('" + AppPermissions.STOCK_PURCHASE_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new stock purchase",
            description = "Creates a new stock purchase record and automatically increases the corresponding stock quantity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stock purchase successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Stock item not found")
    })
    public ResponseEntity<StockPurchaseResponseDTO> createStockPurchase(
            @Validated(OnCreate.class) @RequestBody StockPurchaseDTO dto) {
        return new ResponseEntity<>(stockPurchaseService.createStockPurchase(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.STOCK_PURCHASE_WRITE + "')")
    @PostMapping("/batch")
    @Operation(summary = "Create multiple stock purchases in batch",
            description = "Creates multiple stock purchase records in a single request. Each purchase increases the corresponding stock quantity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stock purchases successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Stock item not found")
    })
    public ResponseEntity<BatchResponseDTO<StockPurchaseResponseDTO>> createBatchStockPurchases(
            @Validated @RequestBody StockPurchaseBatchDTO batchDTO) {
        BatchResponseDTO<StockPurchaseResponseDTO> result = stockPurchaseService.createBatchStockPurchases(batchDTO);
        HttpStatus status = result.totalSuccessful() > 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(result, status);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.STOCK_PURCHASE_READ + "')")
    @GetMapping
    @Operation(summary = "Get all stock purchases with filters",
            description = "Retrieves a paginated list of stock purchases with optional filtering.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved stock purchase list")
    public ResponseEntity<Page<StockPurchaseResponseDTO>> getStockPurchases(
            @Parameter(description = "Filter purchases from this date (inclusive)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter purchases to this date (inclusive)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by stock item ID") @RequestParam(required = false) Long stockId,
            @Parameter(description = "Filter by stock item name") @RequestParam(required = false) String stockName,
            @Parameter(description = "Filter by stock category") @RequestParam(required = false) String stockCategory,
            @Parameter(description = "Minimum quantity") @RequestParam(required = false) BigDecimal minQuantity,
            @Parameter(description = "Maximum quantity") @RequestParam(required = false) BigDecimal maxQuantity,
            @Parameter(description = "Minimum total amount") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum total amount") @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Filter by linked transactional document ID") @RequestParam(required = false) Long transactionalDocumentId,
            @Parameter(description = "Search by stock name") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Options: date, quantity, totalAmount, unitPrice, stockName", example = "date")
            @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        String mappedSortBy = mapSortField(sortBy);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        StockPurchaseFilterDTO filterDTO = new StockPurchaseFilterDTO(
                dateFrom, dateTo, stockId, stockName, stockCategory,
                minQuantity, maxQuantity, minAmount, maxAmount, transactionalDocumentId, search
        );

        return ResponseEntity.ok(stockPurchaseService.getAllStockPurchases(filterDTO, pageable));
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "stockName" -> "stock.name";
            case "stockCategory" -> "stock.stockCategory";
            default -> sortBy;
        };
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.STOCK_PURCHASE_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get stock purchase by ID",
            description = "Retrieves detailed information about a specific stock purchase by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock purchase found"),
            @ApiResponse(responseCode = "404", description = "Stock purchase not found")
    })
    public ResponseEntity<StockPurchaseResponseDTO> getStockPurchaseById(
            @Parameter(description = "Stock purchase unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(stockPurchaseService.getStockPurchaseById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.STOCK_PURCHASE_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update stock purchase",
            description = "Updates an existing stock purchase record. Only provided fields will be updated. If quantity changes, the stock quantity is adjusted accordingly.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock purchase successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Stock purchase not found")
    })
    public ResponseEntity<StockPurchaseResponseDTO> updateStockPurchase(
            @Parameter(description = "Stock purchase unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody StockPurchaseDTO dto) {
        return ResponseEntity.ok(stockPurchaseService.updateStockPurchase(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.STOCK_PURCHASE_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete stock purchase",
            description = "Deletes a stock purchase record. The previously added stock quantity is reversed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Stock purchase successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Stock purchase not found")
    })
    public ResponseEntity<Void> deleteStockPurchase(
            @Parameter(description = "Stock purchase unique identifier", required = true) @PathVariable Long id) {
        stockPurchaseService.deleteStockPurchase(id);
        return ResponseEntity.noContent().build();
    }
}
