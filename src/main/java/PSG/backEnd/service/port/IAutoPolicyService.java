package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.insurance.AutoPolicyDTO;
import PSG.backEnd.model.dto.insurance.AutoPolicyResponseDTO;
import PSG.backEnd.model.entity.insurance.AutoPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAutoPolicyService {
    AutoPolicyResponseDTO createAutoPolicy(AutoPolicyDTO autoPolicyDTO);
    AutoPolicyResponseDTO getAutoPolicyById(Long id);
    AutoPolicyResponseDTO updateAutoPolicy(Long id, AutoPolicyDTO autoPolicyDTO);
    void deleteAutoPolicy(Long id);
    Page<AutoPolicyResponseDTO> getAllAutoPolicies(Long insurancePolicyId, String policyNumber, Pageable pageable);
    AutoPolicy getEntityById(Long id);
    boolean existsById(Long id);
    AutoPolicyResponseDTO getByInsurancePolicyId(Long insurancePolicyId);
}
