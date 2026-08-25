package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.gasStation.CreateCustomFuelTypeDTO;
import PSG.backEnd.model.dto.gasStation.FuelTypeOptionDTO;
import PSG.backEnd.service.port.IFuelTypeCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fuel-types")
@RequiredArgsConstructor
@Tag(name = "Fuel Types", description = "API for the fuel type catalog: built-in FuelType constants plus tenant-defined custom types.")
public class FuelTypeController {

    private final IFuelTypeCatalogService fuelTypeCatalogService;

    @PreAuthorize("hasAuthority('" + AppPermissions.GAS_STATION_READ + "')")
    @GetMapping
    @Operation(summary = "List all selectable fuel types",
            description = "Returns the built-in FuelType constants followed by the tenant's active custom fuel types. " +
                    "Used to populate fuel type selectors for gas station prices and fuel loads.")
    @ApiResponse(responseCode = "200", description = "Fuel type list retrieved successfully")
    public ResponseEntity<List<FuelTypeOptionDTO>> getAllFuelTypes() {
        return ResponseEntity.ok(fuelTypeCatalogService.listAll());
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.GAS_STATION_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a custom fuel type",
            description = "Registers a new tenant-scoped custom fuel type from a display label. The label is normalized into " +
                    "a storage key; creation fails if that key collides with a built-in FuelType or an existing custom type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Custom fuel type created successfully"),
            @ApiResponse(responseCode = "409", description = "A fuel type with that name already exists")
    })
    public ResponseEntity<FuelTypeOptionDTO> createCustomFuelType(
            @Valid @RequestBody CreateCustomFuelTypeDTO dto) {
        return new ResponseEntity<>(fuelTypeCatalogService.createCustom(dto.label()), HttpStatus.CREATED);
    }
}
