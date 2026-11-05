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
    @Mapping(target = "projectAreaTask", ignore = true)
    ServicePayment toEntity(ServicePaymentDTO servicePaymentDTO);

    // ——— Fields from service assignment ———
    @Mapping(target = "serviceAssignmentId", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getId() : null)")
    @Mapping(target = "subjectType", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getSubjectType() : null)")
    @Mapping(target = "serviceSupplierId", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getId() : null)")
    @Mapping(target = "supplierName", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null && sp.getServiceAssignment().getServiceSupplier().getSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getSupplier().getLegalName() : null)")
    @Mapping(target = "supplierTradeName", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null && sp.getServiceAssignment().getServiceSupplier().getSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getSupplier().getTradeName() : null)")
    @Mapping(target = "supplierCuit", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getServiceSupplier() != null && sp.getServiceAssignment().getServiceSupplier().getSupplier() != null ? sp.getServiceAssignment().getServiceSupplier().getSupplier().getCuit() : null)")
    @Mapping(target = "buildingId", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getBuilding() != null ? sp.getServiceAssignment().getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getBuilding() != null ? sp.getServiceAssignment().getBuilding().getName() : null)")
    @Mapping(target = "vehicleId", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getVehicle() != null ? sp.getServiceAssignment().getVehicle().getId() : null)")
    @Mapping(target = "vehicleLicensePlate", expression = "java(sp.getServiceAssignment() != null && sp.getServiceAssignment().getVehicle() != null ? sp.getServiceAssignment().getVehicle().getLicensePlate() : null)")
    @Mapping(target = "serviceType", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getServiceType() : null)")
    @Mapping(target = "accountNumber", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getAccountNumber() : null)")
    @Mapping(target = "accountHolder", expression = "java(sp.getServiceAssignment() != null ? sp.getServiceAssignment().getAccountHolder() : null)")
    // ——— Payment fields ———
    @Mapping(target = "projectAreaId", expression = "java(sp.getProjectArea() != null ? sp.getProjectArea().getId() : null)")
    @Mapping(target = "projectAreaName", expression = "java(sp.getProjectArea() != null ? sp.getProjectArea().getName() : null)")
    @Mapping(target = "projectAreaColor", expression = "java(sp.getProjectArea() != null ? sp.getProjectArea().getColor() : null)")
    @Mapping(target = "projectAreaTaskId", expression = "java(sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getId() : null)")
    @Mapping(target = "projectAreaTaskName", expression = "java(sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getName() : null)")
    ServicePaymentResponseDTO toResponseDto(ServicePayment sp);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceAssignment", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    void partialUpdate(ServicePaymentDTO updateDTO, @MappingTarget ServicePayment servicePayment);
}

