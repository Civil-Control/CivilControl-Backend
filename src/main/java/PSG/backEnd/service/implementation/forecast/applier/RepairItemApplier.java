package PSG.backEnd.service.implementation.forecast.applier;

import PSG.backEnd.exception.forecast.BudgetForecastApplyException;
import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairItemDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import PSG.backEnd.model.enums.vehicle.RepairItemType;
import PSG.backEnd.service.port.IRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Materializa un item REPARACION en un {@code Repair} con un único {@code RepairItem}
 * sintético (tipo MANO_DE_OBRA) que carga el monto previsto.
 */
@Component
@RequiredArgsConstructor
public class RepairItemApplier implements BudgetForecastItemApplier {

    public static final String ENTITY_TYPE = "Repair";

    private final IRepairService repairService;

    @Override
    public BudgetForecastItemType supportedType() {
        return BudgetForecastItemType.REPARACION;
    }

    @Override
    public void validate(BudgetForecastItem item) {
        if (item.getVehicle() == null) {
            throw new BudgetForecastApplyException("Item REPARACION requiere vehículo.");
        }
        if (item.getExpectedAmount() == null || item.getExpectedDate() == null) {
            throw new BudgetForecastApplyException("Item REPARACION requiere monto y fecha esperados.");
        }
    }

    @Override
    public AppliedEntityRef apply(BudgetForecastItem item) {
        validate(item);
        RepairItemDTO syntheticItem = new RepairItemDTO(
                null,
                RepairItemType.MANO_DE_OBRA,
                item.getDescription(),
                item.getExpectedAmount(),
                BigDecimal.ONE,
                new BigDecimal("21.00"),
                null
        );
        RepairDTO dto = new RepairDTO(
                item.getExpectedDate(),
                item.getVehicle().getId(),
                "Generado desde previsión #" + item.getBudgetForecast().getId() + ": " + item.getDescription(),
                null,
                item.getSupplier() != null ? item.getSupplier().getId() : null,
                List.of(syntheticItem)
        );
        RepairResponseDTO created = repairService.createRepair(dto);
        return AppliedEntityRef.of(ENTITY_TYPE, created.id());
    }

    @Override
    public void revert(BudgetForecastItem item) {
        if (item.getAppliedEntityId() == null) return;
        repairService.deleteRepair(item.getAppliedEntityId());
    }
}
