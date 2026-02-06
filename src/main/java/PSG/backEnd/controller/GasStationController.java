package PSG.backEnd.controller;

import PSG.backEnd.model.dto.gasStation.GasStationDTO;
import PSG.backEnd.model.dto.gasStation.GasStationFilterDTO;
import PSG.backEnd.model.dto.gasStation.GasStationResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IGasStationService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/gas-stations")
@RequiredArgsConstructor
@Tag(name = "Gas Stations", description = "API for managing gas stations and their fuel prices. Handles registration of fuel suppliers and their pricing information.")
public class GasStationController {

    private final IGasStationService gasStationService;

    @PostMapping
    @Operation(summary = "Create a new gas station",
            description = "Registers a new gas station with fuel prices. Each gas station must belong to a supplier and have at least one fuel price defined.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Gas station successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Supplier not found")
    })
    public ResponseEntity<GasStationResponseDTO> createGasStation(
            @Validated(OnCreate.class) @RequestBody GasStationDTO gasStationDTO) {
        GasStationResponseDTO createdGasStation = gasStationService.createGasStation(gasStationDTO);
        return new ResponseEntity<>(createdGasStation, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all gas stations with filters",
            description = "Retrieves a paginated list of gas stations with optional filtering by supplier and available fuel types. Supports sorting and pagination.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved gas station list")
    public ResponseEntity<Page<GasStationResponseDTO>> getGasStations(
            @Parameter(description = "Filter by supplier ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by available fuel types") @RequestParam(required = false) List<String> fuelTypes,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Available fields: id, supplierId, supplierLegalName, supplierTradeName, supplierCuit, supplierDefaultDiscountPercentage",
                    example = "supplierLegalName")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        GasStationFilterDTO filterDTO = new GasStationFilterDTO(supplierId, fuelTypes);

        return ResponseEntity.ok(gasStationService.getAllGasStations(filterDTO, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "supplierLegalName" -> "supplier.legalName";
            case "supplierTradeName" -> "supplier.tradeName";
            case "supplierCuit" -> "supplier.cuit";
            case "supplierDefaultDiscountPercentage" -> "supplier.defaultDiscountPercentage";
            case "supplierId" -> "supplier.id";
            // Keep backward compatibility with old field names
            case "legalName" -> "supplier.legalName";
            case "tradeName" -> "supplier.tradeName";
            case "cuit" -> "supplier.cuit";
            case "defaultDiscountPercentage" -> "supplier.defaultDiscountPercentage";
            default -> sortBy; // For 'id' and any other fields
        };
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get gas station by ID",
            description = "Retrieves detailed information about a specific gas station by its unique identifier, including all fuel prices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gas station found"),
            @ApiResponse(responseCode = "404", description = "Gas station not found")
    })
    public ResponseEntity<GasStationResponseDTO> getGasStationById(
            @Parameter(description = "Gas station unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(gasStationService.getGasStationById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update gas station",
            description = "Updates an existing gas station record. Only provided fields will be updated. Can update supplier assignment and fuel prices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gas station successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Gas station or supplier not found")
    })
    public ResponseEntity<GasStationResponseDTO> updateGasStation(
            @Parameter(description = "Gas station unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody GasStationDTO gasStationDTO) {
        return ResponseEntity.ok(gasStationService.updateGasStation(id, gasStationDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete gas station",
            description = "Performs a soft deletion of a gas station. The record is marked as deleted but remains in the database. " +
                    "Gas stations with associated fuel load records cannot be deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Gas station successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Gas station not found"),
            @ApiResponse(responseCode = "409", description = "Gas station has associated fuel loads and cannot be deleted")
    })
    public ResponseEntity<Void> deleteGasStation(
            @Parameter(description = "Gas station unique identifier", required = true) @PathVariable Long id) {
        gasStationService.deleteGasStation(id);
        return ResponseEntity.noContent().build();
    }
}
