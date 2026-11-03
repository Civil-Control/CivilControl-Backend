package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Vehicle to insure in the renewed policy. Dates default to the policy's dates when omitted.")
public record RenewalVehicleDTO(
        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        Long vehicleId,

        @DecimalMin(value = "0.0", inclusive = false, message = "{insurancePolicy.sumInsured.positive}", groups = OnCreate.class)
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = OnCreate.class)
        BigDecimal sumInsured,

        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        @Min(value = 1, message = "{insurancePolicy.numberOfInstallments.positive}", groups = OnCreate.class)
        @Max(value = 12, message = "{insurancePolicy.numberOfInstallments.positive}", groups = OnCreate.class)
        Integer numberOfInstallments,

        @DecimalMin(value = "0.0", inclusive = true, message = "{validation.pattern}", groups = OnCreate.class)
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = OnCreate.class)
        BigDecimal premioTotal,

        @DecimalMin(value = "0.0", inclusive = true, message = "{validation.pattern}", groups = OnCreate.class)
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = OnCreate.class)
        BigDecimal premioMensual
) {}
