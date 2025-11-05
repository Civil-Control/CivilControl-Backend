package PSG.backEnd.controller;

import PSG.backEnd.model.dto.building.BuildingDTO;
import PSG.backEnd.model.dto.building.BuildingFilterDTO;
import PSG.backEnd.model.dto.building.BuildingResponseDTO;
import PSG.backEnd.model.enums.BuildingType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IBuildingService;
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
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
@Tag(name = "Building Management", description = "API for managing buildings and facilities. Handles plants, warehouses, offices, branches, and other physical locations used in construction projects.")
public class BuildingController {

    private final IBuildingService buildingService;

    @PostMapping
    @Operation(summary = "Create a new building",
            description = "Creates a new building in the system. Includes validation for unique building codes and automatic handling of previously deleted buildings with the same code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Building successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "409", description = "Building already exists with the same code")
    })
    public ResponseEntity<BuildingResponseDTO> createBuilding(
            @Validated(OnCreate.class) @RequestBody BuildingDTO dto) {
        BuildingResponseDTO created = buildingService.createBuilding(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update building",
            description = "Updates an existing building in the system. Only provided fields will be updated. Validates that the building exists and isn't marked as deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Building successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Building not found"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate building code")
    })
    public ResponseEntity<BuildingResponseDTO> updateBuilding(
            @Parameter(description = "Building unique identifier", required = true, example = "1") @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody BuildingDTO dto) {
        BuildingResponseDTO updated = buildingService.updateBuilding(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete building",
            description = "Performs a soft delete of a building from the system. The building is marked as deleted but remains in the database for historical purposes. Cannot delete buildings with active stock items.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Building successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete building with active stock items"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    public ResponseEntity<Void> deleteBuilding(
            @Parameter(description = "Building unique identifier", required = true, example = "1") @PathVariable Long id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get building by ID",
            description = "Retrieves detailed information about a specific building by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Building found"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    public ResponseEntity<BuildingResponseDTO> getBuildingById(
            @Parameter(description = "Building unique identifier", required = true, example = "1") @PathVariable Long id) {
        BuildingResponseDTO building = buildingService.getBuildingById(id);
        return ResponseEntity.ok(building);
    }

    @GetMapping
    @Operation(summary = "Get all buildings with filters",
            description = "Retrieves a paginated list of buildings with optional filtering by name, code, type, and active status. Supports sorting by any field.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved building list")
    public ResponseEntity<Page<BuildingResponseDTO>> getBuildings(
            @Parameter(description = "Filter by building name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by building code (partial match)") @RequestParam(required = false) String code,
            @Parameter(description = "Filter by building type") @RequestParam(required = false) BuildingType buildingType,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., name, code, buildingType, active)") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        BuildingFilterDTO filterDTO = new BuildingFilterDTO(name, code, buildingType, active);

        return ResponseEntity.ok(buildingService.getAllBuildings(filterDTO, pageable));
    }
}

