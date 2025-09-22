package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.util.List;

public record AutoPolicyDTO(

        @NotNull(message = "Insurance policy ID is required.", groups = OnCreate.class)
        @Positive(message = "Insurance policy ID must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long insurancePolicyId,

        @NotEmpty(message = "At least one policy vehicle is required.", groups = {OnCreate.class, OnUpdate.class})
        List<PolicyVehicleDTO> policyVehicles
) {}
