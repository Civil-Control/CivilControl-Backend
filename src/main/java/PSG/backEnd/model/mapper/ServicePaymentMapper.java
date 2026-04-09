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
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    ServicePayment toEntity(ServicePaymentDTO servicePaymentDTO);

    // ——— Building-based fields (from service assignment) ———
    @Mapping(target = "serviceAssignmentId", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getId() : null)")
    @Mapping(target = "serviceSupplierId", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getId() : null)")
    @Mapping(target = "supplierName", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null && sp.getServiceAssignment().getServiceSupplier().getSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getSupplier().getLegalName() : null)")
    @Mapping(target = "supplierTradeName", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null && sp.getServiceAssignment().getServiceSupplier().getSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getSupplier().getTradeName() : null)")
    @Mapping(target = "supplierCuit", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null && sp.getServiceAssignment().getServiceSupplier().getSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getSupplier().getCuit() : null)")
    @Mapping(target = "buildingId", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getBuilding() != null ? sp.getServiceAssignment().getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getBuilding() != null ? sp.getServiceAssignment().getBuilding().getName() : null)")
    @Mapping(target = "serviceType", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getServiceType() : null)")
    @Mapping(target = "serviceCategory", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getServiceCategory() : null)")
    @Mapping(target = "accountNumber", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getAccountNumber() : null)")
    @Mapping(target = "accountHolder", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getAccountHolder() : null)")
    // ——— Vehicle-based fields ———
    @Mapping(target = "vehicleId", expression = "java(sp.getVehicle() != null ? sp.getVehicle().getId() : null)")
    @Mapping(target = "vehicleLicensePlate", expression = "java(sp.getVehicle() != null ? sp.getVehicle().getLicensePlate() : null)")
    // ——— Common fields ———
    @Mapping(target = "projectAreaId", expression = "java(sp.getProjectArea() != null ? sp.getProjectArea().getId() : null)")
    @Mapping(target = "projectAreaName", expression = "java(sp.getProjectArea() != null ? sp.getProjectArea().getName() : null)")
    @Mapping(target = "projectAreaColor", expression = "java(sp.getProjectArea() != null ? sp.getProjectArea().getColor() : null)")
    ServicePaymentResponseDTO toResponseDto(ServicePayment sp);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceAssignment", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    void partialUpdate(ServicePaymentDTO updateDTO, @MappingTarget ServicePayment servicePayment);
}

