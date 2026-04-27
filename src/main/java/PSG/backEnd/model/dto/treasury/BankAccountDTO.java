package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.treasury.BankAccountType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Datos de una cuenta bancaria.")
public record BankAccountDTO(

        @NotBlank(groups = OnCreate.class)
        @Size(max = 100, groups = {OnCreate.class, OnUpdate.class})
        String name,

        @NotBlank(groups = OnCreate.class)
        @Size(max = 100, groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        @NotNull(groups = OnCreate.class)
        BankAccountType accountType,

        @NotBlank(groups = OnCreate.class)
        @Size(max = 50, groups = {OnCreate.class, OnUpdate.class})
        String accountNumber,

        @Size(max = 22, groups = {OnCreate.class, OnUpdate.class})
        String cbu,

        @Size(max = 30, groups = {OnCreate.class, OnUpdate.class})
        String alias,

        Currency currency,

        @Schema(description = "Saldo inicial. Solo se usa en la creación; en updates es ignorado.")
        @NotNull(groups = OnCreate.class)
        @Digits(integer = 17, fraction = 2)
        BigDecimal initialBalance,

        Boolean active
) {}
