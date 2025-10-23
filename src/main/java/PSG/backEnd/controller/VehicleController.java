package PSG.backEnd.controller;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.dto.vehicle.VehicleFilterDTO;
import PSG.backEnd.model.dto.vehicle.VehicleResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IVehicleService;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "API for managing vehicles in the fleet. Handles vehicle registration, specifications, assignments, and tracking of administrative details like VTV inspections.")
public class VehicleController {

    private final IVehicleService iVehicleService;

    @PostMapping
    @Operation(summary = "Create a new vehicle",
            description = "Registers a new vehicle in the system. Includes vehicle identification (license plate), specifications (brand, model, year), " +
                    "and administrative details (project area assignment, storage location, VTV expiration).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Project area not found"),
            @ApiResponse(responseCode = "409", description = "Vehicle with this license plate already exists")
    })
    public ResponseEntity<VehicleResponseDTO> createVehicle(
            @Validated(OnCreate.class) @RequestBody VehicleDTO vehicleDTO) {
        VehicleResponseDTO createdVehicle = iVehicleService.createVehicle(vehicleDTO);
        return new ResponseEntity<>(createdVehicle, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all vehicles with filters",
            description = "Retrieves a paginated list of vehicles with optional filtering by license plate, brand, model, year, color, " +
                    "nickname, vehicle type, project area, storage location, VTV expiration date, jurisdiction, and truck equipment. Supports sorting and pagination.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved vehicle list")
    public ResponseEntity<Page<VehicleResponseDTO>> getVehicles(
            @Parameter(description = "Filter by license plate (partial match)") @RequestParam(required = false) String licensePlate,
            @Parameter(description = "Filter by vehicle brand (partial match)") @RequestParam(required = false) String brand,
            @Parameter(description = "Filter by vehicle model (partial match)") @RequestParam(required = false) String model,
            @Parameter(description = "Filter by manufacturing year") @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by vehicle color (partial match)") @RequestParam(required = false) String color,
            @Parameter(description = "Filter by vehicle nickname (partial match)") @RequestParam(required = false) String nickName,
            @Parameter(description = "Filter by vehicle type") @RequestParam(required = false) String vehicleType,
            @Parameter(description = "Filter by project area name (partial match)") @RequestParam(required = false) String projectAreaName,
            @Parameter(description = "Filter by storage location (partial match)") @RequestParam(required = false) String storedIn,
            @Parameter(description = "Filter by VTV expiration date") @RequestParam(required = false) LocalDate vtvExpirationDate,
            @Parameter(description = "Filter by jurisdiction type") @RequestParam(required = false) String jurisdictionType,
            @Parameter(description = "Filter by truck equipment type (only for CAMION vehicles)") @RequestParam(required = false) String truckEquipment,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        VehicleFilterDTO filterDTO = new VehicleFilterDTO(
                licensePlate, brand, model, year, color, nickName,
                vehicleType, projectAreaName, storedIn, vtvExpirationDate, jurisdictionType, truckEquipment
        );

        return ResponseEntity.ok(iVehicleService.getAllVehicles(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID",
            description = "Retrieves detailed information about a specific vehicle by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle found"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponseDTO> getVehicleById(
            @Parameter(description = "Vehicle unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iVehicleService.getVehicleById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update vehicle",
            description = "Updates an existing vehicle record. Only provided fields will be updated. Allows updating vehicle specifications, " +
                    "project area assignment, storage location, and administrative details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Vehicle or project area not found"),
            @ApiResponse(responseCode = "409", description = "License plate already exists for another vehicle")
    })
    public ResponseEntity<VehicleResponseDTO> updateVehicle(
            @Parameter(description = "Vehicle unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody VehicleDTO vehicleDTO) {
        return ResponseEntity.ok(iVehicleService.updateVehicle(id, vehicleDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vehicle",
            description = "Deletes a vehicle record from the system. This operation cannot be undone. " +
                    "Note: Vehicles with associated records (repairs, fuel loads, insurance policies, etc.) cannot be deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vehicle successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "409", description = "Vehicle has associated records and cannot be deleted")
    })
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "Vehicle unique identifier", required = true) @PathVariable Long id) {
        iVehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}

