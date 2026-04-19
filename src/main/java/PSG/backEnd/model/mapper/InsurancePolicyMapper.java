package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InsurancePolicyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "autoPolicy", ignore = true)
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToEntity")
    InsurancePolicy toEntity(InsurancePolicyDTO insurancePolicyDTO);

    @Mapping(target = "policyType", source = "insurancePolicy.policyType")
    @Mapping(target = "policyStatus", source = "insurancePolicy.policyStatus")
    @Mapping(target = "paymentFrequency", source = "insurancePolicy.paymentFrequency")
    @Mapping(target = "supplierId", source = "insurancePolicy.supplier.id")
    @Mapping(target = "supplierName", source = "insurancePolicy.supplier.legalName")
    InsurancePolicyResponseDTO toResponseDto(InsurancePolicy insurancePolicy);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "autoPolicy", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    void partialUpdate(InsurancePolicyDTO updateDTO, @MappingTarget InsurancePolicy insurancePolicy);

    @Named("supplierIdToEntity")
    default Supplier supplierIdToEntity(Long id) {
        if (id == null) return null;
        Supplier supplier = new Supplier();
        supplier.setId(id);
        return supplier;
    }
}
