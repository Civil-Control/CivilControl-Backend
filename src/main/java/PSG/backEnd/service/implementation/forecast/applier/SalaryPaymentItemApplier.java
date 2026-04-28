package PSG.backEnd.service.implementation.forecast.applier;

import PSG.backEnd.exception.forecast.BudgetForecastApplyException;
import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import PSG.backEnd.service.port.ISalaryPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Materializa un item SALARIO en un {@code SalaryPayment}.
 * Asume frecuencia MENSUAL por defecto (la previsión no porta esa info granular).
 * Si el negocio necesita otra frecuencia, se puede extender el item con un campo dedicado.
 */
@Component
@RequiredArgsConstructor
public class SalaryPaymentItemApplier implements BudgetForecastItemApplier {

    public static final String ENTITY_TYPE = "SalaryPayment";

    private final ISalaryPaymentService salaryPaymentService;

    @Override
    public BudgetForecastItemType supportedType() {
        return BudgetForecastItemType.SALARIO;
    }

    @Override
    public void validate(BudgetForecastItem item) {
        if (item.getEmployee() == null) {
            throw new BudgetForecastApplyException("Item SALARIO requiere empleado.");
        }
        if (item.getExpectedAmount() == null) {
            throw new BudgetForecastApplyException("Item SALARIO requiere monto esperado.");
        }
        if (item.getExpectedDate() == null) {
            throw new BudgetForecastApplyException("Item SALARIO requiere fecha esperada.");
        }
    }

    @Override
    public AppliedEntityRef apply(BudgetForecastItem item) {
        validate(item);
        SalaryPaymentDTO dto = new SalaryPaymentDTO(
                item.getEmployee().getId(),
                SalaryFrecuency.MENSUAL,
                item.getExpectedDate(),
                item.getExpectedAmount(),
                null, // paymentMethod
                item.getEmployee().getProjectArea() != null ? item.getEmployee().getProjectArea().getId() : null,
                item.getEmployee().getProjectAreaTask() != null ? item.getEmployee().getProjectAreaTask().getId() : null,
                null, // transactionalDocumentId
                null, // ivaPercentage
                null  // documentSortOrder
        );
        SalaryPaymentResponseDTO created = salaryPaymentService.createSalaryPayment(dto);
        return AppliedEntityRef.of(ENTITY_TYPE, created.id());
    }

    @Override
    public void revert(BudgetForecastItem item) {
        if (item.getAppliedEntityId() == null) return;
        salaryPaymentService.deleteSalaryPayment(item.getAppliedEntityId());
    }
}
