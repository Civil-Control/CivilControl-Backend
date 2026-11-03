package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.insurance.InsurancePolicyRenewalDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyRenewalDraftDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;

import java.util.Optional;

public interface IInsurancePolicyRenewalService {

    /** Policy with the latest end date (any status, not deleted): the default base of a renewal. */
    Optional<InsurancePolicyResponseDTO> findDefaultBasePolicy();

    /** Builds the pre-filled data to renew {@code sourcePolicyId}. Persists nothing. */
    InsurancePolicyRenewalDraftDTO buildDraft(Long sourcePolicyId);

    /** Creates the renewed policy and its vehicles atomically. */
    InsurancePolicyResponseDTO renew(Long sourcePolicyId, InsurancePolicyRenewalDTO renewalDTO);
}
