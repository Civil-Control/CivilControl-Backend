package PSG.backEnd.repository.PaymentRepository;

import PSG.backEnd.model.entity.payment.CashPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashPaymentRepository extends JpaRepository<CashPayment, Long> {
}