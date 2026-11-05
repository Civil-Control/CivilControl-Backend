package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.treasury.BankAccountMovementType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Movimiento manual de cuenta bancaria. Solo se aceptan los tipos INCREMENTO_MANUAL, DECREMENTO_MANUAL y AJUSTE.")
public record BankAccountMovementDTO(

        @NotNull(groups = OnCreate.class)
        Long bankAccountId,

        @NotNull(groups = OnCreate.class)
        BankAccountMovementType type,

        @Schema(description = "Para INCREMENTO/DECREMENTO MANUAL es el monto positivo. " +
                "Para AJUSTE es el SALDO OBJETIVO (delta = target - balanceActual).")
        @NotNull(groups = OnCreate.class)
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotNull(groups = OnCreate.class)
        LocalDate movementDate,

        @Size(max = 500)
        String comment
) {}
