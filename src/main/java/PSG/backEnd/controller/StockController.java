package PSG.backEnd.controller;

import PSG.backEnd.model.dto.stock.StockDTO;
import PSG.backEnd.model.dto.stock.StockFilterDTO;
import PSG.backEnd.model.dto.stock.StockResponseDTO;
import PSG.backEnd.model.enums.StockCategory;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IStockService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock Management", description = "API for managing inventory stock items. Handles tools, equipment, supplies, and materials used in construction projects.")
public class StockController {

    private final IStockService stockService;

    @PostMapping
    @Operation(summary = "Create a new stock item",
            description = "Creates a new stock item in the inventory system. Includes validation for unique items and automatic handling of previously deleted items with the same characteristics.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stock item successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "409", description = "Stock item already exists with the same name and category")
    })
    public ResponseEntity<StockResponseDTO> createStock(
            @Validated(OnCreate.class) @RequestBody StockDTO dto) {
        StockResponseDTO created = stockService.createStock(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update stock item",
            description = "Updates an existing stock item in the inventory. Only provided fields will be updated. Validates that the item exists and isn't marked as deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock item successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Stock item not found"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate item")
    })
    public ResponseEntity<StockResponseDTO> updateStock(
            @Parameter(description = "Stock item unique identifier", required = true, example = "1") @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody StockDTO dto) {
        StockResponseDTO updated = stockService.updateStock(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete stock item",
            description = "Performs a soft delete of a stock item from the inventory. The item is marked as deleted but remains in the database for historical purposes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Stock item successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Stock item not found")
    })
    public ResponseEntity<Void> deleteStock(
            @Parameter(description = "Stock item unique identifier", required = true, example = "1") @PathVariable Long id) {
        stockService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock item by ID",
            description = "Retrieves detailed information about a specific stock item by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock item found"),
            @ApiResponse(responseCode = "404", description = "Stock item not found")
    })
    public ResponseEntity<StockResponseDTO> getStockById(
            @Parameter(description = "Stock item unique identifier", required = true, example = "1") @PathVariable Long id) {
        StockResponseDTO stock = stockService.getStockById(id);
        return ResponseEntity.ok(stock);
    }

    @GetMapping
    @Operation(summary = "Get all stock items with filters",
            description = "Retrieves a paginated list of stock items with optional filtering by name, building, category, and quantity range. Supports sorting by any field.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved stock item list")
    public ResponseEntity<Page<StockResponseDTO>> getStocks(
            @Parameter(description = "Filter by item name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by building ID where stock is stored") @RequestParam(required = false) Long buildingId,
            @Parameter(description = "Filter by stock category") @RequestParam(required = false) StockCategory stockCategory,
            @Parameter(description = "Minimum quantity threshold") @RequestParam(required = false) BigDecimal minQuantity,
            @Parameter(description = "Maximum quantity threshold") @RequestParam(required = false) BigDecimal maxQuantity,
            @Parameter(description = "Generic search across item name and category (partial match)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., name, quantity, stockCategory)") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        StockFilterDTO filterDTO = new StockFilterDTO(name, buildingId, stockCategory, minQuantity, maxQuantity, search);

        return ResponseEntity.ok(stockService.getAllStocks(filterDTO, pageable));
    }
}
