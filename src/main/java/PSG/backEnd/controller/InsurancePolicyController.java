package PSG.backEnd.controller;

import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyFilterDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.dto.insurance.PolicyPaymentDTO;
import PSG.backEnd.model.dto.insurance.PolicyPaymentResponseDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleResponseDTO;
import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.port.IPolicyPaymentService;
import PSG.backEnd.service.port.IPolicyVehicleService;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import PSG.backEnd.model.constants.AppPermissions;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/insurance-policies")
@RequiredArgsConstructor
@Tag(name = "Insurance Policies", description = "API for managing insurance policies and their associated vehicles. Handles policy registration, coverage details, and vehicle assignments.")
public class InsurancePolicyController {

    private final IInsurancePolicyService insurancePolicyService;
    private final IPolicyVehicleService policyVehicleService;
    private final IPolicyPaymentService policyPaymentService;

    // ========== INSURANCE POLICY ENDPOINTS ==========

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new insurance policy",
            description = "Registers a new insurance policy with coverage details, effective dates, and payment terms. Validates that effective dates are in proper order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Insurance policy successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error (e.g., effectiveFrom after effectiveTo)")
    })
    public ResponseEntity<InsurancePolicyResponseDTO> createInsurancePolicy(
            @Validated(OnCreate.class) @RequestBody InsurancePolicyDTO insurancePolicyDTO) {
        InsurancePolicyResponseDTO createdPolicy = insurancePolicyService.createInsurancePolicy(insurancePolicyDTO);
        return new ResponseEntity<>(createdPolicy, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping
    @Operation(summary = "Get all insurance policies with filters",
            description = "Retrieves a paginated list of insurance policies with optional filtering by policy number, type, status, dates, and cancellation status. Supports sorting and pagination.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved insurance policy list")
    public ResponseEntity<Page<InsurancePolicyResponseDTO>> getInsurancePolicies(
            @Parameter(description = "Filter by policy number (partial match)") @RequestParam(required = false) String policyNumber,
            @Parameter(description = "Filter by term number (partial match)") @RequestParam(required = false) String termNumber,
            @Parameter(description = "Filter by policy type") @RequestParam(required = false) PolicyType policyType,
            @Parameter(description = "Filter by policy status") @RequestParam(required = false) PolicyStatus policyStatus,
            @Parameter(description = "Filter by payment frequency") @RequestParam(required = false) PaymentFrequency paymentFrequency,
            @Parameter(description = "Filter policies issued from this date (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDateFrom,
            @Parameter(description = "Filter policies issued to this date (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDateTo,
            @Parameter(description = "Filter policies with effective from date starting from this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromStart,
            @Parameter(description = "Filter policies with effective from date ending at this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromEnd,
            @Parameter(description = "Filter policies with effective to date starting from this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToStart,
            @Parameter(description = "Filter policies with effective to date ending at this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToEnd,
            @Parameter(description = "Filter by cancellation status") @RequestParam(required = false) Boolean isCancelled,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Available fields: id, policyNumber, termNumber, endorsementSecuence, policyType, policyStatus, " +
                    "paymentFrequency, sumInsured, issueDate, effectiveFrom, effectiveTo, cancellationDate, numberOfInstallments, premioTotal, premioMensual. " +
                    "Example: sortBy=effectiveFrom",
                    example = "effectiveFrom")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        InsurancePolicyFilterDTO filterDTO = new InsurancePolicyFilterDTO(policyNumber, termNumber, policyType, policyStatus, paymentFrequency, issueDateFrom, issueDateTo, effectiveFromStart, effectiveFromEnd, effectiveToStart, effectiveToEnd, isCancelled);
        Page<InsurancePolicyResponseDTO> policies = insurancePolicyService.getAllInsurancePolicies(filterDTO, pageable);
        return ResponseEntity.ok(policies);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get insurance policy by ID",
            description = "Retrieves detailed information about a specific insurance policy by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Insurance policy found"),
            @ApiResponse(responseCode = "404", description = "Insurance policy not found")
    })
    public ResponseEntity<InsurancePolicyResponseDTO> getInsurancePolicyById(
            @Parameter(description = "Insurance policy unique identifier", required = true) @PathVariable Long id) {
        InsurancePolicyResponseDTO policy = insurancePolicyService.getInsurancePolicyById(id);
        return ResponseEntity.ok(policy);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update insurance policy",
            description = "Updates an existing insurance policy. Only provided fields will be updated. Validates that effective dates remain in proper order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Insurance policy successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Insurance policy not found")
    })
    public ResponseEntity<InsurancePolicyResponseDTO> updateInsurancePolicy(
            @Parameter(description = "Insurance policy unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody InsurancePolicyDTO insurancePolicyDTO) {
        InsurancePolicyResponseDTO updatedPolicy = insurancePolicyService.updateInsurancePolicy(id, insurancePolicyDTO);
        return ResponseEntity.ok(updatedPolicy);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete insurance policy",
            description = "Deletes an insurance policy from the system. This operation cannot be undone. Policies with associated vehicles may have restrictions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Insurance policy successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Insurance policy not found"),
            @ApiResponse(responseCode = "409", description = "Insurance policy has associated records and cannot be deleted")
    })
    public ResponseEntity<Void> deleteInsurancePolicy(
            @Parameter(description = "Insurance policy unique identifier", required = true) @PathVariable Long id) {
        insurancePolicyService.deleteInsurancePolicy(id);
        return ResponseEntity.noContent().build();
    }

    // ========== POLICY VEHICLE ENDPOINTS ==========

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_WRITE + "')")
    @PostMapping("/{insurancePolicyId}/vehicles")
    @Operation(summary = "Add vehicle to insurance policy",
            description = "Associates a vehicle with an insurance policy. Automatically creates or retrieves the AutoPolicy if the policy type is AUTO. Includes coverage dates for the specific vehicle.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle successfully added to policy"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Insurance policy or vehicle not found")
    })
    public ResponseEntity<PolicyVehicleResponseDTO> addVehicleToPolicy(
            @Parameter(description = "Insurance policy unique identifier", required = true) @PathVariable Long insurancePolicyId,
            @Validated(OnCreate.class) @RequestBody PolicyVehicleDTO policyVehicleDTO) {
        PolicyVehicleResponseDTO createdVehicle = policyVehicleService.addVehicleToInsurancePolicy(insurancePolicyId, policyVehicleDTO);
        return new ResponseEntity<>(createdVehicle, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping("/{insurancePolicyId}/vehicles")
    @Operation(summary = "Get vehicles by insurance policy",
            description = "Retrieves all vehicles associated with a specific insurance policy with optional filtering by vehicle details, dates, and cancellation status.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved vehicle list for the policy")
    public ResponseEntity<Page<PolicyVehicleResponseDTO>> getVehiclesByInsurancePolicyId(
            @Parameter(description = "Insurance policy unique identifier", required = true) @PathVariable Long insurancePolicyId,
            @Parameter(description = "Filter by vehicle license plate (partial match)") @RequestParam(required = false) String licensePlate,
            @Parameter(description = "Filter by vehicle brand (partial match)") @RequestParam(required = false) String vehicleBrand,
            @Parameter(description = "Filter by vehicle model (partial match)") @RequestParam(required = false) String vehicleModel,
            @Parameter(description = "Filter vehicles with effective from date starting from this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromStart,
            @Parameter(description = "Filter vehicles with effective from date ending at this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromEnd,
            @Parameter(description = "Filter vehicles with effective to date starting from this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToStart,
            @Parameter(description = "Filter vehicles with effective to date ending at this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToEnd,
            @Parameter(description = "Filter by cancellation status") @RequestParam(required = false) Boolean isCancelled,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, effectiveFrom, effectiveTo, cancellationDate. " +
                    "For vehicle use: vehicle.licensePlate, vehicle.brand, vehicle.model, vehicle.year. " +
                    "Example: sortBy=vehicle.licensePlate",
                    example = "effectiveFrom")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PolicyVehicleResponseDTO> vehicles = policyVehicleService.getAllPolicyVehiclesByInsurancePolicy(null, insurancePolicyId, licensePlate, vehicleBrand, vehicleModel, null, effectiveFromStart, effectiveFromEnd, effectiveToStart, effectiveToEnd, isCancelled, pageable);
        return ResponseEntity.ok(vehicles);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping("/vehicles")
    @Operation(summary = "Get all policy vehicles with filters",
            description = "Retrieves a paginated list of all policy-vehicle associations with optional filtering by vehicle, policy, dates, and cancellation status.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved policy vehicle list")
    public ResponseEntity<Page<PolicyVehicleResponseDTO>> getPolicyVehicles(
            @Parameter(description = "Filter by vehicle ID") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "Filter by insurance policy ID") @RequestParam(required = false) Long insurancePolicyId,
            @Parameter(description = "Filter by vehicle license plate (partial match)") @RequestParam(required = false) String licensePlate,
            @Parameter(description = "Filter by vehicle brand (partial match)") @RequestParam(required = false) String vehicleBrand,
            @Parameter(description = "Filter by vehicle model (partial match)") @RequestParam(required = false) String vehicleModel,
            @Parameter(description = "Filter by policy number (partial match)") @RequestParam(required = false) String policyNumber,
            @Parameter(description = "Filter vehicles with effective from date starting from this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromStart,
            @Parameter(description = "Filter vehicles with effective from date ending at this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromEnd,
            @Parameter(description = "Filter vehicles with effective to date starting from this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToStart,
            @Parameter(description = "Filter vehicles with effective to date ending at this date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToEnd,
            @Parameter(description = "Filter by cancellation status") @RequestParam(required = false) Boolean isCancelled,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Direct fields: id, effectiveFrom, effectiveTo, cancellationDate. " +
                    "For vehicle use: vehicle.licensePlate, vehicle.brand, vehicle.model, vehicle.year. " +
                    "For insurance policy use: insurancePolicy.policyNumber, insurancePolicy.effectiveFrom. " +
                    "Example: sortBy=vehicle.licensePlate",
                    example = "effectiveFrom")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PolicyVehicleResponseDTO> vehicles = policyVehicleService.getAllPolicyVehiclesByInsurancePolicy(vehicleId, insurancePolicyId, licensePlate, vehicleBrand, vehicleModel, policyNumber, effectiveFromStart, effectiveFromEnd, effectiveToStart, effectiveToEnd, isCancelled, pageable);
        return ResponseEntity.ok(vehicles);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping("/vehicles/by-vehicle/{vehicleId}")
    @Operation(summary = "Get all policies for a vehicle",
            description = "Retrieves all insurance policies associated with a specific vehicle, including historical and active policies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved policy list for the vehicle"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<List<PolicyVehicleResponseDTO>> getPoliciesByVehicleId(
            @Parameter(description = "Vehicle unique identifier", required = true) @PathVariable Long vehicleId) {
        List<PolicyVehicleResponseDTO> policies = policyVehicleService.getByVehicleId(vehicleId);
        return ResponseEntity.ok(policies);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping("/vehicles/{policyVehicleId}")
    @Operation(summary = "Get policy vehicle by ID",
            description = "Retrieves detailed information about a specific policy-vehicle association by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Policy vehicle found"),
            @ApiResponse(responseCode = "404", description = "Policy vehicle not found")
    })
    public ResponseEntity<PolicyVehicleResponseDTO> getPolicyVehicleById(
            @Parameter(description = "Policy vehicle unique identifier", required = true) @PathVariable Long policyVehicleId) {
        PolicyVehicleResponseDTO vehicle = policyVehicleService.getPolicyVehicleById(policyVehicleId);
        return ResponseEntity.ok(vehicle);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_WRITE + "')")
    @PatchMapping("/vehicles/{policyVehicleId}")
    @Operation(summary = "Update policy vehicle",
            description = "Updates an existing policy-vehicle association. Can update coverage dates and cancellation status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Policy vehicle successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Policy vehicle not found")
    })
    public ResponseEntity<PolicyVehicleResponseDTO> updatePolicyVehicle(
            @Parameter(description = "Policy vehicle unique identifier", required = true) @PathVariable Long policyVehicleId,
            @Validated(OnUpdate.class) @RequestBody PolicyVehicleDTO policyVehicleDTO) {
        PolicyVehicleResponseDTO updatedVehicle = policyVehicleService.updatePolicyVehicle(policyVehicleId, policyVehicleDTO);
        return ResponseEntity.ok(updatedVehicle);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_DELETE + "')")
    @DeleteMapping("/vehicles/{policyVehicleId}")
    @Operation(summary = "Remove vehicle from policy",
            description = "Removes a vehicle from an insurance policy. This operation cannot be undone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vehicle successfully removed from policy"),
            @ApiResponse(responseCode = "404", description = "Policy vehicle not found")
    })
    public ResponseEntity<Void> deletePolicyVehicle(
            @Parameter(description = "Policy vehicle unique identifier", required = true) @PathVariable Long policyVehicleId) {
        policyVehicleService.deletePolicyVehicle(policyVehicleId);
        return ResponseEntity.noContent().build();
    }

    // ========== POLICY PAYMENT ENDPOINTS ==========

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_WRITE + "')")
    @PostMapping("/{insurancePolicyId}/payments")
    @Operation(summary = "Register a payment for an insurance policy",
            description = "Creates a new payment record for an insurance policy. The amount is pre-filled with the policy's monthly premium but can be modified.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Insurance policy not found")
    })
    public ResponseEntity<PolicyPaymentResponseDTO> createPayment(
            @Parameter(description = "Insurance policy unique identifier", required = true) @PathVariable Long insurancePolicyId,
            @Validated(OnCreate.class) @RequestBody PolicyPaymentDTO policyPaymentDTO) {
        // Override insurancePolicyId from path
        PolicyPaymentDTO dto = new PolicyPaymentDTO(
                insurancePolicyId,
                policyPaymentDTO.paymentDate(),
                policyPaymentDTO.amount(),
                policyPaymentDTO.periodFrom(),
                policyPaymentDTO.periodTo(),
                policyPaymentDTO.notes()
        );
        PolicyPaymentResponseDTO created = policyPaymentService.createPayment(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping("/{insurancePolicyId}/payments")
    @Operation(summary = "Get all payments for an insurance policy",
            description = "Retrieves all payment records associated with a specific insurance policy, ordered by period descending.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved payment list")
    public ResponseEntity<List<PolicyPaymentResponseDTO>> getPaymentsByPolicyId(
            @Parameter(description = "Insurance policy unique identifier", required = true) @PathVariable Long insurancePolicyId) {
        List<PolicyPaymentResponseDTO> payments = policyPaymentService.getPaymentsByPolicyId(insurancePolicyId);
        return ResponseEntity.ok(payments);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_READ + "')")
    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get payment by ID",
            description = "Retrieves a specific payment record by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PolicyPaymentResponseDTO> getPaymentById(
            @Parameter(description = "Payment unique identifier", required = true) @PathVariable Long paymentId) {
        PolicyPaymentResponseDTO payment = policyPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_WRITE + "')")
    @PatchMapping("/payments/{paymentId}")
    @Operation(summary = "Update a policy payment",
            description = "Updates an existing payment record. Only provided fields will be updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PolicyPaymentResponseDTO> updatePayment(
            @Parameter(description = "Payment unique identifier", required = true) @PathVariable Long paymentId,
            @Validated(OnUpdate.class) @RequestBody PolicyPaymentDTO policyPaymentDTO) {
        PolicyPaymentResponseDTO updated = policyPaymentService.updatePayment(paymentId, policyPaymentDTO);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.INSURANCE_POLICY_DELETE + "')")
    @DeleteMapping("/payments/{paymentId}")
    @Operation(summary = "Delete a policy payment",
            description = "Soft-deletes a payment record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<Void> deletePayment(
            @Parameter(description = "Payment unique identifier", required = true) @PathVariable Long paymentId) {
        policyPaymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}

