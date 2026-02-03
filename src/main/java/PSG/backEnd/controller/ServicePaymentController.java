package PSG.backEnd.controller;

import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IServicePaymentService;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/service-payments")
@RequiredArgsConstructor
@Tag(name = "Service Payments", description = "API for managing utility service payments. " +
        "Handles payments for electricity, water, gas, internet, phone, and other utility services for buildings. " +
        "Tracks payment history, amounts, and reference numbers for accounting and auditing purposes.")
public class ServicePaymentController {

    private final IServicePaymentService servicePaymentService;

    @PostMapping
    @Operation(summary = "Create a new service payment",
            description = "Registers a new payment for a utility service (electricity, water, gas, etc.) for a specific building. " +
                    "Associates the payment with a service supplier and validates that the service type is provided by that supplier. " +
                    "The reference number must be unique across all service payments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Service payment successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data, payment date in future, or service type not provided by supplier"),
            @ApiResponse(responseCode = "404", description = "Service supplier or building not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate reference number: A payment with this reference number already exists")
    })
    public ResponseEntity<ServicePaymentResponseDTO> createServicePayment(
            @Validated(OnCreate.class) @RequestBody ServicePaymentDTO servicePaymentDTO) {
        ServicePaymentResponseDTO createdServicePayment = servicePaymentService.createServicePayment(servicePaymentDTO);
        return new ResponseEntity<>(createdServicePayment, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all service payments with filters",
            description = "Retrieves a paginated list of service payments with optional filtering by service supplier, " +
                    "building, service type, date range, amount range, and reference number. " +
                    "Supports sorting and pagination. Useful for generating payment reports, tracking expenses, and auditing.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved service payments list")
    public ResponseEntity<Page<ServicePaymentResponseDTO>> getServicePayments(
            @Parameter(description = "Filter by service supplier ID")
            @RequestParam(required = false) Long serviceSupplierId,

            @Parameter(description = "Filter by building ID")
            @RequestParam(required = false) Long buildingId,

            @Parameter(description = "Filter by project area ID")
            @RequestParam(required = false) Long projectAreaId,

            @Parameter(description = "Filter by service type (LUZ, AGUA, GAS, INTERNET, TELEFONIA, etc.)")
            @RequestParam(required = false) ServiceType serviceType,

            @Parameter(description = "Filter by minimum payment date (inclusive)")
            @RequestParam(required = false) LocalDate startDate,

            @Parameter(description = "Filter by maximum payment date (inclusive)")
            @RequestParam(required = false) LocalDate endDate,

            @Parameter(description = "Filter by minimum amount (inclusive)")
            @RequestParam(required = false) BigDecimal minAmount,

            @Parameter(description = "Filter by maximum amount (inclusive)")
            @RequestParam(required = false) BigDecimal maxAmount,

            @Parameter(description = "Filter by reference number (partial match)")
            @RequestParam(required = false) String referenceNumber,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by. Direct fields: id, paymentDate, amount, serviceType, referenceNumber. " +
                    "For service supplier use: serviceSupplier.id. " +
                    "For building use: building.name, building.code. " +
                    "Example: sortBy=building.name",
                    example = "paymentDate")
            @RequestParam(defaultValue = "paymentDate") String sortBy,

            @Parameter(description = "Sort direction (asc or desc)")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        ServicePaymentFilterDTO filterDTO = new ServicePaymentFilterDTO(
                serviceSupplierId, buildingId, projectAreaId, serviceType,
                startDate, endDate, minAmount, maxAmount, referenceNumber
        );

        return ResponseEntity.ok(servicePaymentService.getAllServicePayments(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service payment by ID",
            description = "Retrieves detailed information about a specific service payment by its unique identifier, " +
                    "including supplier details, building information, payment amount, date, and reference number.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service payment found"),
            @ApiResponse(responseCode = "404", description = "Service payment not found or has been deleted")
    })
    public ResponseEntity<ServicePaymentResponseDTO> getServicePaymentById(
            @Parameter(description = "Service payment unique identifier", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(servicePaymentService.getServicePaymentById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update service payment",
            description = "Updates an existing service payment record. Only provided fields will be updated. " +
                    "Validates that the new service supplier provides the service type if any of these fields are changed. " +
                    "If updating the reference number, it must be unique across all service payments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service payment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data, payment date in future, or service type not provided by new supplier"),
            @ApiResponse(responseCode = "404", description = "Service payment, service supplier, or building not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate reference number: A payment with this reference number already exists")
    })
    public ResponseEntity<ServicePaymentResponseDTO> updateServicePayment(
            @Parameter(description = "Service payment unique identifier", required = true)
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody ServicePaymentDTO servicePaymentDTO) {
        return ResponseEntity.ok(servicePaymentService.updateServicePayment(id, servicePaymentDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete service payment",
            description = "Performs a soft deletion of a service payment. The record is marked as deleted but remains in the database " +
                    "for audit and historical tracking purposes. The payment can be referenced in reports but won't appear in active queries.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Service payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Service payment not found or already deleted")
    })
    public ResponseEntity<Void> deleteServicePayment(
            @Parameter(description = "Service payment unique identifier", required = true)
            @PathVariable Long id) {
        servicePaymentService.deleteServicePayment(id);
        return ResponseEntity.noContent().build();
    }
}

