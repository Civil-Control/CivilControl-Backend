package PSG.backEnd.service.notification.resolver;

import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InsurancePolicyDueDateResolver implements NextDueDateResolver {

    private final InsurancePolicyRepository insurancePolicyRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.INSURANCE_POLICY; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long policyId) {
        return insurancePolicyRepository.findByIdAndDeletedFalse(policyId)
                .flatMap(this::computeNextPaymentDate)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return insurancePolicyRepository.findAllByDeletedFalseAndPolicyStatus(PolicyStatus.ACTIVO).stream()
                .flatMap(p -> computeNextPaymentDate(p).stream())
                .toList();
    }

    private Optional<SubjectDueDateInfo> computeNextPaymentDate(InsurancePolicy policy) {
        if (Boolean.TRUE.equals(policy.getDeleted())
                || policy.getPolicyStatus() != PolicyStatus.ACTIVO) {
            return Optional.empty();
        }

        LocalDate today      = LocalDate.now();
        LocalDate effectiveTo = policy.getEffectiveTo();
        if (today.isAfter(effectiveTo)) return Optional.empty();

        if (policy.getPaymentFrequency() == PaymentFrequency.PAGO_UNICO) {
            LocalDate dueDate = policy.getEffectiveFrom();
            return dueDate.isBefore(today)
                    ? Optional.empty()
                    : Optional.of(new SubjectDueDateInfo(policy.getId(), buildDisplayName(policy), dueDate));
        }

        int intervalMonths = switch (policy.getPaymentFrequency()) {
            case MENSUAL    -> 1;
            case BIMESTRAL  -> 2;
            case TRIMESTRAL -> 3;
            case SEMI_ANUAL -> 6;
            case ANUAL      -> 12;
            default         -> 1;
        };

        int dueDay = policy.getPeriodicDueDay() != null
                ? policy.getPeriodicDueDay()
                : policy.getEffectiveFrom().getDayOfMonth();

        LocalDate cursor = policy.getEffectiveFrom();
        while (!cursor.isAfter(effectiveTo)) {
            LocalDate paymentDate = cursor.withDayOfMonth(Math.min(dueDay, cursor.lengthOfMonth()));
            if (!paymentDate.isBefore(today) && !paymentDate.isAfter(effectiveTo)) {
                return Optional.of(new SubjectDueDateInfo(policy.getId(), buildDisplayName(policy), paymentDate));
            }
            cursor = cursor.plusMonths(intervalMonths);
        }
        return Optional.empty();
    }

    private String buildDisplayName(InsurancePolicy p) {
        return "Póliza " + p.getPolicyNumber() + " — " + p.getPolicyType().name();
    }
}
