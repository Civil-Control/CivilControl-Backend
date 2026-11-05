package PSG.backEnd.repository.PaymentRepository;


import PSG.backEnd.model.entity.payment.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long>{
}