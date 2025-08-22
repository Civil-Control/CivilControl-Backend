package PSG.backEnd.repository.PaymentRepository;

import PSG.backEnd.model.entity.payment.CheckPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckPaymentRepository extends JpaRepository<CheckPayment, Long> {
}