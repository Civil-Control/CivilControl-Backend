package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.util.List;

public record AutoPolicyDTO(

        @NotNull(message = "{insurancePolicy.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long insurancePolicyId,

        @NotEmpty(message = "{insurancePolicy.policyVehicles.required}", groups = {OnCreate.class, OnUpdate.class})
        List<PolicyVehicleDTO> policyVehicles
) {}
