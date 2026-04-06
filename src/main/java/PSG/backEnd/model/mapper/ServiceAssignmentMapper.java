package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentResponseDTO;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServiceAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceSupplier", ignore = true)
    @Mapping(target = "building", ignore = true)
    @Mapping(target = "paymentLocation", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    ServiceAssignment toEntity(ServiceAssignmentDTO dto);

    @Mapping(target = "serviceSupplierId", expression = "java(entity.getServiceSupplier() != null ? entity.getServiceSupplier().getId() : null)")
    @Mapping(target = "supplierName", expression = "java(entity.getServiceSupplier() != null && entity.getServiceSupplier().getSupplier() != null ? entity.getServiceSupplier().getSupplier().getLegalName() : null)")
    @Mapping(target = "supplierTradeName", expression = "java(entity.getServiceSupplier() != null && entity.getServiceSupplier().getSupplier() != null ? entity.getServiceSupplier().getSupplier().getTradeName() : null)")
    @Mapping(target = "supplierCuit", expression = "java(entity.getServiceSupplier() != null && entity.getServiceSupplier().getSupplier() != null ? entity.getServiceSupplier().getSupplier().getCuit() : null)")
    @Mapping(target = "buildingId", expression = "java(entity.getBuilding() != null ? entity.getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(entity.getBuilding() != null ? entity.getBuilding().getName() : null)")
    @Mapping(target = "paymentLocationId", expression = "java(entity.getPaymentLocation() != null ? entity.getPaymentLocation().getId() : null)")
    @Mapping(target = "paymentLocationName", expression = "java(entity.getPaymentLocation() != null ? entity.getPaymentLocation().getName() : null)")
    @Mapping(target = "projectAreaId", expression = "java(entity.getProjectArea() != null ? entity.getProjectArea().getId() : null)")
    @Mapping(target = "projectAreaName", expression = "java(entity.getProjectArea() != null ? entity.getProjectArea().getName() : null)")
    @Mapping(target = "projectAreaColor", expression = "java(entity.getProjectArea() != null ? entity.getProjectArea().getColor() : null)")
    ServiceAssignmentResponseDTO toResponseDto(ServiceAssignment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceSupplier", ignore = true)
    @Mapping(target = "building", ignore = true)
    @Mapping(target = "paymentLocation", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    void partialUpdate(ServiceAssignmentDTO updateDTO, @MappingTarget ServiceAssignment entity);
}
