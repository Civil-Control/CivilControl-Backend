package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.treasury.CashBoxMovementType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Movimiento manual de caja. Solo se aceptan los tipos INCREMENTO_MANUAL, DECREMENTO_MANUAL y AJUSTE; " +
        "los movimientos PAGO_EMITIDO y TRANSFERENCIA_INTERNA son generados internamente por el sistema.")
public record CashBoxMovementDTO(

        @NotNull(groups = OnCreate.class)
        Long cashBoxId,

        @NotNull(groups = OnCreate.class)
        CashBoxMovementType type,

        @Schema(description = "Para INCREMENTO/DECREMENTO MANUAL es el monto positivo del movimiento. " +
                "Para AJUSTE es el SALDO OBJETIVO; el sistema calcula el delta como (target - balanceActual).")
        @NotNull(groups = OnCreate.class)
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotNull(groups = OnCreate.class)
        LocalDate movementDate,

        @Size(max = 500)
        String comment
) {}
