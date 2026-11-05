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
 * Materializa un item PATENTE en un {@code ServicePayment} con
 * {@link ServiceAssignment} de tipo VEHICLE. El sistema unificó pagos de patente
 * y de servicios en la misma entidad mediante asignaciones tipadas.
 */
@Component
@RequiredArgsConstructor
public class LicencePlatePaymentItemApplier implements BudgetForecastItemApplier {

    public static final String ENTITY_TYPE = "ServicePayment";

    private final IServicePaymentService servicePaymentService;

    @Override
    public BudgetForecastItemType supportedType() {
        return BudgetForecastItemType.PATENTE;
    }

    @Override
    public void validate(BudgetForecastItem item) {
        if (item.getServiceAssignment() == null) {
            throw new BudgetForecastApplyException("Item PATENTE requiere asignación de servicio (tipo vehículo).");
        }
        if (item.getServiceAssignment().getSubjectType() != SubjectType.VEHICLE) {
            throw new BudgetForecastApplyException(
                    "Item PATENTE debe referenciar una asignación de tipo VEHICLE (la actual es "
                    + item.getServiceAssignment().getSubjectType() + ").");
        }
        if (item.getExpectedAmount() == null || item.getExpectedDate() == null) {
            throw new BudgetForecastApplyException("Item PATENTE requiere monto y fecha esperados.");
        }
    }

    @Override
    public AppliedEntityRef apply(BudgetForecastItem item) {
        validate(item);
        String reference = "BFP-" + item.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        ServicePaymentDTO dto = new ServicePaymentDTO(
                item.getServiceAssignment().getId(),
                null, null,
                item.getExpectedDate(),
                item.getExpectedAmount(),
                item.getExpectedDate().getYear(),
                item.getExpectedDate().getMonthValue(),
                reference,
                "Generado desde previsión de gastos #" + item.getBudgetForecast().getId(),
                null
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
