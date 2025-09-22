package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.insurance.AutoPolicyDTO;
import PSG.backEnd.model.dto.insurance.AutoPolicyResponseDTO;
import PSG.backEnd.model.entity.insurance.AutoPolicy;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PolicyVehicleMapper.class})
public interface AutoPolicyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "insurancePolicy", source = "insurancePolicyId", qualifiedByName = "insurancePolicyIdToEntity")
    @Mapping(target = "policyVehicles", source = "policyVehicles")
    AutoPolicy toEntity(AutoPolicyDTO autoPolicyDTO);

    @Mapping(target = "insurancePolicyId", source = "insurancePolicy.id")
    AutoPolicyResponseDTO toResponseDto(AutoPolicy autoPolicy);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "insurancePolicy", source = "insurancePolicyId", qualifiedByName = "insurancePolicyIdToEntity")
    void partialUpdate(AutoPolicyDTO updateDTO, @MappingTarget AutoPolicy autoPolicy);

    @Named("insurancePolicyIdToEntity")
    default InsurancePolicy insurancePolicyIdToEntity(Long insurancePolicyId) {
        if (insurancePolicyId == null) {
            return null;
        }
        InsurancePolicy insurancePolicy = new InsurancePolicy();
        insurancePolicy.setId(insurancePolicyId);
        return insurancePolicy;
    }
}
