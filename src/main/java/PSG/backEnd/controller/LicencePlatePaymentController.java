package PSG.backEnd.controller;

import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentBatchDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentFilterDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ILicencePlatePaymentService;
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
import java.util.List;

import PSG.backEnd.model.constants.AppPermissions;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/licence-plate-payments")
@RequiredArgsConstructor
@Tag(name = "Licence Plate Payments", description = "API for managing vehicle licence plate payments and taxes. Handles registration fees and periodic payments required by different jurisdictions.")
public class LicencePlatePaymentController {

    private final ILicencePlatePaymentService licencePlatePaymentService;

    @PreAuthorize("hasAuthority('" + AppPermissions.LICENCE_PLATE_PAYMENT_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new licence plate payment",
            description = "Creates a new licence plate payment record for a vehicle. Includes payment details such as amount, year, period, and jurisdiction type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Licence plate payment successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<LicencePlatePaymentResponseDTO> createLicencePlatePayment(
            @Validated(OnCreate.class) @RequestBody LicencePlatePaymentDTO licencePlatePaymentDTO) {
        LicencePlatePaymentResponseDTO createdPayment = licencePlatePaymentService.createLicencePlatePayment(licencePlatePaymentDTO);
        return new ResponseEntity<>(createdPayment, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LICENCE_PLATE_PAYMENT_WRITE + "')")
    @PostMapping("/batch")
    @Operation(summary = "Create multiple licence plate payments in batch",
            description = "Creates multiple licence plate payment records in a single request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Licence plate payments successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<List<LicencePlatePaymentResponseDTO>> createBatchLicencePlatePayments(
            @Validated @RequestBody LicencePlatePaymentBatchDTO batchDTO) {
        List<LicencePlatePaymentResponseDTO> created = licencePlatePaymentService.createBatchLicencePlatePayments(batchDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LICENCE_PLATE_PAYMENT_READ + "')")
    @GetMapping
    @Operation(summary = "Get all licence plate payments with filters",
            description = "Retrieves a paginated list of licence plate payments with optional filtering by date range, vehicle, amount, year, period, and jurisdiction.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved licence plate payment list")
    public ResponseEntity<Page<LicencePlatePaymentResponseDTO>> getLicencePlatePayments(
            @Parameter(description = "Filter payments from this date (inclusive)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter payments to this date (inclusive)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by vehicle ID") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Filter by vehicle license plate") @RequestParam(required = false) String vehicleLicensePlate,
            @Parameter(description = "Filter by project area ID") @RequestParam(required = false) Long projectAreaId,
            @Parameter(description = "Minimum payment amount") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum payment amount") @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Filter by payment year") @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by payment period (1-12)") @RequestParam(required = false) Integer period,
            @Parameter(description = "Filter by jurisdiction type") @RequestParam(required = false) String jurisdictionType,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, date, amount, year, period, jurisdictionType. " +
                    "For vehicle use: vehicleLicensePlate, vehicleBrand, vehicleModel, vehicleId. " +
                    "Example: sortBy=vehicleLicensePlate",
                    example = "date")
            @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        LicencePlatePaymentFilterDTO filterDTO = new LicencePlatePaymentFilterDTO(
                dateFrom, dateTo, vehicleId, vehicleLicensePlate, projectAreaId,
                minAmount, maxAmount, year, period, jurisdictionType
        );

        return ResponseEntity.ok(licencePlatePaymentService.getAllLicencePlatePayments(filterDTO, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "vehicleLicensePlate" -> "vehicle.licensePlate";
            case "vehicleBrand" -> "vehicle.brand";
            case "vehicleModel" -> "vehicle.model";
            case "vehicleId" -> "vehicle.id";
            default -> sortBy; // For 'id', 'date', 'amount', 'year', 'period', 'jurisdictionType', etc.
        };
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LICENCE_PLATE_PAYMENT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get licence plate payment by ID",
            description = "Retrieves detailed information about a specific licence plate payment by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Licence plate payment found"),
            @ApiResponse(responseCode = "404", description = "Licence plate payment not found")
    })
    public ResponseEntity<LicencePlatePaymentResponseDTO> getLicencePlatePaymentById(
            @Parameter(description = "Licence plate payment unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(licencePlatePaymentService.getLicencePlatePaymentById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LICENCE_PLATE_PAYMENT_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update licence plate payment",
            description = "Updates an existing licence plate payment record. Only provided fields will be updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Licence plate payment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Licence plate payment not found")
    })
    public ResponseEntity<LicencePlatePaymentResponseDTO> updateLicencePlatePayment(
            @Parameter(description = "Licence plate payment unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody LicencePlatePaymentDTO licencePlatePaymentDTO) {
        return ResponseEntity.ok(licencePlatePaymentService.updateLicencePlatePayment(id, licencePlatePaymentDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LICENCE_PLATE_PAYMENT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete licence plate payment",
            description = "Deletes a licence plate payment record from the system. This operation cannot be undone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Licence plate payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Licence plate payment not found")
    })
    public ResponseEntity<Void> deleteLicencePlatePayment(
            @Parameter(description = "Licence plate payment unique identifier", required = true) @PathVariable Long id) {
        licencePlatePaymentService.deleteLicencePlatePayment(id);
        return ResponseEntity.noContent().build();
    }
}
