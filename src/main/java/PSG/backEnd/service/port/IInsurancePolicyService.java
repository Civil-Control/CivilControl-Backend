package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyFilterDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IInsurancePolicyService {
    InsurancePolicyResponseDTO createInsurancePolicy(InsurancePolicyDTO insurancePolicyDTO);
    InsurancePolicyResponseDTO getInsurancePolicyById(Long id);
    InsurancePolicyResponseDTO updateInsurancePolicy(Long id, InsurancePolicyDTO insurancePolicyDTO);
    void deleteInsurancePolicy(Long id);
    Page<InsurancePolicyResponseDTO> getAllInsurancePolicies(InsurancePolicyFilterDTO filterDTO, Pageable pageable);
    InsurancePolicy getEntityById(Long id);
    boolean existsById(Long id);
    void validatePolicyNumber(String policyNumber, Long excludeId);
}
