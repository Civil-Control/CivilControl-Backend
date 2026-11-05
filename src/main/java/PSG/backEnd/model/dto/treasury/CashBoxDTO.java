package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Datos de una caja en efectivo (tesorería). El saldo no se establece por edición directa: " +
        "en alta se usa el `initialBalance` que generará un movimiento INCREMENTO_MANUAL inicial; " +
        "en updates posteriores se ignora.")
public record CashBoxDTO(

        @NotBlank(message = "{treasury.cashBox.name.required}", groups = OnCreate.class)
        @Size(max = 100, groups = {OnCreate.class, OnUpdate.class})
        String name,

        @Size(max = 500, groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Saldo inicial. Solo se usa en la creación; en updates es ignorado.")
        @NotNull(message = "{treasury.cashBox.initialBalance.required}", groups = OnCreate.class)
        @Digits(integer = 17, fraction = 2)
        BigDecimal initialBalance,

        Boolean active
) {}
