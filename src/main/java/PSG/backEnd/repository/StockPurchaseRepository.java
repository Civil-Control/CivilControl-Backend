package PSG.backEnd.repository;

import PSG.backEnd.model.entity.StockPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockPurchaseRepository extends JpaRepository<StockPurchase, Long> {

    List<StockPurchase> findByTransactionalDocumentId(Long transactionalDocumentId);

    @Query("SELECT sp FROM StockPurchase sp " +
            "LEFT JOIN Stock s ON sp.stockId = s.id " +
            "WHERE (CAST(:dateFrom AS date) IS NULL OR sp.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR sp.date <= :dateTo) " +
            "AND (CAST(:stockId AS long) IS NULL OR sp.stockId = :stockId) " +
            "AND (:stockName IS NULL OR LOWER(CAST(s.name AS string)) LIKE LOWER(CONCAT('%', CAST(:stockName AS string), '%'))) " +
            "AND (:stockCategory IS NULL OR LOWER(CAST(s.stockCategory AS string)) LIKE LOWER(CONCAT('%', CAST(:stockCategory AS string), '%'))) " +
            "AND (CAST(:minQuantity AS BigDecimal) IS NULL OR sp.quantity >= :minQuantity) " +
            "AND (CAST(:maxQuantity AS BigDecimal) IS NULL OR sp.quantity <= :maxQuantity) " +
            "AND (CAST(:minAmount AS BigDecimal) IS NULL OR sp.totalAmount >= :minAmount) " +
            "AND (CAST(:maxAmount AS BigDecimal) IS NULL OR sp.totalAmount <= :maxAmount) " +
            "AND (CAST(:transactionalDocumentId AS long) IS NULL OR sp.transactionalDocumentId = :transactionalDocumentId) " +
            "AND (:unlinked = false OR sp.transactionalDocumentId IS NULL) " +
            "AND (:search IS NULL OR LOWER(CAST(s.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<StockPurchase> findAllWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("stockId") Long stockId,
            @Param("stockName") String stockName,
            @Param("stockCategory") String stockCategory,
            @Param("minQuantity") BigDecimal minQuantity,
            @Param("maxQuantity") BigDecimal maxQuantity,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("transactionalDocumentId") Long transactionalDocumentId,
            @Param("search") String search,
            @Param("unlinked") boolean unlinked,
            Pageable pageable
    );
}
