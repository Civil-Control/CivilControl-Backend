package PSG.backEnd.service.notification.resolver;

import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.entity.payment.CheckPayment;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.model.enums.payment.CheckStatus;
import PSG.backEnd.repository.PaymentRepository.CheckPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CheckPaymentDueDateResolver implements NextDueDateResolver {

    private final CheckPaymentRepository checkPaymentRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.CHECK_PAYMENT; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long checkId) {
        return checkPaymentRepository.findByIdAndDeletedFalse(checkId)
                .filter(c -> c.getStatus() == CheckStatus.PENDIENTE && c.getDueDate() != null)
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getDueDate()))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return checkPaymentRepository.findAllByStatusAndDeletedFalse(CheckStatus.PENDIENTE).stream()
                .filter(c -> c.getDueDate() != null)
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getDueDate()))
                .toList();
    }

    private String buildDisplayName(CheckPayment c) {
        String num = c.getCheckNumber() != null ? " #" + c.getCheckNumber() : "";
        return "Cheque" + num + " — " + c.getBankAccount().getName();
    }
}
