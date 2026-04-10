package PSG.backEnd.controller;

import PSG.backEnd.model.dto.payment.*;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IPaymentService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import PSG.backEnd.model.constants.AppPermissions;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "API for managing payments to suppliers. " +
        "Handles different payment methods including cash, bank transfers, and checks. " +
        "Payments can be linked to one or more transactional documents (invoices) for tracking and accounting purposes.")
public class PaymentController {

    private final IPaymentService paymentService;

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_WRITE + "')")
    @PostMapping("/cash")
    @Operation(summary = "Create a cash payment",
            description = "Registers a new payment made in cash (efectivo). Include payment date, amount, supplier, " +
                    "and optionally link it to one or more invoices or documents being paid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cash payment successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (negative amount, invalid date, etc.)"),
            @ApiResponse(responseCode = "404", description = "Supplier not found or referenced documents not found")
    })
    public ResponseEntity<CashPaymentResponseDTO> createCash(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cash payment data including payment details (date, amount, supplier, optional document references)",
                    required = true
            )
            @Validated(OnCreate.class) @RequestBody CashPaymentDTO dto) {
        CashPaymentResponseDTO response = paymentService.createCash(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_WRITE + "')")
    @PostMapping("/transfer")
    @Operation(summary = "Create a bank transfer payment",
            description = "Registers a new payment made via bank transfer (transferencia). " +
                    "Include payment details, transaction number, and bank name for tracking purposes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transfer payment successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (invalid transaction number format, missing bank name, etc.)"),
            @ApiResponse(responseCode = "404", description = "Supplier not found or referenced documents not found")
    })
    public ResponseEntity<TransferPaymentResponseDTO> createTransfer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transfer payment data including transaction number, bank name, and payment details",
                    required = true
            )
            @Validated(OnCreate.class) @RequestBody TransferPaymentDTO dto) {
        TransferPaymentResponseDTO response = paymentService.createTransfer(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_WRITE + "')")
    @PostMapping("/check")
    @Operation(summary = "Create a check payment",
            description = "Registers a new payment made by check (cheque). " +
                    "Include check number, bank name, due date, and payment details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Check payment successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (due date not in future, invalid check number, etc.)"),
            @ApiResponse(responseCode = "404", description = "Supplier not found or referenced documents not found")
    })
    public ResponseEntity<CheckPaymentResponseDTO> createCheck(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Check payment data including check number, bank name, due date, and payment details",
                    required = true
            )
            @Validated(OnCreate.class) @RequestBody CheckPaymentDTO dto) {
        CheckPaymentResponseDTO response = paymentService.createCheck(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_READ + "')")
    @GetMapping
    @Operation(summary = "Get all payments with filters",
            description = "Retrieves a paginated list of all payments (cash, transfer, and check) with optional filtering. " +
                    "Supports filtering by payment method, date range, amount range, transaction number, and supplier. " +
                    "Useful for financial reports, payment tracking, and accounting reconciliation.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved payments list")
    public ResponseEntity<Page<PaymentResponseDTO>> getAll(
            @Parameter(description = "Filter by payment method type", example = "EFECTIVO") @RequestParam(required = false) PaymentMethod paymentMethod,
            @Parameter(description = "Filter by minimum payment date (inclusive). Format: yyyy-MM-dd", example = "2025-01-01") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "Filter by maximum payment date (inclusive). Format: yyyy-MM-dd", example = "2025-12-31") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "Filter by minimum payment amount", example = "1000.00") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Filter by maximum payment amount", example = "50000.00") @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Filter by transaction number (check number or transfer number)", example = "TRF20250115") @RequestParam(required = false) String transactionNumber,
            @Parameter(description = "Filter by supplier name (partial match on legal or trade name)", example = "Proveedor SA") @RequestParam(required = false) String supplierName,
            @Parameter(description = "Filter by supplier ID who received the payment", example = "42") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, paymentDate, amount, comment. " +
                    "For supplier use: supplierId, supplierLegalName, supplierTradeName, supplierCuit. " +
                    "Example: sortBy=paymentDate",
                    example = "paymentDate")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)", example = "desc") @RequestParam(defaultValue = "asc") String sortDir) {

        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        PaymentFilterDTO filter = new PaymentFilterDTO(
                paymentMethod, startDate, endDate, minAmount, maxAmount, transactionNumber, supplierName, supplierId
        );
        return ResponseEntity.ok(paymentService.findAll(filter, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     * Note: The query works directly with PaymentDetails entity, so we don't need paymentDetails prefix.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "supplierId" -> "supplier.id";
            case "supplierLegalName" -> "supplier.legalName";
            case "supplierTradeName" -> "supplier.tradeName";
            case "supplierCuit" -> "supplier.cuit";
            // paymentDate and amount are direct fields of PaymentDetails, no mapping needed
            default -> sortBy; // For 'id', 'paymentDate', 'amount', etc.
        };
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_READ + "')")
    @GetMapping("/by-document/{documentId}")
    @Operation(summary = "Get payment ID by document ID",
            description = "Returns the payment-details ID of the active payment that covers the given document. Returns 204 if no payment is linked.")
    public ResponseEntity<Long> getPaymentIdByDocumentId(
            @Parameter(description = "Transactional document ID", required = true) @PathVariable Long documentId) {
        return paymentService.findPaymentIdByDocumentId(documentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID",
            description = "Retrieves a payment by its PaymentDetails ID, automatically resolving the payment type (cash, transfer or check). " +
                    "The response 'type' field indicates the actual payment method.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found or has been deleted")
    })
    public ResponseEntity<PaymentResponseDTO> getById(
            @Parameter(description = "Payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_READ + "')")
    @GetMapping("/{id}/payment-order")
    @Operation(summary = "Generate payment order PDF",
            description = "Generates a PDF payment order document for the specified payment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generated successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<byte[]> generatePaymentOrderPdf(
            @Parameter(description = "Payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        byte[] pdf = paymentService.generatePaymentOrderPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "orden-de-pago-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_READ + "')")
    @GetMapping("/cash/{id}")
    @Operation(summary = "Get cash payment by ID",
            description = "Retrieves detailed information about a specific cash payment by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cash payment found"),
            @ApiResponse(responseCode = "404", description = "Cash payment not found or has been deleted")
    })
    public ResponseEntity<CashPaymentResponseDTO> getCash(
            @Parameter(description = "Cash payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        CashPaymentResponseDTO response = paymentService.getCash(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_READ + "')")
    @GetMapping("/transfer/{id}")
    @Operation(summary = "Get transfer payment by ID",
            description = "Retrieves detailed information about a specific bank transfer payment by its unique identifier, " +
                    "including transaction number and bank details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer payment found"),
            @ApiResponse(responseCode = "404", description = "Transfer payment not found or has been deleted")
    })
    public ResponseEntity<TransferPaymentResponseDTO> getTransfer(
            @Parameter(description = "Transfer payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        TransferPaymentResponseDTO response = paymentService.getTransfer(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_READ + "')")
    @GetMapping("/check/{id}")
    @Operation(summary = "Get check payment by ID",
            description = "Retrieves detailed information about a specific check payment by its unique identifier, " +
                    "including check number, bank name, and due date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check payment found"),
            @ApiResponse(responseCode = "404", description = "Check payment not found or has been deleted")
    })
    public ResponseEntity<CheckPaymentResponseDTO> getCheck(
            @Parameter(description = "Check payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        CheckPaymentResponseDTO response = paymentService.getCheck(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_WRITE + "')")
    @PatchMapping("/cash/{id}")
    @Operation(summary = "Update cash payment",
            description = "Updates an existing cash payment. Only provided fields will be updated. " +
                    "Can update payment date, amount, comments, and linked documents.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cash payment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Cash payment not found or has been deleted")
    })
    public ResponseEntity<CashPaymentResponseDTO> updateCash(
            @Parameter(description = "Cash payment unique identifier", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cash payment data to update. Only include fields you want to modify.",
                    required = true
            )
            @Validated(OnUpdate.class) @RequestBody CashPaymentDTO dto) {
        CashPaymentResponseDTO response = paymentService.updateCash(id, dto);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_WRITE + "')")
    @PatchMapping("/transfer/{id}")
    @Operation(summary = "Update transfer payment",
            description = "Updates an existing bank transfer payment. Only provided fields will be updated. " +
                    "Can update transaction number, bank name, payment date, amount, and comments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer payment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Transfer payment not found or has been deleted")
    })
    public ResponseEntity<TransferPaymentResponseDTO> updateTransfer(
            @Parameter(description = "Transfer payment unique identifier", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transfer payment data to update. Only include fields you want to modify.",
                    required = true
            )
            @Validated(OnUpdate.class) @RequestBody TransferPaymentDTO dto) {
        TransferPaymentResponseDTO response = paymentService.updateTransfer(id, dto);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_WRITE + "')")
    @PatchMapping("/check/{id}")
    @Operation(summary = "Update check payment",
            description = "Updates an existing check payment. Only provided fields will be updated. " +
                    "Can update check number, bank name, due date, payment date, amount, and comments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check payment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Check payment not found or has been deleted")
    })
    public ResponseEntity<CheckPaymentResponseDTO> updateCheck(
            @Parameter(description = "Check payment unique identifier", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Check payment data to update. Only include fields you want to modify.",
                    required = true
            )
            @Validated(OnUpdate.class) @RequestBody CheckPaymentDTO dto) {
        CheckPaymentResponseDTO response = paymentService.updateCheck(id, dto);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payment by ID",
            description = "Performs a soft delete of a payment by its PaymentDetails ID, automatically resolving the type (cash, transfer or check). " +
                    "The payment is marked as deleted but remains in the database for audit trails.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        paymentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_DELETE + "')")
    @DeleteMapping("/cash/{id}")
    @Operation(summary = "Delete cash payment",
            description = "Performs a soft delete of a cash payment. The payment is marked as deleted but remains in the database for audit trails. " +
                    "Cannot delete payments that are referenced in financial reports or reconciliations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cash payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Cash payment not found"),
            @ApiResponse(responseCode = "400", description = "Cannot delete payment with active references")
    })
    public ResponseEntity<Void> deleteCash(
            @Parameter(description = "Cash payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        paymentService.deleteCash(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_DELETE + "')")
    @DeleteMapping("/transfer/{id}")
    @Operation(summary = "Delete transfer payment",
            description = "Performs a soft delete of a bank transfer payment. The payment is marked as deleted but remains in the database for audit trails. " +
                    "Cannot delete payments that are referenced in financial reports or reconciliations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transfer payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Transfer payment not found"),
            @ApiResponse(responseCode = "400", description = "Cannot delete payment with active references")
    })
    public ResponseEntity<Void> deleteTransfer(
            @Parameter(description = "Transfer payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        paymentService.deleteTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.PAYMENT_DELETE + "')")
    @DeleteMapping("/check/{id}")
    @Operation(summary = "Delete check payment",
            description = "Performs a soft delete of a check payment. The payment is marked as deleted but remains in the database for audit trails. " +
                    "Cannot delete payments that are referenced in financial reports or reconciliations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Check payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Check payment not found"),
            @ApiResponse(responseCode = "400", description = "Cannot delete payment with active references")
    })
    public ResponseEntity<Void> deleteCheck(
            @Parameter(description = "Check payment unique identifier", required = true, example = "1") @PathVariable Long id) {
        paymentService.deleteCheck(id);
        return ResponseEntity.noContent().build();
    }
}