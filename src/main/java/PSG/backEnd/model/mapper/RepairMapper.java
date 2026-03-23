package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairOrderResponseDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.entity.vehicle.RepairOrder;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.enums.vehicle.RepairType;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RepairMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToEntity")
    @Mapping(target = "repairTypes", source = "repairTypes", qualifiedByName = "stringsToRepairTypes")
    @Mapping(target = "repairOrder", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    Repair toEntity(RepairDTO repairDTO);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierLegalName", source = "supplier.legalName")
    @Mapping(target = "supplierTradeName", source = "supplier.tradeName")
    @Mapping(target = "repairTypes", source = "repairTypes", qualifiedByName = "repairTypesToStrings")
    @Mapping(target = "repairOrder", source = "repairOrder", qualifiedByName = "mapRepairOrderToDto")
    @Mapping(target = "transactionalDocument", source = "transactionalDocument", qualifiedByName = "documentToSummaryDto")
    RepairResponseDTO toResponseDto(Repair repair);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToEntity")
    @Mapping(target = "repairTypes", source = "repairTypes", qualifiedByName = "stringsToRepairTypes")
    @Mapping(target = "repairOrder", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    void partialUpdate(RepairDTO updateDTO, @MappingTarget Repair repair);

    @Named("stringsToRepairTypes")
    default List<RepairType> stringsToRepairTypes(List<String> strings) {
        if (strings == null) return null;
        return strings.stream()
                .map(RepairType::valueOf)
                .collect(Collectors.toList());
    }

    @Named("repairTypesToStrings")
    default List<String> repairTypesToStrings(List<RepairType> types) {
        if (types == null) return null;
        return types.stream()
                .map(RepairType::name)
                .collect(Collectors.toList());
    }

    @Named("vehicleIdToEntity")
    default Vehicle vehicleIdToEntity(Long vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        return vehicle;
    }

    @Named("supplierIdToEntity")
    default Supplier supplierIdToEntity(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        return supplier;
    }

    @Named("mapRepairOrderToDto")
    default RepairOrderResponseDTO mapRepairOrderToDto(RepairOrder order) {
        if (order == null) return null;
        return new RepairOrderResponseDTO(
                order.getId(),
                order.getDate(),
                order.getVehicle() != null ? order.getVehicle().getId() : null,
                order.getVehicle() != null ? order.getVehicle().getLicensePlate() : null,
                order.getDescription(),
                order.getReportedBy(),
                order.getStatus(),
                order.getCreatedByUser() != null
                        ? order.getCreatedByUser().getFirstName() + " " + order.getCreatedByUser().getLastName()
                        : null
        );
    }

    @Named("documentToSummaryDto")
    default TransactionalDocumentSummaryDTO documentToSummaryDto(TransactionalDocument doc) {
        if (doc == null) return null;
        return new TransactionalDocumentSummaryDTO(
                doc.getId(),
                doc.getDocumentType() != null ? doc.getDocumentType().name() : null,
                doc.getBranchCode(),
                doc.getDocumentNumber(),
                doc.getSupplier() != null ? doc.getSupplier().getLegalName() : null,
                doc.getTotal(),
                doc.getDate()
        );
    }
}

