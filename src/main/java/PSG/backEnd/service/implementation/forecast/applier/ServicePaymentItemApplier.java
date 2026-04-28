package PSG.backEnd.service.implementation.forecast.applier;

import PSG.backEnd.exception.forecast.BudgetForecastApplyException;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import PSG.backEnd.service.port.IServicePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Materializa un item SERVICIO en un {@code ServicePayment} con
 * {@link ServiceAssignment} de tipo BUILDING.
 */
@Component
@RequiredArgsConstructor
public class ServicePaymentItemApplier implements BudgetForecastItemApplier {

    public static final String ENTITY_TYPE = "ServicePayment";

    private final IServicePaymentService servicePaymentService;

    @Override
    public BudgetForecastItemType supportedType() {
        return BudgetForecastItemType.SERVICIO;
    }

    @Override
    public void validate(BudgetForecastItem item) {
        if (item.getServiceAssignment() == null) {
            throw new BudgetForecastApplyException("Item SERVICIO requiere asignación de servicio.");
        }
        if (item.getServiceAssignment().getSubjectType() != SubjectType.BUILDING) {
            throw new BudgetForecastApplyException(
                    "Item SERVICIO debe referenciar una asignación de tipo BUILDING (la actual es "
                    + item.getServiceAssignment().getSubjectType() + ").");
        }
        if (item.getExpectedAmount() == null || item.getExpectedDate() == null) {
            throw new BudgetForecastApplyException("Item SERVICIO requiere monto y fecha esperados.");
        }
    }

    @Override
    public AppliedEntityRef apply(BudgetForecastItem item) {
        validate(item);
        // Reference number único derivado del item para evitar conflictos al re-aplicar
        String reference = "BF-" + item.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        ServicePaymentDTO dto = new ServicePaymentDTO(
                item.getServiceAssignment().getId(),
                null,                          // projectAreaId — auto-resolved por el service
                null,                          // projectAreaTaskId
                item.getExpectedDate(),
                item.getExpectedAmount(),
                item.getExpectedDate().getYear(),
                item.getExpectedDate().getMonthValue(),
                reference,
                "Generado desde previsión de gastos #" + item.getBudgetForecast().getId(),
                null                           // paymentMethod
        );
        ServicePaymentResponseDTO created = servicePaymentService.createServicePayment(dto);
        return AppliedEntityRef.of(ENTITY_TYPE, created.id());
    }

    @Override
    public void revert(BudgetForecastItem item) {
        if (item.getAppliedEntityId() == null) return;
        servicePaymentService.deleteServicePayment(item.getAppliedEntityId());
    }
}
