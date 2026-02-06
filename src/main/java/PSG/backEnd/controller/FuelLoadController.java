package PSG.backEnd.controller;

import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadFilterDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadResponseDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IFuelLoadService;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/fuel-loads")
@RequiredArgsConstructor
@Tag(name = "Fuel Loads", description = "API for managing fuel load transactions. Handles recording of fuel purchases made at gas stations for vehicles, including individual and batch operations.")
public class FuelLoadController {

    private final IFuelLoadService fuelLoadService;

    @PostMapping
    @Operation(summary = "Create a new fuel load",
            description = "Records a new fuel load transaction for a vehicle. Automatically calculates the total amount based on the gas station's current fuel price.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Fuel load successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Vehicle, gas station, or project area not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate ticket number for the same branch")
    })
    public ResponseEntity<FuelLoadResponseDTO> createFuelLoad(@Validated(OnCreate.class) @RequestBody FuelLoadDTO fuelLoadDTO) {
        FuelLoadResponseDTO createdFuelLoad = fuelLoadService.createFuelLoad(fuelLoadDTO);
        return new ResponseEntity<>(createdFuelLoad, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple fuel loads in batch",
            description = "Records multiple fuel load transactions at once. Returns a summary with successful and failed operations. Continues processing even if some records fail validation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Batch processing completed with at least one successful fuel load"),
            @ApiResponse(responseCode = "400", description = "All fuel loads in batch failed validation")
    })
    public ResponseEntity<FuelLoadBatchResponseDTO> createFuelLoadBatch(@Validated @RequestBody FuelLoadBatchDTO fuelLoadBatchDTO) {
        FuelLoadBatchResponseDTO result = fuelLoadService.createFuelLoadBatch(fuelLoadBatchDTO);
        HttpStatus status = result.totalSuccessful() > 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(result, status);
    }

    @GetMapping
    @Operation(summary = "Get all fuel loads with filters",
            description = "Retrieves a paginated list of fuel load transactions with optional filtering by date range, branch code, ticket number, fuel type, vehicle, project area, and gas station. Supports sorting and pagination.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved fuel load list")
    public ResponseEntity<Page<FuelLoadResponseDTO>> getFuelLoads(
            @Parameter(description = "Filter fuel loads from this date (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Filter fuel loads to this date (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Filter by branch code (partial match)") @RequestParam(required = false) String branchCode,
            @Parameter(description = "Filter by ticket number (partial match)") @RequestParam(required = false) String ticketNumber,
            @Parameter(description = "Filter by fuel type") @RequestParam(required = false) String fuelType,
            @Parameter(description = "Filter by vehicle ID") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Filter by vehicle license plate (partial match)") @RequestParam(required = false) String vehicleLicensePlate,
            @Parameter(description = "Filter by project area ID") @RequestParam(required = false) Long projectAreaId,
            @Parameter(description = "Filter by project area name (partial match)") @RequestParam(required = false) String projectAreaName,
            @Parameter(description = "Filter by gas station ID") @RequestParam(required = false) Long gasStationId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, date, branchCode, ticketNumber, fuelType, liters, pricePerLiter, totalAmount. " +
                    "For vehicle use: vehicleLicensePlate, vehicleBrand, vehicleModel, vehicleId. " +
                    "For project area use: projectAreaName, projectAreaId. " +
                    "For gas station use: gasStationId. " +
                    "Example: sortBy=vehicleLicensePlate",
                    example = "date")
            @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        FuelLoadFilterDTO filterDTO = new FuelLoadFilterDTO(dateFrom, dateTo, branchCode, ticketNumber, fuelType, vehicleId, vehicleLicensePlate, projectAreaId, projectAreaName, gasStationId);
        return ResponseEntity.ok(fuelLoadService.getAllFuelLoads(filterDTO, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "vehicleLicensePlate" -> "vehicle.licensePlate";
            case "vehicleBrand" -> "vehicle.brand";
            case "vehicleModel" -> "vehicle.model";
            case "vehicleId" -> "vehicle.id";
            case "projectAreaName" -> "projectArea.name";
            case "projectAreaId" -> "projectArea.id";
            case "gasStationId" -> "gasStation.id";
            default -> sortBy; // For 'id', 'date', 'branchCode', 'ticketNumber', 'fuelType', 'liters', 'pricePerLiter', 'totalAmount', etc.
        };
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fuel load by ID",
            description = "Retrieves detailed information about a specific fuel load transaction by its unique identifier, including calculated amounts and related entity details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fuel load found"),
            @ApiResponse(responseCode = "404", description = "Fuel load not found")
    })
    public ResponseEntity<FuelLoadResponseDTO> getFuelLoadById(@Parameter(description = "Fuel load unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(fuelLoadService.getFuelLoadById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update fuel load",
            description = "Updates an existing fuel load transaction. Only provided fields will be updated. Total amount is recalculated if liters or gas station changes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fuel load successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Fuel load, vehicle, gas station, or project area not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate ticket number for the same branch")
    })
    public ResponseEntity<FuelLoadResponseDTO> updateFuelLoad(@Parameter(description = "Fuel load unique identifier", required = true) @PathVariable Long id, @Validated(OnUpdate.class) @RequestBody FuelLoadDTO fuelLoadDTO) {
        return ResponseEntity.ok(fuelLoadService.updateFuelLoad(id, fuelLoadDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete fuel load",
            description = "Deletes a fuel load transaction from the system. This operation cannot be undone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fuel load successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Fuel load not found")
    })
    public ResponseEntity<Void> deleteFuelLoad(@Parameter(description = "Fuel load unique identifier", required = true) @PathVariable Long id) {
        fuelLoadService.deleteFuelLoad(id);
        return ResponseEntity.noContent().build();
    }
}

