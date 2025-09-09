package PSG.backEnd.repository.PaymentRepository;

import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentDetails, Long> {

    @Query("SELECT pd FROM PaymentDetails pd " +
            "LEFT JOIN FETCH pd.supplier " +
            "WHERE " +
            "(:paymentMethod IS NULL OR " +
            "  (EXISTS (SELECT 1 FROM CashPayment cp WHERE cp.paymentDetails.id = pd.id AND :paymentMethod = 'CASH') OR " +
            "   EXISTS (SELECT 1 FROM CheckPayment chp WHERE chp.paymentDetails.id = pd.id AND :paymentMethod = 'CHECK') OR " +
            "   EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND :paymentMethod = 'TRANSFER'))) AND " +
            "(:startDate IS NULL OR pd.paymentDate >= :startDate) AND " +
            "(:endDate IS NULL OR pd.paymentDate <= :endDate) AND " +
            "(:minAmount IS NULL OR pd.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR pd.amount <= :maxAmount) AND " +
            "(:transactionNumber IS NULL OR " +
            "  (EXISTS (SELECT 1 FROM CheckPayment chp WHERE chp.paymentDetails.id = pd.id AND chp.checkNumber = :transactionNumber) OR " +
            "   EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.transactionNumber = :transactionNumber))) AND " +
            "(:supplierId IS NULL OR pd.supplier.id = :supplierId) AND " +
            "(NOT EXISTS (SELECT 1 FROM CashPayment cp WHERE cp.paymentDetails.id = pd.id AND cp.deleted = true) AND " +
            " NOT EXISTS (SELECT 1 FROM CheckPayment chp WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) AND " +
            " NOT EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.deleted = true))")
    Page<PaymentDetails> findAllWithFilters(
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("transactionNumber") String transactionNumber,
            @Param("supplierId") Long supplierId,
            Pageable pageable
    );
}
