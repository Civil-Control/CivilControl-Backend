package PSG.backEnd.repository.PaymentRepository;

import PSG.backEnd.model.entity.payment.PaymentDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentDetails, Long> {

    /**
     * Fetches a PaymentDetails by ID eagerly loading all payment subtype relations
     * (cashPayment, transferPayment, checkPayment) and supplier in a single query.
     * This avoids LazyInitializationException when resolving the payment type.
     */
    @Query("SELECT pd FROM PaymentDetails pd " +
            "LEFT JOIN FETCH pd.cashPayment " +
            "LEFT JOIN FETCH pd.transferPayment " +
            "LEFT JOIN FETCH pd.checkPayment " +
            "LEFT JOIN FETCH pd.supplier " +
            "WHERE pd.id = :id")
    Optional<PaymentDetails> findByIdWithPaymentType(@Param("id") Long id);

    @Query("SELECT pd FROM PaymentDetails pd " +
            "LEFT JOIN FETCH pd.supplier " +
            "WHERE " +
            "(:paymentMethod IS NULL OR " +
            "  ((:paymentMethod = 'CASH'     AND EXISTS (SELECT 1 FROM CashPayment cp     WHERE cp.paymentDetails.id = pd.id)) OR " +
            "   (:paymentMethod = 'CHECK'    AND EXISTS (SELECT 1 FROM CheckPayment chp   WHERE chp.paymentDetails.id = pd.id)) OR " +
            "   (:paymentMethod = 'TRANSFER' AND EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id)))) AND " +
            "(:startDate IS NULL OR pd.paymentDate >= :startDate) AND " +
            "(:endDate IS NULL OR pd.paymentDate <= :endDate) AND " +
            "(:minAmount IS NULL OR pd.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR pd.amount <= :maxAmount) AND " +
            "(:amount IS NULL OR LOWER(CAST(pd.amount AS string)) LIKE LOWER(CONCAT('%', CAST(:amount AS string), '%'))) AND " +
            "(:transactionNumber IS NULL OR " +
            "  (EXISTS (SELECT 1 FROM CheckPayment chp WHERE chp.paymentDetails.id = pd.id " +
            "           AND LOWER(chp.checkNumber) LIKE LOWER(CONCAT('%', CAST(:transactionNumber AS string), '%'))) OR " +
            "   EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id " +
            "           AND LOWER(tp.transactionNumber) LIKE LOWER(CONCAT('%', CAST(:transactionNumber AS string), '%'))))) AND " +
            "(:supplierName IS NULL OR LOWER(pd.supplier.legalName) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')) OR LOWER(pd.supplier.tradeName) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) AND " +
            "(:supplierId IS NULL OR pd.supplier.id = :supplierId) AND " +
            "(:search IS NULL OR " +
            "  LOWER(pd.supplier.legalName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "  LOWER(pd.supplier.tradeName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "  LOWER(CAST(pd.amount AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND " +
            "(NOT EXISTS (SELECT 1 FROM CashPayment cp WHERE cp.paymentDetails.id = pd.id AND cp.deleted = true) AND " +
            " NOT EXISTS (SELECT 1 FROM CheckPayment chp WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) AND " +
            " NOT EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.deleted = true))")
    Page<PaymentDetails> findAllWithFilters(
            @Param("paymentMethod") String paymentMethod,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("transactionNumber") String transactionNumber,
            @Param("supplierName") String supplierName,
            @Param("supplierId") Long supplierId,
            @Param("amount") String amount,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Returns the PaymentDetails ID of the (non-deleted) payment that includes
     * the given document among its paid-document references, if any.
     */
    @Query("SELECT pd.id FROM PaymentDetails pd " +
            "JOIN pd.paidDocuments doc " +
            "WHERE doc.id = :documentId " +
            "AND NOT EXISTS (SELECT 1 FROM CashPayment cp     WHERE cp.paymentDetails.id = pd.id AND cp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM CheckPayment chp   WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.deleted = true)")
    Optional<Long> findPaymentIdByDocumentId(@Param("documentId") Long documentId);

    /**
     * Returns the payment method display name for the payment linked to the given document.
     */
    @Query("SELECT CASE " +
            "WHEN EXISTS (SELECT 1 FROM CashPayment cp WHERE cp.paymentDetails.id = pd.id AND (cp.deleted = false OR cp.deleted IS NULL)) THEN 'efectivo' " +
            "WHEN EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND (tp.deleted = false OR tp.deleted IS NULL)) THEN 'transferencia' " +
            "WHEN EXISTS (SELECT 1 FROM CheckPayment chp WHERE chp.paymentDetails.id = pd.id AND (chp.deleted = false OR chp.deleted IS NULL)) THEN 'cheque' " +
            "ELSE NULL END " +
            "FROM PaymentDetails pd " +
            "JOIN pd.paidDocuments doc " +
            "WHERE doc.id = :documentId " +
            "AND NOT EXISTS (SELECT 1 FROM CashPayment cp WHERE cp.paymentDetails.id = pd.id AND cp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM CheckPayment chp WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.deleted = true)")
    Optional<String> findPaymentMethodByDocumentId(@Param("documentId") Long documentId);

    /**
     * Sums the total amount of all (non-deleted) payments registered for the given supplier
     * within the optional date range. Aggregates over the {@link PaymentDetails#getAmount() amount}
     * of every payment regardless of its concrete subtype (cash, transfer, check).
     *
     * <p>This is the canonical "total paid to supplier" figure: it reflects the actual cash
     * outflow toward the supplier and is independent of how those payments are imputed to
     * invoices. In particular, it correctly accounts for payment amounts that differ from
     * the sum of attached invoice totals (e.g. a credit note applied as a discount on the
     * payment without canceling any specific invoice).
     */
    @Query("SELECT COALESCE(SUM(pd.amount), 0) FROM PaymentDetails pd " +
            "WHERE pd.supplier.id = :supplierId " +
            "AND (CAST(:fromDate AS date) IS NULL OR pd.paymentDate >= :fromDate) " +
            "AND (CAST(:toDate AS date) IS NULL OR pd.paymentDate <= :toDate) " +
            "AND NOT EXISTS (SELECT 1 FROM CashPayment cp     WHERE cp.paymentDetails.id = pd.id AND cp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM CheckPayment chp   WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.deleted = true)")
    BigDecimal sumAmountBySupplierId(@Param("supplierId") Long supplierId,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate);

    // ════════════════════════════════════════════════════════════════════════
    // Supplier current-account report queries
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Sums the amount of all (non-deleted) payments registered for the given supplier
     * strictly before the given date. Used to compute the previous balance
     * (saldo anterior) of the supplier current-account report.
     */
    @Query("SELECT COALESCE(SUM(pd.amount), 0) FROM PaymentDetails pd " +
            "WHERE pd.supplier.id = :supplierId " +
            "AND pd.paymentDate < :beforeDate " +
            "AND NOT EXISTS (SELECT 1 FROM CashPayment cp     WHERE cp.paymentDetails.id = pd.id AND cp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM CheckPayment chp   WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.deleted = true)")
    BigDecimal sumAmountBySupplierIdBeforeDate(@Param("supplierId") Long supplierId,
                                               @Param("beforeDate") LocalDate beforeDate);

    /**
     * Returns all non-deleted payments for the given suppliers within the date range,
     * fetching supplier and payment-method subtypes eagerly for use in the
     * supplier current-account report timeline construction.
     */
    @Query("SELECT DISTINCT pd FROM PaymentDetails pd " +
            "LEFT JOIN FETCH pd.supplier " +
            "LEFT JOIN FETCH pd.cashPayment " +
            "LEFT JOIN FETCH pd.transferPayment " +
            "LEFT JOIN FETCH pd.checkPayment " +
            "WHERE pd.supplier.id IN :supplierIds " +
            "AND pd.paymentDate >= :fromDate AND pd.paymentDate <= :toDate " +
            "AND NOT EXISTS (SELECT 1 FROM CashPayment cp     WHERE cp.paymentDetails.id = pd.id AND cp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM CheckPayment chp   WHERE chp.paymentDetails.id = pd.id AND chp.deleted = true) " +
            "AND NOT EXISTS (SELECT 1 FROM TransferPayment tp WHERE tp.paymentDetails.id = pd.id AND tp.deleted = true)")
    List<PaymentDetails> findAllBySupplierIdInAndPaymentDateBetween(
            @Param("supplierIds") Collection<Long> supplierIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}

