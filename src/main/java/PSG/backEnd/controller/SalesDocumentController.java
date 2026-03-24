package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.sales.SalesDocumentDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentFilterDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentResponseDTO;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ISalesDocumentService;
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
@RequestMapping("/api/v1/sales-documents")
@RequiredArgsConstructor
@Tag(name = "Sales Documents", description = "Endpoints for managing sales invoices and credit/debit notes.")
public class SalesDocumentController {

    private final ISalesDocumentService salesDocumentService;

    @PostMapping
    @Operation(summary = "Create a new sales document")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sales document created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.SALES_DOCUMENT_WRITE + "')")
    public ResponseEntity<SalesDocumentResponseDTO> createSalesDocument(
            @Validated(OnCreate.class) @RequestBody SalesDocumentDTO dto) {
        return new ResponseEntity<>(salesDocumentService.createSalesDocument(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all sales documents with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of sales documents")
    @PreAuthorize("hasAuthority('" + AppPermissions.SALES_DOCUMENT_READ + "')")
    public ResponseEntity<Page<SalesDocumentResponseDTO>> getAllSalesDocuments(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) SalesDocumentType documentType,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal minTotal,
            @RequestParam(required = false) BigDecimal maxTotal,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) Long projectAreaId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        SalesDocumentFilterDTO filterDTO = new SalesDocumentFilterDTO(
                clientId, documentType, documentNumber, dateFrom, dateTo,
                minTotal, maxTotal, paid, projectAreaId, search);
        return ResponseEntity.ok(salesDocumentService.getAllSalesDocuments(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a sales document by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sales document found"),
        @ApiResponse(responseCode = "404", description = "Sales document not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.SALES_DOCUMENT_READ + "')")
    public ResponseEntity<SalesDocumentResponseDTO> getSalesDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(salesDocumentService.getSalesDocumentById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a sales document")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sales document updated successfully"),
        @ApiResponse(responseCode = "404", description = "Sales document not found")
    })
    @PreAuthorize("hasAuthority('" + AppPermissions.SALES_DOCUMENT_WRITE + "')")
    public ResponseEntity<SalesDocumentResponseDTO> updateSalesDocument(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody SalesDocumentDTO dto) {
        return ResponseEntity.ok(salesDocumentService.updateSalesDocument(id, dto));
    }

    @PatchMapping("/{id}/paid")
    @Operation(summary = "Mark a sales document as paid or unpaid")
    @ApiResponse(responseCode = "200", description = "Payment status updated")
    @PreAuthorize("hasAuthority('" + AppPermissions.SALES_DOCUMENT_WRITE + "')")
    public ResponseEntity<SalesDocumentResponseDTO> markAsPaid(
            @PathVariable Long id,
            @RequestParam boolean paid) {
        return ResponseEntity.ok(salesDocumentService.markAsPaid(id, paid));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a sales document")
    @ApiResponse(responseCode = "204", description = "Sales document deleted successfully")
    @PreAuthorize("hasAuthority('" + AppPermissions.SALES_DOCUMENT_DELETE + "')")
    public ResponseEntity<Void> deleteSalesDocument(@PathVariable Long id) {
        salesDocumentService.deleteSalesDocument(id);
        return ResponseEntity.noContent().build();
    }
}
