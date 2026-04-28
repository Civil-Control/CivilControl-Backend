package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "DTO para crear o actualizar una previsión de gastos.")
public record BudgetForecastDTO(

        @Schema(description = "Nombre descriptivo de la previsión.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{budgetForecast.name.required}", groups = OnCreate.class)
        @Size(max = 200, message = "{budgetForecast.name.size}", groups = {OnCreate.class, OnUpdate.class})
        String name,

        @Schema(description = "Descripción opcional.", nullable = true)
        @Size(max = 1000, message = "{budgetForecast.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Inicio del período.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{budgetForecast.periodFrom.required}", groups = OnCreate.class)
        LocalDate periodFrom,

        @Schema(description = "Fin del período (>= periodFrom).", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{budgetForecast.periodTo.required}", groups = OnCreate.class)
        LocalDate periodTo,

        @Schema(description = "ID de plantilla origen (informativo).", nullable = true)
        @Positive
        Long createdFromTemplateId,

        @Schema(description = "Items de la previsión. Si null/vacío al crear, la previsión queda sin items.", nullable = true)
        @Valid
        List<BudgetForecastItemDTO> items
) {}
