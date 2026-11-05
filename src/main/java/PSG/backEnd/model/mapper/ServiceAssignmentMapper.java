package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentResponseDTO;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.serviceSupplier.SpecificDueDate;
import org.mapstruct.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceSupplier", ignore = true)
    @Mapping(target = "building", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "paymentLocation", ignore = true)
    @Mapping(target = "specificDueDates", ignore = true)
    ServiceAssignment toEntity(ServiceAssignmentDTO dto);

    @Mapping(target = "serviceSupplierId", expression = "java(entity.getServiceSupplier() != null ? entity.getServiceSupplier().getId() : null)")
    @Mapping(target = "supplierName", expression = "java(entity.getServiceSupplier() != null && entity.getServiceSupplier().getSupplier() != null ? entity.getServiceSupplier().getSupplier().getLegalName() : null)")
    @Mapping(target = "supplierTradeName", expression = "java(entity.getServiceSupplier() != null && entity.getServiceSupplier().getSupplier() != null ? entity.getServiceSupplier().getSupplier().getTradeName() : null)")
    @Mapping(target = "supplierCuit", expression = "java(entity.getServiceSupplier() != null && entity.getServiceSupplier().getSupplier() != null ? entity.getServiceSupplier().getSupplier().getCuit() : null)")
    @Mapping(target = "buildingId", expression = "java(entity.getBuilding() != null ? entity.getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(entity.getBuilding() != null ? entity.getBuilding().getName() : null)")
    @Mapping(target = "vehicleId", expression = "java(entity.getVehicle() != null ? entity.getVehicle().getId() : null)")
    @Mapping(target = "vehicleLicensePlate", expression = "java(entity.getVehicle() != null ? entity.getVehicle().getLicensePlate() : null)")
    @Mapping(target = "paymentLocationId", expression = "java(entity.getPaymentLocation() != null ? entity.getPaymentLocation().getId() : null)")
    @Mapping(target = "paymentLocationName", expression = "java(entity.getPaymentLocation() != null ? entity.getPaymentLocation().getName() : null)")
    @Mapping(target = "subjectProjectAreaId", expression = "java(resolveSubjectProjectAreaId(entity))")
    @Mapping(target = "subjectProjectAreaName", expression = "java(resolveSubjectProjectAreaName(entity))")
    @Mapping(target = "specificDueDates", expression = "java(mapSpecificDueDates(entity))")
    ServiceAssignmentResponseDTO toResponseDto(ServiceAssignment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "serviceSupplier", ignore = true)
    @Mapping(target = "building", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "paymentLocation", ignore = true)
    @Mapping(target = "specificDueDates", ignore = true)
    void partialUpdate(ServiceAssignmentDTO updateDTO, @MappingTarget ServiceAssignment entity);

    default List<LocalDate> mapSpecificDueDates(ServiceAssignment entity) {
        if (entity.getSpecificDueDates() == null || entity.getSpecificDueDates().isEmpty()) {
            return Collections.emptyList();
        }
        return entity.getSpecificDueDates().stream()
                .map(SpecificDueDate::getDueDate)
                .sorted()
                .toList();
    }

    default Long resolveSubjectProjectAreaId(ServiceAssignment entity) {
        if (entity.getBuilding() != null && entity.getBuilding().getProjectArea() != null) {
            return entity.getBuilding().getProjectArea().getId();
        }
        if (entity.getVehicle() != null && entity.getVehicle().getProjectArea() != null) {
            return entity.getVehicle().getProjectArea().getId();
        }
        return null;
    }

    default String resolveSubjectProjectAreaName(ServiceAssignment entity) {
        if (entity.getBuilding() != null && entity.getBuilding().getProjectArea() != null) {
            return entity.getBuilding().getProjectArea().getName();
        }
        if (entity.getVehicle() != null && entity.getVehicle().getProjectArea() != null) {
            return entity.getVehicle().getProjectArea().getName();
        }
        return null;
    }
}
