package PSG.backEnd.repository.PaymentRepository;

import PSG.backEnd.model.entity.payment.PaymentApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentApplicationRepository extends JpaRepository<PaymentApplication, Long> {

    List<PaymentApplication> findByPayment_Id(Long paymentDetailsId);

    List<PaymentApplication> findByDocument_Id(Long documentId);

    /**
     * Sum of payment amounts applied to the given document, excluding applications
     * that belong to soft-deleted payments (cash/transfer/check).
     */
    @Query("SELECT COALESCE(SUM(pa.amountApplied), 0) FROM PaymentApplication pa " +
            "WHERE pa.document.id = :documentId " +
            "AND NOT EXISTS (SELECT 1 FROM CashPayment     cp  WHERE cp.paymentDetails.id  = pa.payment.id AND cp.deleted  = true) " +
            "AND NOT EXISTS (SELECT 1 FROM CheckPayment    chp WHERE chp.paymentDetails.id = pa.payment.id AND chp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM TransferPayment tp  WHERE tp.paymentDetails.id  = pa.payment.id AND tp.deleted  = true)")
    BigDecimal sumAppliedToDocument(@Param("documentId") Long documentId);

    /**
     * Sum of on-account amounts (saldo a favor) currently parked at the supplier across
     * all non-deleted payments. The actual <em>available</em> balance still has to subtract
     * whatever was later consumed against new invoices, but consumption is also persisted
     * as PaymentApplication rows on the same payment, so the gross figure is enough for the
     * UI to show the disponible.
     */
    @Query("SELECT COALESCE(SUM(pd.onAccountAmount), 0) " +
            "FROM PSG.backEnd.model.entity.payment.PaymentDetails pd " +
            "WHERE pd.supplier.id = :supplierId " +
            "AND NOT EXISTS (SELECT 1 FROM CashPayment     cp  WHERE cp.paymentDetails.id  = pd.id AND cp.deleted  = true) " +
            "AND NOT EXISTS (SELECT 1 FROM CheckPayment    chp WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM TransferPayment tp  WHERE tp.paymentDetails.id  = pd.id AND tp.deleted  = true)")
    BigDecimal sumOnAccountBySupplier(@Param("supplierId") Long supplierId);

    void deleteByPayment_Id(Long paymentDetailsId);
}
