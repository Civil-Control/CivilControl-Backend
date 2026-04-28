package PSG.backEnd.model.dto.forecast;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Instancia una nueva previsión a partir de una plantilla.")
public record BudgetForecastInstantiateDTO(
        @NotNull @Positive Long templateId,

        @NotNull LocalDate periodFrom,

        /** Si null, se calcula como periodFrom + template.defaultPeriodDays - 1. */
        LocalDate periodTo,

        @Size(max = 200) String nameOverride
) {}
