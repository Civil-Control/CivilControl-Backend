package PSG.backEnd.service.implementation;

import PSG.backEnd.model.dto.insurance.InsurancePolicyPaymentCreateDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyPaymentResponseDTO;
import PSG.backEnd.model.dto.payment.*;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.insurance.InsurancePolicyPaymentDetail;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.repository.InsurancePolicyPaymentDetailRepository;
import PSG.backEnd.repository.PaymentRepository.PaymentRepository;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.port.IPaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InsurancePolicyPaymentService {

    private final InsurancePolicyPaymentDetailRepository policyPaymentDetailRepository;
    private final PaymentRepository paymentRepository;
    private final IPaymentService paymentService;
    private final IInsurancePolicyService insurancePolicyService;

    public InsurancePolicyPaymentResponseDTO createPayment(Long policyId, InsurancePolicyPaymentCreateDTO dto) {
        InsurancePolicy policy = insurancePolicyService.getEntityById(policyId);

        if (policy.getSupplier() == null) {
            throw new IllegalStateException("La póliza debe tener una aseguradora (proveedor) asignada para registrar pagos.");
        }

        Long supplierId = policy.getSupplier().getId();

        PaymentDetailsDTO paymentDetailsDTO = new PaymentDetailsDTO(
                dto.paymentDate(),
                supplierId,
                dto.amount(),
                dto.notes(),
                Collections.emptyList()
        );

        PaymentResponseDTO paymentResponse = switch (dto.paymentMethod().toUpperCase()) {
            case "CASH" -> paymentService.createCash(
                    new CashPaymentDTO(paymentDetailsDTO, false));
            case "TRANSFER" -> paymentService.createTransfer(
                    new TransferPaymentDTO(paymentDetailsDTO, dto.transactionNumber(), dto.bankName(), false));
            case "CHECK" -> paymentService.createCheck(
                    new CheckPaymentDTO(paymentDetailsDTO, dto.checkDueDate(), dto.checkNumber(), dto.bankName(), false));
            default -> throw new IllegalArgumentException("Método de pago no válido: " + dto.paymentMethod());
        };

        Long paymentDetailsId = extractPaymentDetailsId(paymentResponse);

        PaymentDetails paymentDetails = paymentRepository.findById(paymentDetailsId)
                .orElseThrow(() -> new EntityNotFoundException("PaymentDetails no encontrado: " + paymentDetailsId));

        InsurancePolicyPaymentDetail link = InsurancePolicyPaymentDetail.builder()
                .insurancePolicy(policy)
                .paymentDetails(paymentDetails)
                .periodFrom(dto.periodFrom())
                .periodTo(dto.periodTo())
                .deleted(false)
                .build();

        InsurancePolicyPaymentDetail saved = policyPaymentDetailRepository.save(link);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<InsurancePolicyPaymentResponseDTO> getPaymentsByPolicyId(Long policyId) {
        return policyPaymentDetailRepository.findByPolicyIdWithPaymentDetails(policyId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public InsurancePolicyPaymentResponseDTO getPaymentById(Long id) {
        InsurancePolicyPaymentDetail detail = policyPaymentDetailRepository.findByIdWithPaymentDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Pago de póliza no encontrado: " + id));
        return toResponseDTO(detail);
    }

    public InsurancePolicyPaymentResponseDTO updatePayment(Long id, InsurancePolicyPaymentCreateDTO dto) {
        InsurancePolicyPaymentDetail detail = policyPaymentDetailRepository.findByIdWithPaymentDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Pago de póliza no encontrado: " + id));

        PaymentDetails pd = detail.getPaymentDetails();
        Long supplierId = pd.getSupplier().getId();

        PaymentDetailsDTO paymentDetailsDTO = new PaymentDetailsDTO(
                dto.paymentDate(),
                supplierId,
                dto.amount(),
                dto.notes(),
                Collections.emptyList()
        );

        String method = dto.paymentMethod().toUpperCase();
        Long paymentId = pd.getId();

        switch (method) {
            case "CASH" -> paymentService.updateCash(paymentId, new CashPaymentDTO(paymentDetailsDTO, false));
            case "TRANSFER" -> paymentService.updateTransfer(paymentId,
                    new TransferPaymentDTO(paymentDetailsDTO, dto.transactionNumber(), dto.bankName(), false));
            case "CHECK" -> paymentService.updateCheck(paymentId,
                    new CheckPaymentDTO(paymentDetailsDTO, dto.checkDueDate(), dto.checkNumber(), dto.bankName(), false));
            default -> throw new IllegalArgumentException("Método de pago no válido: " + method);
        }

        detail.setPeriodFrom(dto.periodFrom());
        detail.setPeriodTo(dto.periodTo());
        policyPaymentDetailRepository.save(detail);

        // Re-fetch to get updated data
        InsurancePolicyPaymentDetail updated = policyPaymentDetailRepository.findByIdWithPaymentDetails(id)
                .orElseThrow();
        return toResponseDTO(updated);
    }

    public void deletePayment(Long id) {
        InsurancePolicyPaymentDetail detail = policyPaymentDetailRepository.findByIdWithPaymentDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Pago de póliza no encontrado: " + id));

        Long paymentDetailsId = detail.getPaymentDetails().getId();

        // Delete payment (handles balance reversal)
        paymentService.deleteById(paymentDetailsId);

        // Soft delete the link
        detail.setDeleted(true);
        policyPaymentDetailRepository.save(detail);
    }

    private InsurancePolicyPaymentResponseDTO toResponseDTO(InsurancePolicyPaymentDetail detail) {
        PaymentDetails pd = detail.getPaymentDetails();
        InsurancePolicy policy = detail.getInsurancePolicy();

        String paymentMethod;
        String bankName = null;
        String transactionNumber = null;
        String checkNumber = null;
        LocalDate checkDueDate = null;

        if (pd.getCashPayment() != null) {
            paymentMethod = "CASH";
        } else if (pd.getTransferPayment() != null) {
            paymentMethod = "TRANSFER";
            bankName = pd.getTransferPayment().getBankName();
            transactionNumber = pd.getTransferPayment().getTransactionNumber();
        } else if (pd.getCheckPayment() != null) {
            paymentMethod = "CHECK";
            bankName = pd.getCheckPayment().getBankName();
            checkNumber = pd.getCheckPayment().getCheckNumber();
            checkDueDate = pd.getCheckPayment().getDueDate();
        } else {
            paymentMethod = "UNKNOWN";
        }

        return new InsurancePolicyPaymentResponseDTO(
                detail.getId(),
                pd.getId(),
                policy.getId(),
                policy.getPolicyNumber(),
                pd.getPaymentDate(),
                pd.getAmount(),
                detail.getPeriodFrom(),
                detail.getPeriodTo(),
                pd.getComment(),
                paymentMethod,
                bankName,
                transactionNumber,
                checkNumber,
                checkDueDate
        );
    }

    private Long extractPaymentDetailsId(PaymentResponseDTO response) {
        if (response instanceof CashPaymentResponseDTO cash) {
            return cash.paymentDetails().id();
        } else if (response instanceof TransferPaymentResponseDTO transfer) {
            return transfer.paymentDetails().id();
        } else if (response instanceof CheckPaymentResponseDTO check) {
            return check.paymentDetails().id();
        }
        throw new IllegalStateException("Tipo de pago no reconocido");
    }
}
