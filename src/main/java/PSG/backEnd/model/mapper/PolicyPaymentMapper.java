package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.insurance.PolicyPaymentDTO;
import PSG.backEnd.model.dto.insurance.PolicyPaymentResponseDTO;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.insurance.PolicyPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PolicyPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "insurancePolicy", source = "insurancePolicyId", qualifiedByName = "policyIdToEntity")
    PolicyPayment toEntity(PolicyPaymentDTO dto);

    @Mapping(target = "insurancePolicyId", source = "insurancePolicy.id")
    @Mapping(target = "policyNumber", source = "insurancePolicy.policyNumber")
    PolicyPaymentResponseDTO toResponseDto(PolicyPayment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "insurancePolicy", ignore = true)
    void partialUpdate(PolicyPaymentDTO dto, @MappingTarget PolicyPayment entity);

    @Named("policyIdToEntity")
    default InsurancePolicy policyIdToEntity(Long id) {
        if (id == null) return null;
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId(id);
        return policy;
    }
}
