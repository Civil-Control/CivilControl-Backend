package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.supplier.SupplierDTO;
import PSG.backEnd.model.dto.supplier.SupplierResponseDTO;
import PSG.backEnd.model.entity.Supplier;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, ContactInfoMapper.class})
public interface SupplierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pendingBalance", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Supplier toEntity(SupplierDTO supplierDTO);

    SupplierResponseDTO toResponseDto(Supplier supplier);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pendingBalance", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void partialUpdate(SupplierDTO updateDTO, @MappingTarget Supplier supplier);

    @AfterMapping
    default void handleCollections(@MappingTarget Supplier supplier, SupplierDTO updateDTO) {
        if (updateDTO.allowedPaymentMethods() != null && !updateDTO.allowedPaymentMethods().isEmpty()) {
            supplier.setAllowedPaymentMethods(updateDTO.allowedPaymentMethods());
        }
    }
}
