package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServicePaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceSupplier", ignore = true)
    @Mapping(target = "building", ignore = true)
    ServicePayment toEntity(ServicePaymentDTO servicePaymentDTO);

    @Mapping(target = "serviceSupplierId", expression = "java(servicePayment.getServiceSupplier() != null ? servicePayment.getServiceSupplier().getId() : null)")
    @Mapping(target = "supplierName", expression = "java(servicePayment.getServiceSupplier() != null && servicePayment.getServiceSupplier().getSupplier() != null ? servicePayment.getServiceSupplier().getSupplier().getLegalName() : null)")
    @Mapping(target = "supplierTradeName", expression = "java(servicePayment.getServiceSupplier() != null && servicePayment.getServiceSupplier().getSupplier() != null ? servicePayment.getServiceSupplier().getSupplier().getTradeName() : null)")
    @Mapping(target = "supplierCuit", expression = "java(servicePayment.getServiceSupplier() != null && servicePayment.getServiceSupplier().getSupplier() != null ? servicePayment.getServiceSupplier().getSupplier().getCuit() : null)")
    @Mapping(target = "buildingId", expression = "java(servicePayment.getBuilding() != null ? servicePayment.getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(servicePayment.getBuilding() != null ? servicePayment.getBuilding().getName() : null)")
    ServicePaymentResponseDTO toResponseDto(ServicePayment servicePayment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceSupplier", ignore = true)
    @Mapping(target = "building", ignore = true)
    void partialUpdate(ServicePaymentDTO updateDTO, @MappingTarget ServicePayment servicePayment);
}

