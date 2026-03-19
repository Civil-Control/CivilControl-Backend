package PSG.backEnd.controller;

import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemFilterDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IItemService;
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

import java.net.URI;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Tag(name = "Item Catalog", description = "API for managing purchasable items catalog. " +
        "Items represent goods or services that can be purchased from suppliers and included in transactional documents such as invoices. " +
        "Examples include construction materials, equipment, supplies, and services.")
public class ItemController {

    private final IItemService itemService;

    @PostMapping
    @Operation(summary = "Create a new item",
            description = "Creates a new item in the catalog. Items represent purchasable goods or services that can be referenced in transactional documents. " +
                    "Includes validation for unique item names and automatic handling of previously deleted items.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item successfully created. Returns the created item with its assigned ID."),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (missing required fields, invalid format, etc.)"),
            @ApiResponse(responseCode = "409", description = "Item already exists with the same name")
    })
    public ResponseEntity<ItemResponseDTO> createItem(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Item data including name and optional description",
                    required = true
            )
            @Validated(OnCreate.class) @RequestBody ItemDTO dto) {
        ItemResponseDTO created = itemService.create(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update item",
            description = "Updates an existing item in the catalog. Only provided fields will be updated. " +
                    "Can update the item name and description. Validates that the item exists and isn't marked as deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Item not found or has been deleted"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate item name")
    })
    public ResponseEntity<ItemResponseDTO> updateItem(
            @Parameter(description = "Item unique identifier", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Item data to update. Only include fields you want to modify.",
                    required = true
            )
            @Validated(OnUpdate.class) @RequestBody ItemDTO dto) {
        ItemResponseDTO updated = itemService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete item",
            description = "Performs a soft delete of an item from the catalog. The item is marked as deleted but remains in the database for historical purposes. " +
                    "Cannot delete items that are referenced in existing transactional documents.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete item with references in transactional documents"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "Item unique identifier", required = true, example = "1") @PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID",
            description = "Retrieves detailed information about a specific item by its unique identifier, including its name and description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item found"),
            @ApiResponse(responseCode = "404", description = "Item not found or has been deleted")
    })
    public ResponseEntity<ItemResponseDTO> getItemById(
            @Parameter(description = "Item unique identifier", required = true, example = "1") @PathVariable Long id) {
        ItemResponseDTO item = itemService.getById(id);
        return ResponseEntity.ok(item);
    }

    @GetMapping
    @Operation(summary = "Get all items with filters",
            description = "Retrieves a paginated list of items with optional filtering by name and description (partial match). " +
                    "Supports sorting by any field. Useful for searching and browsing the items catalog.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved items list")
    public ResponseEntity<Page<ItemResponseDTO>> getItems(
            @Parameter(description = "Filter by item name (partial match, case-insensitive)", example = "Cemento") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by item description (partial match, case-insensitive)", example = "Portland") @RequestParam(required = false) String description,
            @Parameter(description = "Generic search across name and description (partial match)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., name, description, id)", example = "name") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)", example = "asc") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        ItemFilterDTO filterDTO = new ItemFilterDTO(name, description, search);

        return ResponseEntity.ok(itemService.list(filterDTO, pageable));
    }
}
