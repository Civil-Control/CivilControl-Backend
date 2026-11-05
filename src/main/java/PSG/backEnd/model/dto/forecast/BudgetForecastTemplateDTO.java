package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "DTO para crear o actualizar una plantilla de previsión.")
public record BudgetForecastTemplateDTO(
        @NotBlank(message = "{budgetForecast.template.name.required}", groups = OnCreate.class)
        @Size(max = 200, groups = {OnCreate.class, OnUpdate.class})
        String name,

        @Size(max = 1000, groups = {OnCreate.class, OnUpdate.class})
        String description,

        @NotNull(message = "{budgetForecast.template.defaultPeriodDays.required}", groups = OnCreate.class)
        @Min(value = 1, message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Integer defaultPeriodDays,

        Boolean active,

        @Valid
        List<BudgetForecastTemplateItemDTO> items
) {}
