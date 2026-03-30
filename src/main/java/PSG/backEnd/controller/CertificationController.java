package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.contracts.CertificationDTO;
import PSG.backEnd.model.dto.contracts.CertificationFilterDTO;
import PSG.backEnd.model.dto.contracts.CertificationResponseDTO;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ICertificationService;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/certifications")
@RequiredArgsConstructor
@Tag(name = "Certifications", description = "Endpoints for managing work certifications.")
public class CertificationController {

    private final ICertificationService certificationService;

    @PostMapping
    @Operation(summary = "Create a new certification")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Certification created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CERTIFICATION_WRITE + "')")
    public ResponseEntity<CertificationResponseDTO> createCertification(
            @Validated(OnCreate.class) @RequestBody CertificationDTO dto) {
        return new ResponseEntity<>(certificationService.createCertification(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all certifications with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of certifications")
    @PreAuthorize("hasAuthority('" + AppPermissions.CERTIFICATION_READ + "')")
    public ResponseEntity<Page<CertificationResponseDTO>> getAllCertifications(
            @RequestParam(required = false) Long workContractId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) CertificationStatus status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Boolean hasInvoice,
            @RequestParam(required = false) Long salesDocumentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "certificationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        CertificationFilterDTO filterDTO = new CertificationFilterDTO(
                workContractId, clientId, status, dateFrom, dateTo, hasInvoice, salesDocumentId);
        return ResponseEntity.ok(certificationService.getAllCertifications(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a certification by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Certification found"),
        @ApiResponse(responseCode = "404", description = "Certification not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CERTIFICATION_READ + "')")
    public ResponseEntity<CertificationResponseDTO> getCertificationById(@PathVariable Long id) {
        return ResponseEntity.ok(certificationService.getCertificationById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a certification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Certification updated successfully"),
        @ApiResponse(responseCode = "404", description = "Certification not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CERTIFICATION_WRITE + "')")
    public ResponseEntity<CertificationResponseDTO> updateCertification(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody CertificationDTO dto) {
        return ResponseEntity.ok(certificationService.updateCertification(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a certification")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Certification deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Certification not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CERTIFICATION_DELETE + "')")
    public ResponseEntity<Void> deleteCertification(@PathVariable Long id) {
        certificationService.deleteCertification(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cobrado")
    @Operation(summary = "Mark a certification as cobrado (collected)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Certification marked as cobrado"),
        @ApiResponse(responseCode = "404", description = "Certification not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.CERTIFICATION_WRITE + "')")
    public ResponseEntity<CertificationResponseDTO> markAsCobrado(@PathVariable Long id) {
        return ResponseEntity.ok(certificationService.markAsCobrado(id));
    }
}
