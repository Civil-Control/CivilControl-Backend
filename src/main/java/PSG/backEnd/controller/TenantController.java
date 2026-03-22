package PSG.backEnd.controller;

import PSG.backEnd.model.dto.tenant.TenantCreateDTO;
import PSG.backEnd.model.dto.tenant.TenantDTO;
import PSG.backEnd.model.dto.tenant.TenantFilterDTO;
import PSG.backEnd.model.dto.tenant.TenantResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ITenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import PSG.backEnd.model.constants.AppPermissions;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "API for managing tenants (construction companies).")
public class TenantController {

    private final ITenantService tenantService;

    @PreAuthorize("hasAuthority('" + AppPermissions.TENANT_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tenant successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "409", description = "Tenant already exists with the same CUIT")
    })
    public ResponseEntity<TenantResponseDTO> createTenant(
            @Validated({Default.class, OnCreate.class}) @RequestBody TenantCreateDTO dto) {
        TenantResponseDTO created = tenantService.createTenant(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TENANT_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Tenant not found"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate CUIT")
    })
    public ResponseEntity<TenantResponseDTO> updateTenant(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody TenantDTO dto) {
        TenantResponseDTO updated = tenantService.updateTenant(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TENANT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tenant successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current tenant")
    @ApiResponse(responseCode = "200", description = "Current tenant found")
    public ResponseEntity<TenantResponseDTO> getCurrentTenant() {
        TenantResponseDTO tenant = tenantService.getCurrentTenant();
        return ResponseEntity.ok(tenant);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TENANT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant found"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantResponseDTO> getTenantById(@PathVariable Long id) {
        TenantResponseDTO tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TENANT_READ + "')")
    @GetMapping
    @Operation(summary = "Get all tenants with filters")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved tenant list")
    public ResponseEntity<Page<TenantResponseDTO>> getTenants(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cuit,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        TenantFilterDTO filterDTO = new TenantFilterDTO(name, cuit, active, search);
        return ResponseEntity.ok(tenantService.getAllTenants(filterDTO, pageable));
    }
}
