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
            description = "Returns the built-in FuelType constants followed by the tenant's custom fuel types. Deleted custom " +
                    "types are excluded unless includeDeleted=true (needed to resolve the label of a value already assigned " +
                    "to an existing price or fuel load).")
    @ApiResponse(responseCode = "200", description = "Fuel type list retrieved successfully")
    public ResponseEntity<List<FuelTypeOptionDTO>> getAllFuelTypes(
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(fuelTypeCatalogService.listAll(includeDeleted));
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

    @PreAuthorize("hasAuthority('" + AppPermissions.GAS_STATION_WRITE + "')")
    @DeleteMapping("/{key}")
    @Operation(summary = "Delete a custom fuel type",
            description = "Soft-deletes a tenant-scoped custom fuel type so it stops appearing in selectable lists. " +
                    "Existing prices/fuel loads that already reference it keep resolving its label; its key becomes free " +
                    "to reuse for a new custom fuel type. Built-in FuelType constants cannot be deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Custom fuel type deleted successfully"),
            @ApiResponse(responseCode = "404", description = "No custom fuel type with that key")
    })
    public ResponseEntity<Void> deleteCustomFuelType(@PathVariable String key) {
        fuelTypeCatalogService.deleteCustom(key);
        return ResponseEntity.noContent().build();
    }
}
