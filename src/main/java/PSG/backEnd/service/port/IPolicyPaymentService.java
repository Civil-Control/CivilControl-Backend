package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.insurance.PolicyPaymentDTO;
import PSG.backEnd.model.dto.insurance.PolicyPaymentResponseDTO;

import java.util.List;

public interface IPolicyPaymentService {

    PolicyPaymentResponseDTO createPayment(PolicyPaymentDTO dto);

    PolicyPaymentResponseDTO getPaymentById(Long id);

    PolicyPaymentResponseDTO updatePayment(Long id, PolicyPaymentDTO dto);

    void deletePayment(Long id);

    List<PolicyPaymentResponseDTO> getPaymentsByPolicyId(Long insurancePolicyId);
}
