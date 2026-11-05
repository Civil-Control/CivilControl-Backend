package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Renewal of an insurance policy: the data of the new policy plus the vehicles it covers.")
public record InsurancePolicyRenewalDTO(
        @Valid
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        InsurancePolicyDTO policy,

        @Valid
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        List<RenewalVehicleDTO> vehicles
) {}
