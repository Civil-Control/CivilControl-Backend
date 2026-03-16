package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyVehicleDTO(

        @NotNull(message = "{validation.required}", groups = OnCreate.class)
        Long vehicleId,

        @DecimalMin(value = "0.0", inclusive = false, message = "{insurancePolicy.sumInsured.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal sumInsured,

        @NotNull(message = "{insurancePolicy.effectiveFrom.required}", groups = OnCreate.class)
        LocalDate effectiveFrom,

        @NotNull(message = "{insurancePolicy.effectiveTo.required}", groups = OnCreate.class)
        LocalDate effectiveTo,

        LocalDate cancellationDate,

        @Min(value = 1, message = "{insurancePolicy.numberOfInstallments.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 12, message = "{insurancePolicy.numberOfInstallments.positive}", groups = {OnCreate.class, OnUpdate.class})
        Integer numberOfInstallments,

        @DecimalMin(value = "0.0", inclusive = true, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal premioTotal,

        @DecimalMin(value = "0.0", inclusive = true, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 13, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal premioMensual
) {}
