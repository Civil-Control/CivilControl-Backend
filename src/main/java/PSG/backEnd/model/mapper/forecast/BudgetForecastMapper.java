package PSG.backEnd.model.mapper.forecast;

import PSG.backEnd.model.dto.forecast.BudgetForecastDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastItemDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastItemResponseDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastResponseDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastSummaryDTO;
import PSG.backEnd.model.entity.forecast.BudgetForecast;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper para BudgetForecast y sus items. La construcción de DTOs enriquecidos
 * (con conteos y etiquetas de entidades referenciadas) se delega a métodos
 * default que el servicio invoca tras hidratar la entidad.
 */
@Mapper(componentModel = "spring")
public interface BudgetForecastMapper {

    // ── Entity ← DTO (las relaciones se setean en el servicio) ──

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "appliedAmount", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "confirmedByUserId", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedByUserId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BudgetForecast toEntity(BudgetForecastDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "appliedAmount", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "confirmedByUserId", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedByUserId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void partialUpdate(BudgetForecastDTO dto, @MappingTarget BudgetForecast forecast);

    // ── Item entity ← item DTO (FKs se resuelven en el servicio) ──

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "budgetForecast", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "serviceAssignment", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "applicationStatus", ignore = true)
    @Mapping(target = "appliedEntityType", ignore = true)
    @Mapping(target = "appliedEntityId", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "appliedByUserId", ignore = true)
    @Mapping(target = "skipReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BudgetForecastItem toItemEntity(BudgetForecastItemDTO dto);

    // ── Item entity → response DTO ──

    default BudgetForecastItemResponseDTO toItemResponse(BudgetForecastItem item) {
        if (item == null) return null;
        return new BudgetForecastItemResponseDTO(
                item.getId(),
                item.getBudgetForecast() != null ? item.getBudgetForecast().getId() : null,
                item.getRowOrder(),
                item.getItemType(),
                item.getDescription(),
                item.getExpectedDate(),
                item.getExpectedAmount(),
                item.getEmployee() != null ? item.getEmployee().getId() : null,
                item.getEmployee() != null ? (item.getEmployee().getLastName() + " " + item.getEmployee().getName()) : null,
                item.getSupplier() != null ? item.getSupplier().getId() : null,
                item.getSupplier() != null ? item.getSupplier().getLegalName() : null,
                item.getServiceAssignment() != null ? item.getServiceAssignment().getId() : null,
                buildAssignmentLabel(item),
                item.getVehicle() != null ? item.getVehicle().getId() : null,
                item.getVehicle() != null ? item.getVehicle().getLicensePlate() : null,
                item.getStock() != null ? item.getStock().getId() : null,
                item.getStock() != null ? item.getStock().getName() : null,
                item.getStockQuantity(),
                item.getApplicationStatus(),
                item.getAppliedEntityType(),
                item.getAppliedEntityId(),
                item.getAppliedAt(),
                item.getAppliedByUserId(),
                item.getSkipReason(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    default String buildAssignmentLabel(BudgetForecastItem item) {
        if (item.getServiceAssignment() == null) return null;
        var sa = item.getServiceAssignment();
        StringBuilder sb = new StringBuilder();
        if (sa.getServiceSupplier() != null && sa.getServiceSupplier().getSupplier() != null)
            sb.append(sa.getServiceSupplier().getSupplier().getLegalName());
        if (sa.getServiceType() != null) sb.append(" / ").append(sa.getServiceType().name());
        if (sa.getVehicle() != null) sb.append(" [").append(sa.getVehicle().getLicensePlate()).append(']');
        else if (sa.getBuilding() != null && sa.getBuilding().getName() != null) sb.append(" [").append(sa.getBuilding().getName()).append(']');
        return sb.toString();
    }

    // ── Forecast → response (enriquecido) ──

    default BudgetForecastResponseDTO toResponse(BudgetForecast forecast) {
        if (forecast == null) return null;
        List<BudgetForecastItemResponseDTO> itemDtos = forecast.getItems().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getRowOrder() == null ? 0 : a.getRowOrder(),
                        b.getRowOrder() == null ? 0 : b.getRowOrder()))
                .map(this::toItemResponse)
                .toList();
        int total = forecast.getItems().size();
        int applied = (int) forecast.getItems().stream()
                .filter(i -> i.getApplicationStatus() == BudgetForecastItemApplicationStatus.APLICADO).count();
        int skipped = (int) forecast.getItems().stream()
                .filter(i -> i.getApplicationStatus() == BudgetForecastItemApplicationStatus.OMITIDO).count();
        int pending = total - applied - skipped;
        BigDecimal totalAmount = forecast.getTotalAmount() != null ? forecast.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal appliedAmount = forecast.getAppliedAmount() != null ? forecast.getAppliedAmount() : BigDecimal.ZERO;
        return new BudgetForecastResponseDTO(
                forecast.getId(),
                forecast.getName(),
                forecast.getDescription(),
                forecast.getPeriodFrom(),
                forecast.getPeriodTo(),
                forecast.getStatus(),
                totalAmount,
                appliedAmount,
                totalAmount.subtract(appliedAmount),
                total, applied, skipped, pending,
                forecast.getCreatedFromTemplateId(),
                forecast.getConfirmedAt(),
                forecast.getConfirmedByUserId(),
                forecast.getClosedAt(),
                forecast.getClosedByUserId(),
                forecast.getCreatedAt(),
                forecast.getUpdatedAt(),
                itemDtos
        );
    }

    default BudgetForecastSummaryDTO toSummary(BudgetForecast forecast) {
        if (forecast == null) return null;
        BigDecimal totalAmount = forecast.getTotalAmount() != null ? forecast.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal appliedAmount = forecast.getAppliedAmount() != null ? forecast.getAppliedAmount() : BigDecimal.ZERO;
        int total = forecast.getItems().size();
        int applied = (int) forecast.getItems().stream()
                .filter(i -> i.getApplicationStatus() == BudgetForecastItemApplicationStatus.APLICADO).count();
        return new BudgetForecastSummaryDTO(
                forecast.getId(),
                forecast.getName(),
                forecast.getPeriodFrom(),
                forecast.getPeriodTo(),
                forecast.getStatus(),
                totalAmount,
                appliedAmount,
                totalAmount.subtract(appliedAmount),
                total, applied
        );
    }
}
