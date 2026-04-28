package PSG.backEnd.service.implementation.forecast;

import PSG.backEnd.exception.forecast.BudgetForecastNotValidException;
import PSG.backEnd.model.dto.forecast.BudgetForecastItemDTO;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import org.springframework.stereotype.Component;

/**
 * Valida que un {@link BudgetForecastItemDTO} cumpla los requisitos cruzados de campos
 * según su {@code itemType}. Las anotaciones JSR-380 cubren los chequeos uniformes;
 * este componente cubre las reglas condicionales por tipo.
 */
@Component
public class BudgetForecastItemValidator {

    public void validate(BudgetForecastItemDTO dto) {
        if (dto == null || dto.itemType() == null) {
            throw new BudgetForecastNotValidException("itemType es requerido.");
        }
        switch (dto.itemType()) {
            case SALARIO -> requireField("employeeId", dto.employeeId(), "SALARIO");
            case SERVICIO -> requireField("serviceAssignmentId", dto.serviceAssignmentId(), "SERVICIO");
            case PATENTE -> requireField("serviceAssignmentId", dto.serviceAssignmentId(), "PATENTE");
            case REPARACION -> requireField("vehicleId", dto.vehicleId(), "REPARACION");
            case COMPRA_STOCK -> {
                requireField("stockId", dto.stockId(), "COMPRA_STOCK");
                if (dto.stockQuantity() == null || dto.stockQuantity().signum() <= 0) {
                    throw new BudgetForecastNotValidException("Item COMPRA_STOCK requiere stockQuantity > 0.");
                }
            }
            case OTRO -> { /* sin requerimientos cruzados */ }
        }
    }

    private void requireField(String fieldName, Object value, String typeName) {
        if (value == null) {
            throw new BudgetForecastNotValidException(
                    "Item de tipo " + typeName + " requiere el campo " + fieldName + ".");
        }
    }

    public boolean isApplicable(BudgetForecastItemType type) {
        return type != BudgetForecastItemType.OTRO;
    }
}
