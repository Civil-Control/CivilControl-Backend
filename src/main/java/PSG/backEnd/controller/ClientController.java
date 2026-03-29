package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.client.ClientDTO;
import PSG.backEnd.model.dto.client.ClientFilterDTO;
import PSG.backEnd.model.dto.client.ClientResponseDTO;
import PSG.backEnd.model.dto.client.ClientStatsDTO;
import PSG.backEnd.model.enums.IvaCondition;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IClientService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client Management", description = "Endpoints for managing sales clients.")
public class ClientController {

    private final IClientService clientService;

    @PostMapping
    @Operation(summary = "Create a new client")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Client created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Client already exists")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CLIENT_WRITE + "')")
    public ResponseEntity<ClientResponseDTO> createClient(
            @Validated(OnCreate.class) @RequestBody ClientDTO dto) {
        return new ResponseEntity<>(clientService.createClient(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all clients with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of clients")
    @PreAuthorize("hasAuthority('" + AppPermissions.CLIENT_READ + "')")
    public ResponseEntity<Page<ClientResponseDTO>> getAllClients(
            @RequestParam(required = false) String cuit,
            @RequestParam(required = false) String businessName,
            @RequestParam(required = false) String tradeName,
            @RequestParam(required = false) IvaCondition ivaCondition,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        ClientFilterDTO filterDTO = new ClientFilterDTO(cuit, businessName, tradeName, ivaCondition, active, search);
        return ResponseEntity.ok(clientService.getAllClients(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a client by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Client found"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CLIENT_READ + "')")
    public ResponseEntity<ClientResponseDTO> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get invoicing statistics for a client")
    @ApiResponse(responseCode = "200", description = "Client statistics")
    @PreAuthorize("hasAuthority('" + AppPermissions.CLIENT_READ + "')")
    public ResponseEntity<ClientStatsDTO> getClientStats(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(clientService.getClientStats(id, fromDate, toDate));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a client")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Client updated successfully"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CLIENT_WRITE + "')")
    public ResponseEntity<ClientResponseDTO> updateClient(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody ClientDTO dto) {
        return ResponseEntity.ok(clientService.updateClient(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a client")
    @ApiResponse(responseCode = "204", description = "Client deleted successfully")
    @PreAuthorize("hasAuthority('" + AppPermissions.CLIENT_DELETE + "')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
