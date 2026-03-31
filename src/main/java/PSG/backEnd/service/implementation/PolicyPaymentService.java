package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.insurance.InsurancePolicyNotFoundException;
import PSG.backEnd.exception.insurance.PolicyPaymentNotFoundException;
import PSG.backEnd.model.dto.insurance.PolicyPaymentDTO;
import PSG.backEnd.model.dto.insurance.PolicyPaymentResponseDTO;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.insurance.PolicyPayment;
import PSG.backEnd.model.mapper.PolicyPaymentMapper;
import PSG.backEnd.repository.InsurancePolicyRepository;
import PSG.backEnd.repository.PolicyPaymentRepository;
import PSG.backEnd.service.port.IPolicyPaymentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyPaymentService implements IPolicyPaymentService {

    private final PolicyPaymentRepository paymentRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final PolicyPaymentMapper paymentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public PolicyPaymentResponseDTO createPayment(PolicyPaymentDTO dto) {
        // Validate insurance policy exists
        InsurancePolicy policy = insurancePolicyRepository.findByIdAndDeletedFalse(dto.insurancePolicyId())
                .orElseThrow(() -> new InsurancePolicyNotFoundException(dto.insurancePolicyId()));

        validateBusinessRules(dto);

        PolicyPayment entity = paymentMapper.toEntity(dto);
        entity.setInsurancePolicy(policy);
        PolicyPayment saved = paymentRepository.save(entity);

        return paymentMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyPaymentResponseDTO getPaymentById(Long id) {
        PolicyPayment entity = getEntityById(id);
        return paymentMapper.toResponseDto(entity);
    }

    @Override
    public PolicyPaymentResponseDTO updatePayment(Long id, PolicyPaymentDTO dto) {
        PolicyPayment existing = getEntityById(id);

        if (dto.periodFrom() != null && dto.periodTo() != null) {
            validateBusinessRules(dto);
        }

        paymentMapper.partialUpdate(dto, existing);
        PolicyPayment saved = paymentRepository.save(existing);
        return paymentMapper.toResponseDto(saved);
    }

    @Override
    public void deletePayment(Long id) {
        PolicyPayment entity = getEntityById(id);
        entity.setDeleted(true);
        paymentRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyPaymentResponseDTO> getPaymentsByPolicyId(Long insurancePolicyId) {
        // Validate policy exists
        insurancePolicyRepository.findByIdAndDeletedFalse(insurancePolicyId)
                .orElseThrow(() -> new InsurancePolicyNotFoundException(insurancePolicyId));

        return paymentRepository.findByInsurancePolicyIdAndDeletedFalseOrderByPeriodFromDesc(insurancePolicyId)
                .stream()
                .map(paymentMapper::toResponseDto)
                .toList();
    }

    private PolicyPayment getEntityById(Long id) {
        return paymentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new PolicyPaymentNotFoundException(id));
    }

    private void validateBusinessRules(PolicyPaymentDTO dto) {
        // periodFrom must be before periodTo
        if (dto.periodFrom() != null && dto.periodTo() != null) {
            if (dto.periodFrom().isAfter(dto.periodTo())) {
                throw new IllegalArgumentException(
                        messageSourceHelper.getMessage("policyPayment.periodFrom.beforePeriodTo"));
            }
        }
    }
}
