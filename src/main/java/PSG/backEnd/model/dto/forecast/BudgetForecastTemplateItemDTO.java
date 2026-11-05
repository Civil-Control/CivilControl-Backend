package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Item dentro de una plantilla de previsión.")
public record BudgetForecastTemplateItemDTO(
        Long id,
        Integer rowOrder,

        @NotNull(message = "{budgetForecast.item.itemType.required}", groups = OnCreate.class)
        BudgetForecastItemType itemType,

        @NotBlank(message = "{budgetForecast.item.description.required}", groups = OnCreate.class)
        @Size(max = 500, groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Días desde periodFrom para calcular expectedDate al instanciar.")
        @NotNull(message = "{budgetForecast.template.dayOffset.required}", groups = OnCreate.class)
        @Min(value = 0, message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
        Integer dayOffset,

        @NotNull(message = "{budgetForecast.item.expectedAmount.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 17, fraction = 2, groups = {OnCreate.class, OnUpdate.class})
        BigDecimal expectedAmount,

        @Positive Long employeeId,
        @Positive Long supplierId,
        @Positive Long serviceAssignmentId,
        @Positive Long vehicleId,
        @Positive Long stockId,

        @DecimalMin(value = "0.0001", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 15, fraction = 4, groups = {OnCreate.class, OnUpdate.class})
        BigDecimal stockQuantity
) {}
