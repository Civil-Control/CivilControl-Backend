package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import PSG.backEnd.model.entity.vehicle.Repair;
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
    Repair toEntity(RepairDTO repairDTO);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierLegalName", source = "supplier.legalName")
    @Mapping(target = "supplierTradeName", source = "supplier.tradeName")
    @Mapping(target = "repairTypes", source = "repairTypes", qualifiedByName = "repairTypesToStrings")
    RepairResponseDTO toResponseDto(Repair repair);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToEntity")
    @Mapping(target = "repairTypes", source = "repairTypes", qualifiedByName = "stringsToRepairTypes")
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
}

