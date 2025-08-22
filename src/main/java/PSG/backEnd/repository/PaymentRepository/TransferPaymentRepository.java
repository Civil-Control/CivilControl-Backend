package PSG.backEnd.repository.PaymentRepository;

import PSG.backEnd.model.entity.payment.TransferPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferPaymentRepository extends JpaRepository<TransferPayment, Long> {
}