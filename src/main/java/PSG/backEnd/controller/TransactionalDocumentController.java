package PSG.backEnd.controller;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentFilterDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.dto.transactionalDocument.LinkedRecordsSummaryDTO;
import PSG.backEnd.model.validation.ValidationGroups;
import PSG.backEnd.service.port.ITransactionalDocumentService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import PSG.backEnd.model.constants.AppPermissions;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactional-documents")
@RequiredArgsConstructor
@Tag(name = "Transactional Document Management", description = "API for managing transactional documents such as invoices, credit notes, and debit notes. " +
        "These documents represent commercial transactions with suppliers, including purchases, returns, and adjustments. " +
        "Each document contains itemized details, tax calculations (IVA), and can be linked to payments for accounting purposes.")
public class TransactionalDocumentController {

    private final ITransactionalDocumentService iTransactionalDocumentService;

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new transactional document",
            description = "Registers a new transactional document (invoice, credit note, or debit note) in the system. " +
                    "Include document type (FACTURA_A/B/C, NOTA_CREDITO_A/B/C, NOTA_DEBITO_A/B/C), supplier information, " +
                    "itemized list of goods/services, tax calculations, and optional project area assignment. " +
                    "The document follows Argentine AFIP standards with branch code and document number.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transactional document successfully created. Returns the created document with assigned ID."),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (invalid AFIP format, missing items, tax calculation errors, etc.)"),
            @ApiResponse(responseCode = "404", description = "Supplier or project area not found"),
            @ApiResponse(responseCode = "409", description = "Document already exists with the same branch code, document number, and supplier combination")
    })
    public ResponseEntity<TransactionalDocumentResponseDTO> createTransactionalDocument(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transactional document data including type, supplier, branch code, document number, items, taxes, and totals",
                    required = true
            )
            @Validated(ValidationGroups.OnCreate.class) @RequestBody TransactionalDocumentDTO transactionalDocumentDTO) {
        TransactionalDocumentResponseDTO response = iTransactionalDocumentService.createTransactionalDocument(transactionalDocumentDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_READ + "')")
    @GetMapping
    @Operation(summary = "Get all transactional documents with filters",
            description = "Retrieves a paginated list of transactional documents with optional filtering by document number, supplier information, " +
                    "project area, amount range, date range, and payment status. " +
                    "Supports sorting by any field. Useful for financial reports, expense tracking, and supplier account reconciliation.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved transactional documents list")
    public ResponseEntity<Page<TransactionalDocumentResponseDTO>> getTransactionalDocuments(
            @Parameter(description = "Filter by document number (partial match). Example: 00012345", example = "12345")
            @RequestParam(required = false) String documentNumber,

            @Parameter(description = "Filter by document type (enum key). Values: BILL_A, BILL_B, BILL_C, DEBIT_NOTE_A, DEBIT_NOTE_B, DEBIT_NOTE_C, CREDIT_NOTE_A, CREDIT_NOTE_B, CREDIT_NOTE_C, OTHER_DOCUMENT", example = "BILL_A")
            @RequestParam(required = false) String documentType,

            @Parameter(description = "Filter by supplier CUIT (tax ID, partial match). Format: XX-XXXXXXXX-X", example = "30-12345678")
            @RequestParam(required = false) String supplierCuit,

            @Parameter(description = "Filter by supplier name (legal or trade name, partial match, case-insensitive)", example = "García")
            @RequestParam(required = false) String supplierName,

            @Parameter(description = "Filter by project area ID. References the organizational division.", example = "5")
            @RequestParam(required = false) Long projectAreaId,

            @Parameter(description = "Filter by project area name (partial match, case-insensitive)", example = "Obras Norte")
            @RequestParam(required = false) String projectAreaName,

            @Parameter(description = "Filter by minimum total amount (inclusive)", example = "1000.00")
            @RequestParam(required = false) BigDecimal minTotalAmount,

            @Parameter(description = "Filter by maximum total amount (inclusive)", example = "100000.00")
            @RequestParam(required = false) BigDecimal maxTotalAmount,

            @Parameter(description = "Filter by exact total amount", example = "15750.50")
            @RequestParam(required = false) BigDecimal totalAmount,

            @Parameter(description = "Filter by minimum document date (inclusive). Format: yyyy-MM-dd", example = "2025-01-01")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,

            @Parameter(description = "Filter by maximum document date (inclusive). Format: yyyy-MM-dd", example = "2025-12-31")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,

            @Parameter(description = "Filter by payment status. True for paid documents, false for unpaid, omit for all.", example = "false")
            @RequestParam(required = false) Boolean paid,

            @Parameter(description = "Generic search across supplier name and document number (partial match)")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by. Direct fields: date, total, documentNumber, branchCode, netTotal, ivaTotal, discountPercentage. " +
                    "For supplier fields use: supplierLegalName, supplierTradeName, supplierCuit, supplierId. " +
                    "For project area use: projectAreaName, projectAreaId. " +
                    "Example: sortBy=supplierLegalName",
                    example = "date")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Sort direction (asc or desc)", example = "desc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        TransactionalDocumentFilterDTO filter = new TransactionalDocumentFilterDTO(
                documentNumber, documentType, supplierCuit, supplierName, projectAreaId, projectAreaName, minTotalAmount,
                maxTotalAmount, totalAmount, fromDate, toDate, paid, search
        );
        return ResponseEntity.ok(iTransactionalDocumentService.getAllTransactionalDocuments(filter, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "supplierName", "supplierLegalName" -> "supplier.legalName";
            case "supplierTradeName" -> "supplier.tradeName";
            case "supplierCuit" -> "supplier.cuit";
            case "supplierId" -> "supplier.id";
            case "projectAreaName" -> "projectArea.name";
            case "projectAreaId" -> "projectArea.id";
            default -> sortBy; // For 'id', 'date', 'total', 'documentNumber', 'branchCode', 'netTotal', 'ivaTotal', 'discountPercentage', etc.
        };
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get transactional document by ID",
            description = "Retrieves detailed information about a specific transactional document by its unique identifier, " +
                    "including complete item list, tax breakdown, supplier information, and payment status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactional document found"),
            @ApiResponse(responseCode = "404", description = "Transactional document not found or has been deleted")
    })
    public ResponseEntity<TransactionalDocumentResponseDTO> getTransactionalDocumentById(
            @Parameter(description = "Transactional document unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(iTransactionalDocumentService.getTransactionalDocumentById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update transactional document",
            description = "Updates an existing transactional document. Only provided fields will be updated. " +
                    "Can update document details, items, tax calculations, totals, and project area assignment. " +
                    "Validates that the document exists and isn't marked as deleted. " +
                    "Use this endpoint to correct errors or add missing information to existing documents.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactional document successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (tax calculation mismatch, invalid totals, etc.)"),
            @ApiResponse(responseCode = "404", description = "Transactional document, supplier, or project area not found"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate document (same branch code, number, and supplier)")
    })
    public ResponseEntity<TransactionalDocumentResponseDTO> updateTransactionalDocument(
            @Parameter(description = "Transactional document unique identifier", required = true, example = "1")
            @PathVariable Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transactional document data to update. Only include fields you want to modify. Items list will replace existing items.",
                    required = true
            )
            @Validated(ValidationGroups.OnUpdate.class) @RequestBody TransactionalDocumentDTO transactionalDocumentDTO) {
        return ResponseEntity.ok(iTransactionalDocumentService.updateTransactionalDocument(id, transactionalDocumentDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_READ + "')")
    @GetMapping("/{id}/linked-records")
    @Operation(summary = "Get linked records for a transactional document",
            description = "Returns a summary of all records (repairs, fuel loads, salary payments, stock items) "
                    + "currently linked to the specified transactional document. Used before deletion to inform the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Linked records summary returned"),
            @ApiResponse(responseCode = "404", description = "Transactional document not found")
    })
    public ResponseEntity<LinkedRecordsSummaryDTO> getLinkedRecords(
            @Parameter(description = "Transactional document unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(iTransactionalDocumentService.getLinkedRecordsSummary(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transactional document",
            description = "Performs a soft delete of a transactional document. The document is marked as deleted but remains in the database for audit trails and historical records. " +
                    "Cannot delete documents that have associated payments or are referenced in financial reports.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transactional document successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete document with associated payments or active references"),
            @ApiResponse(responseCode = "404", description = "Transactional document not found")
    })
    public ResponseEntity<Void> deleteTransactionalDocument(
            @Parameter(description = "Transactional document unique identifier", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "When true, also deletes linked records (repairs, fuel loads, salary payments) and soft-deletes stock items", example = "false")
            @RequestParam(defaultValue = "false") boolean deleteLinkedRecords) {
        iTransactionalDocumentService.deleteTransactionalDocument(id, deleteLinkedRecords);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "')")
    @PostMapping("/{id}/mark-applied")
    @Operation(summary = "Mark a credit note as manually applied",
            description = "Flags a credit note as APPLIED without linking any invoice/debit-note. " +
                    "The supplier balance is not modified (it was already adjusted at creation time): " +
                    "only the document's status changes so it stops appearing as available credit. " +
                    "Only valid for CREDIT_NOTE_* documents that have no invoice links.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credit note marked as applied"),
            @ApiResponse(responseCode = "400", description = "Document is not a credit note or already has applications"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<TransactionalDocumentResponseDTO> markCreditNoteApplied(
            @Parameter(description = "Credit note unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(iTransactionalDocumentService.markCreditNoteApplied(id, true));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "')")
    @PostMapping("/{id}/mark-unapplied")
    @Operation(summary = "Revert a credit note's manual-applied flag",
            description = "Clears the manual-applied flag on a credit note, returning it to UNAPPLIED " +
                    "(\"Crédito disponible\") so it can again be linked to invoices/debit-notes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credit note marked as unapplied"),
            @ApiResponse(responseCode = "400", description = "Document is not a credit note"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<TransactionalDocumentResponseDTO> markCreditNoteUnapplied(
            @Parameter(description = "Credit note unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(iTransactionalDocumentService.markCreditNoteApplied(id, false));
    }
}