package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierResponseDTO;
import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ServiceSupplierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "providedServices", source = "providedServices")
    ServiceSupplier toEntity(ServiceSupplierDTO serviceSupplierDTO);

    @Mapping(target = "supplierId", expression = "java(serviceSupplier.getSupplier() != null ? serviceSupplier.getSupplier().getId() : null)")
    @Mapping(target = "supplierName", expression = "java(serviceSupplier.getSupplier() != null ? serviceSupplier.getSupplier().getLegalName() : null)")
    @Mapping(target = "providedServices", source = "providedServices", qualifiedByName = "mapServiceTypesToStrings")
    ServiceSupplierResponseDTO toResponseDto(ServiceSupplier serviceSupplier);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    void partialUpdate(ServiceSupplierDTO updateDTO, @MappingTarget ServiceSupplier serviceSupplier);

    @Named("mapServiceTypesToStrings")
    default List<String> mapServiceTypesToStrings(List<PSG.backEnd.model.enums.ServiceType> serviceTypes) {
        if (serviceTypes == null || serviceTypes.isEmpty()) {
            return List.of();
        }
        return serviceTypes.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    @AfterMapping
    default void handleCollections(@MappingTarget ServiceSupplier serviceSupplier, ServiceSupplierDTO updateDTO) {
        if (updateDTO != null && updateDTO.providedServices() != null && !updateDTO.providedServices().isEmpty()) {
            serviceSupplier.setProvidedServices(updateDTO.providedServices());
        }
    }
}

