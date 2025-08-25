package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionalDocumentRepository extends JpaRepository<TransactionalDocument, Long> {
    @Query("SELECT td FROM TransactionalDocument td JOIN FETCH td.supplier WHERE td.deleted = false")
    List<TransactionalDocument> findByDeletedFalse();

    @Query("SELECT td FROM TransactionalDocument td JOIN FETCH td.supplier WHERE td.id = :id AND td.deleted = false")
    Optional<TransactionalDocument> findByIdAndDeletedFalse(@Param("id") Long id);

    Optional<TransactionalDocument> findByBranchCodeAndDocumentNumberAndDeletedTrue(String branchCode, String documentNumber);
    boolean existsByBranchCodeAndDocumentNumberAndSupplierIdAndDeletedFalse(String branchCode, String documentNumber, Long supplierId);

    @Query("""
            SELECT td FROM TransactionalDocument td JOIN FETCH td.supplier
            WHERE (:documentNumber IS NULL OR td.documentNumber LIKE %:documentNumber%)
            AND (:supplierCuit IS NULL OR td.supplier.cuit LIKE %:supplierCuit%)
            AND (:supplierName IS NULL OR 
                 LOWER(td.supplier.legalName) LIKE LOWER(CONCAT('%', :supplierName, '%')) OR
                 LOWER(td.supplier.tradeName) LIKE LOWER(CONCAT('%', :supplierName, '%'))
            )
            AND (:maxTotalAmount IS NULL OR td.total <= :maxTotalAmount)
            AND (:minTotalAmount IS NULL OR td.total >= :minTotalAmount)
            AND (:totalAmount IS NULL OR td.total = :totalAmount)
            AND (:fromDate IS NULL OR td.date >= :fromDate)
            AND (:toDate IS NULL OR td.date <= :toDate)
            AND (:paid IS NULL OR td.paid = :paid)
            AND td.deleted = false
           """)
    Page<TransactionalDocument> findAllWithFilters(
            @Param("documentNumber") String documentNumber,
            @Param("supplierCuit") String supplierCuit,
            @Param("supplierName") String supplierName,
            @Param("maxTotalAmount") BigDecimal maxTotalAmount,
            @Param("minTotalAmount") BigDecimal minTotalAmount,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("paid") Boolean paid,
            Pageable pageable
    );
}
