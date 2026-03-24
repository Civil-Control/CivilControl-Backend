package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.contracts.WorkContractDTO;
import PSG.backEnd.model.dto.contracts.WorkContractFilterDTO;
import PSG.backEnd.model.dto.contracts.WorkContractResponseDTO;
import PSG.backEnd.model.dto.contracts.WorkContractStatsDTO;
import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.contracts.WorkContractStatus;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IWorkContractService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/work-contracts")
@RequiredArgsConstructor
@Tag(name = "Work Contracts", description = "Endpoints for managing civil work contracts.")
public class WorkContractController {

    private final IWorkContractService workContractService;

    @PostMapping
    @Operation(summary = "Create a new work contract")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Work contract created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.WORK_CONTRACT_WRITE + "')")
    public ResponseEntity<WorkContractResponseDTO> createWorkContract(
            @Validated(OnCreate.class) @RequestBody WorkContractDTO dto) {
        return new ResponseEntity<>(workContractService.createWorkContract(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all work contracts with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of work contracts")
    @PreAuthorize("hasAuthority('" + AppPermissions.WORK_CONTRACT_READ + "')")
    public ResponseEntity<Page<WorkContractResponseDTO>> getAllWorkContracts(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String clientBusinessName,
            @RequestParam(required = false) String contractNumber,
            @RequestParam(required = false) Long projectAreaId,
            @RequestParam(required = false) WorkContractStatus status,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) LocalDate contractDateFrom,
            @RequestParam(required = false) LocalDate contractDateTo,
            @RequestParam(required = false) BigDecimal minContractedAmount,
            @RequestParam(required = false) BigDecimal maxContractedAmount,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "contractDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        WorkContractFilterDTO filterDTO = new WorkContractFilterDTO(
                clientId, clientBusinessName, contractNumber, projectAreaId, status, currency,
                contractDateFrom, contractDateTo, minContractedAmount, maxContractedAmount, search);
        return ResponseEntity.ok(workContractService.getAllWorkContracts(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a work contract by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work contract found"),
        @ApiResponse(responseCode = "404", description = "Work contract not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.WORK_CONTRACT_READ + "')")
    public ResponseEntity<WorkContractResponseDTO> getWorkContractById(@PathVariable Long id) {
        return ResponseEntity.ok(workContractService.getWorkContractById(id));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get certification and billing stats for a work contract")
    @ApiResponse(responseCode = "200", description = "Stats computed successfully")
    @PreAuthorize("hasAuthority('" + AppPermissions.WORK_CONTRACT_READ + "')")
    public ResponseEntity<WorkContractStatsDTO> getWorkContractStats(@PathVariable Long id) {
        return ResponseEntity.ok(workContractService.getWorkContractStats(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a work contract")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work contract updated successfully"),
        @ApiResponse(responseCode = "404", description = "Work contract not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.WORK_CONTRACT_WRITE + "')")
    public ResponseEntity<WorkContractResponseDTO> updateWorkContract(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody WorkContractDTO dto) {
        return ResponseEntity.ok(workContractService.updateWorkContract(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a work contract")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Work contract deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Work contract not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.WORK_CONTRACT_DELETE + "')")
    public ResponseEntity<Void> deleteWorkContract(@PathVariable Long id) {
        workContractService.deleteWorkContract(id);
        return ResponseEntity.noContent().build();
    }
}
