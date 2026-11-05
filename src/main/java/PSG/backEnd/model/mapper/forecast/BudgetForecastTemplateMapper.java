package PSG.backEnd.model.mapper.forecast;

import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateItemDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateResponseDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateResponseDTO.BudgetForecastTemplateItemResponseDTO;
import PSG.backEnd.model.entity.forecast.BudgetForecastTemplate;
import PSG.backEnd.model.entity.forecast.BudgetForecastTemplateItem;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BudgetForecastTemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BudgetForecastTemplate toEntity(BudgetForecastTemplateDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void partialUpdate(BudgetForecastTemplateDTO dto, @MappingTarget BudgetForecastTemplate template);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "template", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "serviceAssignment", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "stock", ignore = true)
    BudgetForecastTemplateItem toItemEntity(BudgetForecastTemplateItemDTO dto);

    default BudgetForecastTemplateItemResponseDTO toItemResponse(BudgetForecastTemplateItem item) {
        if (item == null) return null;
        return new BudgetForecastTemplateItemResponseDTO(
                item.getId(),
                item.getRowOrder(),
                item.getItemType(),
                item.getDescription(),
                item.getDayOffset(),
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
                item.getStockQuantity()
        );
    }

    default String buildAssignmentLabel(BudgetForecastTemplateItem item) {
        if (item.getServiceAssignment() == null) return null;
        var sa = item.getServiceAssignment();
        StringBuilder sb = new StringBuilder();
        if (sa.getServiceSupplier() != null && sa.getServiceSupplier().getSupplier() != null)
            sb.append(sa.getServiceSupplier().getSupplier().getLegalName());
        if (sa.getServiceType() != null) sb.append(" / ").append(sa.getServiceType().name());
        return sb.toString();
    }

    default BudgetForecastTemplateResponseDTO toResponse(BudgetForecastTemplate template) {
        if (template == null) return null;
        List<BudgetForecastTemplateItemResponseDTO> itemDtos = template.getItems().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getRowOrder() == null ? 0 : a.getRowOrder(),
                        b.getRowOrder() == null ? 0 : b.getRowOrder()))
                .map(this::toItemResponse)
                .toList();
        BigDecimal expectedTotal = template.getItems().stream()
                .map(BudgetForecastTemplateItem::getExpectedAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BudgetForecastTemplateResponseDTO(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getDefaultPeriodDays(),
                template.getActive(),
                template.getItems().size(),
                expectedTotal,
                template.getCreatedAt(),
                template.getUpdatedAt(),
                itemDtos
        );
    }
}
