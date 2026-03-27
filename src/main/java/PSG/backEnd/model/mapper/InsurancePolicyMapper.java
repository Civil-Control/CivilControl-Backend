package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InsurancePolicyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "autoPolicy", ignore = true)
    InsurancePolicy toEntity(InsurancePolicyDTO insurancePolicyDTO);

    @Mapping(target = "policyType", source = "insurancePolicy.policyType")
    @Mapping(target = "policyStatus", source = "insurancePolicy.policyStatus")
    @Mapping(target = "paymentFrequency", source = "insurancePolicy.paymentFrequency")
    InsurancePolicyResponseDTO toResponseDto(InsurancePolicy insurancePolicy);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "autoPolicy", ignore = true)
    void partialUpdate(InsurancePolicyDTO updateDTO, @MappingTarget InsurancePolicy insurancePolicy);
}
