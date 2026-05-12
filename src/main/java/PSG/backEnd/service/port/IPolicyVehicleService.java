package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.insurance.PolicyVehicleDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleResponseDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleWithPaymentsDTO;
import PSG.backEnd.model.entity.insurance.PolicyVehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface IPolicyVehicleService {
    PolicyVehicleResponseDTO createPolicyVehicle(PolicyVehicleDTO policyVehicleDTO);
    PolicyVehicleResponseDTO getPolicyVehicleById(Long id);
    PolicyVehicleResponseDTO updatePolicyVehicle(Long id, PolicyVehicleDTO policyVehicleDTO);
    void deletePolicyVehicle(Long id);
    Page<PolicyVehicleResponseDTO> getAllPolicyVehicles(
            Long vehicleId, Long autoPolicyId, String licensePlate, String vehicleBrand,
            String vehicleModel, String policyNumber, LocalDate effectiveFromStart,
            LocalDate effectiveFromEnd, LocalDate effectiveToStart, LocalDate effectiveToEnd,
            Boolean isCancelled, Pageable pageable);
    PolicyVehicle getEntityById(Long id);
    boolean existsById(Long id);
    List<PolicyVehicleResponseDTO> getByAutoPolicyId(Long autoPolicyId);
    List<PolicyVehicleWithPaymentsDTO> getByVehicleId(Long vehicleId);
    void validateVehicleNotInPolicy(Long vehicleId, Long autoPolicyId, Long excludeId);

    PolicyVehicleResponseDTO addVehicleToInsurancePolicy(Long insurancePolicyId, PolicyVehicleDTO policyVehicleDTO);
    List<PolicyVehicleResponseDTO> getByInsurancePolicyId(Long insurancePolicyId);
    Page<PolicyVehicleResponseDTO> getAllPolicyVehiclesByInsurancePolicy(
            Long vehicleId, Long insurancePolicyId, String licensePlate, String vehicleBrand,
            String vehicleModel, String policyNumber, LocalDate effectiveFromStart,
            LocalDate effectiveFromEnd, LocalDate effectiveToStart, LocalDate effectiveToEnd,
            Boolean isCancelled, Pageable pageable);
}
