package PSG.backEnd.model.dto.insurance;

import java.util.List;

public record AutoPolicyResponseDTO(
        Long id,
        Long insurancePolicyId,
        List<PolicyVehicleResponseDTO> policyVehicles
) {}
