package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServicePaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceAssignment", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    ServicePayment toEntity(ServicePaymentDTO servicePaymentDTO);

    @Mapping(target = "serviceAssignmentId", expression = "java(servicePayment.getServiceAssignment() != null ? servicePayment.getServiceAssignment().getId() : null)")
    @Mapping(target = "serviceSupplierId", expression = "java(servicePayment.getServiceAssignment() != null && servicePayment.getServiceAssignment().getServiceSupplier() != null ? servicePayment.getServiceAssignment().getServiceSupplier().getId() : null)")
    @Mapping(target = "supplierName", expression = "java(servicePayment.getServiceAssignment() != null && servicePayment.getServiceAssignment().getServiceSupplier() != null && servicePayment.getServiceAssignment().getServiceSupplier().getSupplier() != null ? servicePayment.getServiceAssignment().getServiceSupplier().getSupplier().getLegalName() : null)")
    @Mapping(target = "supplierTradeName", expression = "java(servicePayment.getServiceAssignment() != null && servicePayment.getServiceAssignment().getServiceSupplier() != null && servicePayment.getServiceAssignment().getServiceSupplier().getSupplier() != null ? servicePayment.getServiceAssignment().getServiceSupplier().getSupplier().getTradeName() : null)")
    @Mapping(target = "supplierCuit", expression = "java(servicePayment.getServiceAssignment() != null && servicePayment.getServiceAssignment().getServiceSupplier() != null && servicePayment.getServiceAssignment().getServiceSupplier().getSupplier() != null ? servicePayment.getServiceAssignment().getServiceSupplier().getSupplier().getCuit() : null)")
    @Mapping(target = "buildingId", expression = "java(servicePayment.getServiceAssignment() != null && servicePayment.getServiceAssignment().getBuilding() != null ? servicePayment.getServiceAssignment().getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(servicePayment.getServiceAssignment() != null && servicePayment.getServiceAssignment().getBuilding() != null ? servicePayment.getServiceAssignment().getBuilding().getName() : null)")
    @Mapping(target = "serviceType", expression = "java(servicePayment.getServiceAssignment() != null ? servicePayment.getServiceAssignment().getServiceType() : null)")
    @Mapping(target = "serviceCategory", expression = "java(servicePayment.getServiceAssignment() != null ? servicePayment.getServiceAssignment().getServiceCategory() : null)")
    @Mapping(target = "accountNumber", expression = "java(servicePayment.getServiceAssignment() != null ? servicePayment.getServiceAssignment().getAccountNumber() : null)")
    @Mapping(target = "accountHolder", expression = "java(servicePayment.getServiceAssignment() != null ? servicePayment.getServiceAssignment().getAccountHolder() : null)")
    @Mapping(target = "projectAreaId", expression = "java(servicePayment.getProjectArea() != null ? servicePayment.getProjectArea().getId() : null)")
    @Mapping(target = "projectAreaName", expression = "java(servicePayment.getProjectArea() != null ? servicePayment.getProjectArea().getName() : null)")
    @Mapping(target = "projectAreaColor", expression = "java(servicePayment.getProjectArea() != null ? servicePayment.getProjectArea().getColor() : null)")
    ServicePaymentResponseDTO toResponseDto(ServicePayment servicePayment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceAssignment", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    void partialUpdate(ServicePaymentDTO updateDTO, @MappingTarget ServicePayment servicePayment);
}

