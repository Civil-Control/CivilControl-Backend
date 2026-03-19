package PSG.backEnd.controller;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.dto.vehicle.VehicleFilterDTO;
import PSG.backEnd.model.dto.vehicle.VehicleResponseDTO;
import PSG.backEnd.model.dto.vehicle.VehicleTypeDTO;
import PSG.backEnd.model.dto.vehicle.VehicleTypeResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IVehicleService;
import PSG.backEnd.service.port.IVehicleTypeService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "API for managing vehicles in the fleet. Handles vehicle registration, specifications, assignments, and tracking of administrative details like VTV inspections.")
public class VehicleController {

    private final IVehicleService iVehicleService;
    private final IVehicleTypeService iVehicleTypeService;

    // ============================================================
    // VEHICLE ENDPOINTS
    // ============================================================

    @PostMapping
    @Operation(summary = "Create a new vehicle",
            description = "Registers a new vehicle in the system. Includes vehicle identification (license plate), specifications (brand, model, year), " +
                    "vehicle type (referenced by vehicleTypeId), and administrative details (project area assignment, storage location, VTV expiration). " +
                    "IMPORTANT: If the selected vehicle type has requiresTruckEquipment=true, the truckEquipment field is REQUIRED (NADA, HIDROELEVADOR, or HIDROGRUA). " +
                    "For vehicle types with requiresTruckEquipment=false, truckEquipment must be null or empty.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (e.g., truckEquipment missing for types that require it)"),
            @ApiResponse(responseCode = "404", description = "Project area or vehicle type not found"),
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
            @Parameter(description = "Include deactivated vehicles in results (default: false)") @RequestParam(defaultValue = "false") boolean includeInactive,
            @Parameter(description = "Generic search across license plate, brand, model and nickname (partial match)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, licensePlate, brand, model, year, color, nickName, vehicleType, " +
                    "storedIn, vtvExpirationDate, jurisdictionType, truckEquipment. " +
                    "For project area use: projectArea.name. " +
                    "Example: sortBy=projectArea.name",
                    example = "licensePlate")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        VehicleFilterDTO filterDTO = new VehicleFilterDTO(
                licensePlate, brand, model, year, color, nickName,
                vehicleType, projectAreaName, storedIn, vtvExpirationDate, jurisdictionType, truckEquipment,
                includeInactive, search
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
                    "project area assignment, storage location, and administrative details. " +
                    "IMPORTANT: truckEquipment validation applies based on the vehicle type's requiresTruckEquipment flag. " +
                    "If vehicleTypeId is changed to a type with requiresTruckEquipment=true, truckEquipment becomes required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., truckEquipment validation failed for the vehicle type)"),
            @ApiResponse(responseCode = "404", description = "Vehicle, project area or vehicle type not found"),
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

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Set vehicle as active (in service)",
            description = "Marks an inactive vehicle as active/in service. This is an operational status change, not a soft-delete restore.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle successfully activated"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponseDTO> activateVehicle(
            @Parameter(description = "Vehicle unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iVehicleService.activateVehicle(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Set vehicle as inactive (out of service)",
            description = "Marks a vehicle as inactive/out of service. The vehicle record is preserved and can be reactivated. This is NOT a soft-delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle successfully deactivated"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponseDTO> deactivateVehicle(
            @Parameter(description = "Vehicle unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iVehicleService.deactivateVehicle(id));
    }

    // ============================================================
    // VEHICLE TYPE ENDPOINTS
    // ============================================================

    @PostMapping("/types")
    @Operation(summary = "Create a new vehicle type",
            description = "Registers a new vehicle type in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle type successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "A vehicle type with that name already exists")
    })
    public ResponseEntity<VehicleTypeResponseDTO> createVehicleType(
            @Validated(OnCreate.class) @RequestBody VehicleTypeDTO vehicleTypeDTO) {
        return new ResponseEntity<>(iVehicleTypeService.createVehicleType(vehicleTypeDTO), HttpStatus.CREATED);
    }

    @GetMapping("/types")
    @Operation(summary = "Get all vehicle types",
            description = "Returns the full list of available vehicle types.")
    @ApiResponse(responseCode = "200", description = "Vehicle type list successfully retrieved")
    public ResponseEntity<List<VehicleTypeResponseDTO>> getAllVehicleTypes() {
        return ResponseEntity.ok(iVehicleTypeService.getAllVehicleTypes());
    }

    @GetMapping("/types/{id}")
    @Operation(summary = "Get vehicle type by ID",
            description = "Returns the information of a specific vehicle type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle type found"),
            @ApiResponse(responseCode = "404", description = "Vehicle type not found")
    })
    public ResponseEntity<VehicleTypeResponseDTO> getVehicleTypeById(
            @Parameter(description = "Vehicle type unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iVehicleTypeService.getVehicleTypeById(id));
    }

    @PatchMapping("/types/{id}")
    @Operation(summary = "Update vehicle type",
            description = "Updates an existing vehicle type. Only provided fields will be updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle type successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Vehicle type not found"),
            @ApiResponse(responseCode = "409", description = "A vehicle type with that name already exists")
    })
    public ResponseEntity<VehicleTypeResponseDTO> updateVehicleType(
            @Parameter(description = "Vehicle type unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody VehicleTypeDTO vehicleTypeDTO) {
        return ResponseEntity.ok(iVehicleTypeService.updateVehicleType(id, vehicleTypeDTO));
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "Delete vehicle type",
            description = "Deletes a vehicle type from the system. Cannot be deleted if there are associated vehicles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vehicle type successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Vehicle type not found"),
            @ApiResponse(responseCode = "409", description = "Vehicle type has associated vehicles and cannot be deleted")
    })
    public ResponseEntity<Void> deleteVehicleType(
            @Parameter(description = "Vehicle type unique identifier", required = true) @PathVariable Long id) {
        iVehicleTypeService.deleteVehicleType(id);
        return ResponseEntity.noContent().build();
    }
}

