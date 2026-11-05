package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item de una previsión. Las referencias (employee/supplier/serviceAssignment/vehicle/stock)
 * se requieren según itemType — la validación cruzada se hace en BudgetForecastItemValidator.
 */
@Schema(description = "Item de una previsión de gastos.")
public record BudgetForecastItemDTO(

        @Schema(description = "ID del item. Null al crear; requerido al actualizar items existentes en upsert.", nullable = true)
        Long id,

        @Schema(description = "Orden de fila dentro de la previsión. Asignado por el server si es null.", nullable = true)
        Integer rowOrder,

        @Schema(description = "Tipo de gasto previsto.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{budgetForecast.item.itemType.required}", groups = OnCreate.class)
        BudgetForecastItemType itemType,

        @Schema(description = "Descripción del gasto.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
        @NotBlank(message = "{budgetForecast.item.description.required}", groups = OnCreate.class)
        @Size(max = 500, message = "{budgetForecast.item.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Fecha estimada del gasto.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{budgetForecast.item.expectedDate.required}", groups = OnCreate.class)
        LocalDate expectedDate,

        @Schema(description = "Monto estimado total. Debe ser > 0.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{budgetForecast.item.expectedAmount.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{budgetForecast.item.expectedAmount.min}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 17, fraction = 2, message = "{validation.digits}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal expectedAmount,

        @Schema(description = "ID empleado (requerido si itemType = SALARIO).", nullable = true)
        @Positive(groups = {OnCreate.class, OnUpdate.class})
        Long employeeId,

        @Schema(description = "ID proveedor (requerido si itemType = REPARACION o COMPRA_STOCK).", nullable = true)
        @Positive(groups = {OnCreate.class, OnUpdate.class})
        Long supplierId,

        @Schema(description = "ID asignación de servicio (requerido si itemType = SERVICIO o PATENTE).", nullable = true)
        @Positive(groups = {OnCreate.class, OnUpdate.class})
        Long serviceAssignmentId,

        @Schema(description = "ID vehículo (requerido si itemType = REPARACION).", nullable = true)
        @Positive(groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "ID stock (requerido si itemType = COMPRA_STOCK).", nullable = true)
        @Positive(groups = {OnCreate.class, OnUpdate.class})
        Long stockId,

        @Schema(description = "Cantidad de unidades de stock (requerido si itemType = COMPRA_STOCK).", nullable = true)
        @DecimalMin(value = "0.0001", message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 15, fraction = 4, message = "{validation.digits}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal stockQuantity
) {}
