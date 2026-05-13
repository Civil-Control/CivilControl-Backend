package PSG.backEnd.repository.PaymentRepository;

import PSG.backEnd.model.entity.payment.CheckPayment;
import PSG.backEnd.model.enums.payment.CheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckPaymentRepository extends JpaRepository<CheckPayment, Long> {

    Optional<CheckPayment> findByIdAndDeletedFalse(Long id);

    List<CheckPayment> findAllByStatusAndDeletedFalse(CheckStatus status);
}