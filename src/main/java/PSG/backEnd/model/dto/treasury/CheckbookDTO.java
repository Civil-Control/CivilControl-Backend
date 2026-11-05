package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.treasury.CheckType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos de una chequera (talonario). Restringe los números de cheque emitidos.")
public record CheckbookDTO(

        @NotBlank(groups = OnCreate.class)
        @Size(max = 100, groups = {OnCreate.class, OnUpdate.class})
        String name,

        @NotBlank(groups = OnCreate.class)
        @Size(max = 50, groups = {OnCreate.class, OnUpdate.class})
        String checkbookNumber,

        @NotNull(groups = OnCreate.class)
        Long bankAccountId,

        @NotNull(groups = OnCreate.class)
        CheckType checkType,

        @NotNull(groups = OnCreate.class)
        @Positive
        Long rangeFrom,

        @NotNull(groups = OnCreate.class)
        @Positive
        Long rangeTo
) {}
