package PSG.backEnd.controller;

import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyFilterDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleResponseDTO;
import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.port.IPolicyVehicleService;
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

@RestController
@RequestMapping("/api/v1/insurance-policies")
@RequiredArgsConstructor
public class InsurancePolicyController {

    private final IInsurancePolicyService insurancePolicyService;
    private final IPolicyVehicleService policyVehicleService;

    // ========== INSURANCE POLICY ENDPOINTS ==========

    @PostMapping
    public ResponseEntity<InsurancePolicyResponseDTO> createInsurancePolicy(
            @Validated(OnCreate.class) @RequestBody InsurancePolicyDTO insurancePolicyDTO) {
        InsurancePolicyResponseDTO createdPolicy = insurancePolicyService.createInsurancePolicy(insurancePolicyDTO);
        return new ResponseEntity<>(createdPolicy, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<InsurancePolicyResponseDTO>> getInsurancePolicies(
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) String termNumber,
            @RequestParam(required = false) PolicyType policyType,
            @RequestParam(required = false) PolicyStatus policyStatus,
            @RequestParam(required = false) PaymentFrequency paymentFrequency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToEnd,
            @RequestParam(required = false) Boolean isCancelled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        InsurancePolicyFilterDTO filterDTO = new InsurancePolicyFilterDTO(
                policyNumber, termNumber, policyType, policyStatus, paymentFrequency,
                issueDateFrom, issueDateTo, effectiveFromStart, effectiveFromEnd,
                effectiveToStart, effectiveToEnd, isCancelled
        );

        Page<InsurancePolicyResponseDTO> policies = insurancePolicyService.getAllInsurancePolicies(filterDTO, pageable);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsurancePolicyResponseDTO> getInsurancePolicyById(@PathVariable Long id) {
        InsurancePolicyResponseDTO policy = insurancePolicyService.getInsurancePolicyById(id);
        return ResponseEntity.ok(policy);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InsurancePolicyResponseDTO> updateInsurancePolicy(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody InsurancePolicyDTO insurancePolicyDTO) {
        InsurancePolicyResponseDTO updatedPolicy = insurancePolicyService.updateInsurancePolicy(id, insurancePolicyDTO);
        return ResponseEntity.ok(updatedPolicy);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInsurancePolicy(@PathVariable Long id) {
        insurancePolicyService.deleteInsurancePolicy(id);
        return ResponseEntity.noContent().build();
    }

    // ========== POLICY VEHICLE ENDPOINTS (GESTIONADOS A TRAVÉS DE INSURANCE POLICY) ==========

    @PostMapping("/{insurancePolicyId}/vehicles")
    public ResponseEntity<PolicyVehicleResponseDTO> addVehicleToPolicy(
            @PathVariable Long insurancePolicyId,
            @Validated(OnCreate.class) @RequestBody PolicyVehicleDTO policyVehicleDTO) {
        // El servicio se encargará de obtener o crear automáticamente el AutoPolicy
        PolicyVehicleResponseDTO createdVehicle = policyVehicleService.addVehicleToInsurancePolicy(insurancePolicyId, policyVehicleDTO);
        return new ResponseEntity<>(createdVehicle, HttpStatus.CREATED);
    }

    @GetMapping("/{insurancePolicyId}/vehicles")
    public ResponseEntity<List<PolicyVehicleResponseDTO>> getVehiclesByInsurancePolicyId(@PathVariable Long insurancePolicyId) {
        List<PolicyVehicleResponseDTO> vehicles = policyVehicleService.getByInsurancePolicyId(insurancePolicyId);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/vehicles")
    public ResponseEntity<Page<PolicyVehicleResponseDTO>> getPolicyVehicles(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long insurancePolicyId,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String vehicleBrand,
            @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFromEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveToEnd,
            @RequestParam(required = false) Boolean isCancelled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PolicyVehicleResponseDTO> vehicles = policyVehicleService.getAllPolicyVehiclesByInsurancePolicy(
                vehicleId, insurancePolicyId, licensePlate, vehicleBrand, vehicleModel,
                policyNumber, effectiveFromStart, effectiveFromEnd, effectiveToStart,
                effectiveToEnd, isCancelled, pageable);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/vehicles/by-vehicle/{vehicleId}")
    public ResponseEntity<List<PolicyVehicleResponseDTO>> getPoliciesByVehicleId(@PathVariable Long vehicleId) {
        List<PolicyVehicleResponseDTO> policies = policyVehicleService.getByVehicleId(vehicleId);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/vehicles/{policyVehicleId}")
    public ResponseEntity<PolicyVehicleResponseDTO> getPolicyVehicleById(@PathVariable Long policyVehicleId) {
        PolicyVehicleResponseDTO vehicle = policyVehicleService.getPolicyVehicleById(policyVehicleId);
        return ResponseEntity.ok(vehicle);
    }

    @PatchMapping("/vehicles/{policyVehicleId}")
    public ResponseEntity<PolicyVehicleResponseDTO> updatePolicyVehicle(
            @PathVariable Long policyVehicleId,
            @Validated(OnUpdate.class) @RequestBody PolicyVehicleDTO policyVehicleDTO) {
        PolicyVehicleResponseDTO updatedVehicle = policyVehicleService.updatePolicyVehicle(policyVehicleId, policyVehicleDTO);
        return ResponseEntity.ok(updatedVehicle);
    }

    @DeleteMapping("/vehicles/{policyVehicleId}")
    public ResponseEntity<Void> removePolicyVehicle(@PathVariable Long policyVehicleId) {
        policyVehicleService.deletePolicyVehicle(policyVehicleId);
        return ResponseEntity.noContent().build();
    }
}
