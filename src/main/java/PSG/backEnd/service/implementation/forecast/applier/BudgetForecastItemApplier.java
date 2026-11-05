package PSG.backEnd.service.implementation.forecast.applier;

import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;

/**
 * Strategy que materializa un {@link BudgetForecastItem} en un registro real
 * (SalaryPayment, ServicePayment, Repair o StockPurchase). Cada implementación
 * declara el tipo que soporta y la lógica de creación/eliminación.
 *
 * <p>Las implementaciones delegan en los services existentes ({@code I*Service})
 * para mantener una sola fuente de verdad para validaciones y reglas de negocio.</p>
 */
public interface BudgetForecastItemApplier {

    /** Tipo de item que esta implementación soporta. */
    BudgetForecastItemType supportedType();

    /**
     * Valida pre-condiciones del item (campos requeridos, FKs, reglas de negocio).
     * Lanza {@code BudgetForecastApplyException} si la materialización no es viable.
     */
    void validate(BudgetForecastItem item);

    /**
     * Materializa el item creando el registro real y devuelve la referencia
     * (entity type + id) que se persiste en el item para trazabilidad.
     */
    AppliedEntityRef apply(BudgetForecastItem item);

    /**
     * Reverte la materialización eliminando el registro real correspondiente.
     * Si el applier no soporta revert, debe lanzar {@code BudgetForecastApplyException}.
     */
    void revert(BudgetForecastItem item);
}
