package PSG.backEnd.repository;

import PSG.backEnd.model.enums.documents.DocumentType;
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
            SELECT td FROM TransactionalDocument td 
            JOIN td.supplier s
            LEFT JOIN td.projectArea pa
            WHERE (:documentNumber IS NULL OR td.documentNumber LIKE %:documentNumber%)
            AND (:documentType IS NULL OR td.documentType = :documentType)
            AND (:supplierCuit IS NULL OR s.cuit LIKE %:supplierCuit%)
            AND (:supplierName IS NULL OR 
                 LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')) OR
                 LOWER(CAST(s.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))
            )
            AND (CAST(:projectAreaId AS long) IS NULL OR pa.id = :projectAreaId)
            AND (:projectAreaName IS NULL OR 
                 LOWER(CAST(pa.name AS string)) LIKE LOWER(CONCAT('%', CAST(:projectAreaName AS string), '%'))
            )
            AND (CAST(:maxTotalAmount AS BigDecimal) IS NULL OR td.total <= :maxTotalAmount)
            AND (CAST(:minTotalAmount AS BigDecimal) IS NULL OR td.total >= :minTotalAmount)
            AND (CAST(:totalAmount AS BigDecimal) IS NULL OR td.total = :totalAmount)
            AND (CAST(:fromDate AS date) IS NULL OR td.date >= :fromDate)
            AND (CAST(:toDate AS date) IS NULL OR td.date <= :toDate)
            AND (:paid IS NULL OR td.paid = :paid)
            AND (:search IS NULL OR (
                LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                LOWER(CAST(s.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                td.documentNumber LIKE CONCAT('%', CAST(:search AS string), '%')
            ))
            AND td.deleted = false
           """)
    Page<TransactionalDocument> findAllWithFilters(
            @Param("documentNumber") String documentNumber,
            @Param("documentType") DocumentType documentType,
            @Param("supplierCuit") String supplierCuit,
            @Param("supplierName") String supplierName,
            @Param("projectAreaId") Long projectAreaId,
            @Param("projectAreaName") String projectAreaName,
            @Param("maxTotalAmount") BigDecimal maxTotalAmount,
            @Param("minTotalAmount") BigDecimal minTotalAmount,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("paid") Boolean paid,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(td.total), 0) FROM TransactionalDocument td " +
           "WHERE td.supplier.id = :supplierId AND td.deleted = false " +
           "AND (CAST(:fromDate AS date) IS NULL OR td.date >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR td.date <= :toDate)")
    BigDecimal sumTotalBySupplierId(@Param("supplierId") Long supplierId,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(td.total), 0) FROM TransactionalDocument td " +
           "WHERE td.supplier.id = :supplierId AND td.deleted = false AND td.paid = true " +
           "AND (CAST(:fromDate AS date) IS NULL OR td.date >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR td.date <= :toDate)")
    BigDecimal sumPaidBySupplierId(@Param("supplierId") Long supplierId,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate);

    /**
     * Sums the total of all invoices and debit notes for a supplier (positive ledger entries).
     * Used by the supplier stats: this is the "Facturado" figure that should not include credit notes.
     */
    @Query("SELECT COALESCE(SUM(td.total), 0) FROM TransactionalDocument td " +
           "WHERE td.supplier.id = :supplierId AND td.deleted = false " +
           "AND td.documentType IN (PSG.backEnd.model.enums.documents.DocumentType.BILL_A, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.BILL_B, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.BILL_C, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.DEBIT_NOTE_A, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.DEBIT_NOTE_B, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.DEBIT_NOTE_C) " +
           "AND (CAST(:fromDate AS date) IS NULL OR td.date >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR td.date <= :toDate)")
    BigDecimal sumInvoicedBySupplierId(@Param("supplierId") Long supplierId,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate);

    /**
     * Sums the total of paid invoices/debit notes only. Excludes credit notes which never use the paid flag.
     */
    @Query("SELECT COALESCE(SUM(td.total), 0) FROM TransactionalDocument td " +
           "WHERE td.supplier.id = :supplierId AND td.deleted = false AND td.paid = true " +
           "AND td.documentType IN (PSG.backEnd.model.enums.documents.DocumentType.BILL_A, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.BILL_B, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.BILL_C, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.DEBIT_NOTE_A, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.DEBIT_NOTE_B, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.DEBIT_NOTE_C) " +
           "AND (CAST(:fromDate AS date) IS NULL OR td.date >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR td.date <= :toDate)")
    BigDecimal sumPaidInvoicedBySupplierId(@Param("supplierId") Long supplierId,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate);

    /**
     * Sums the total of all credit notes for a supplier (positive figure representing money credited back).
     */
    @Query("SELECT COALESCE(SUM(td.total), 0) FROM TransactionalDocument td " +
           "WHERE td.supplier.id = :supplierId AND td.deleted = false " +
           "AND td.documentType IN (PSG.backEnd.model.enums.documents.DocumentType.CREDIT_NOTE_A, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.CREDIT_NOTE_B, " +
           "                        PSG.backEnd.model.enums.documents.DocumentType.CREDIT_NOTE_C) " +
           "AND (CAST(:fromDate AS date) IS NULL OR td.date >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR td.date <= :toDate)")
    BigDecimal sumCreditedBySupplierId(@Param("supplierId") Long supplierId,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate);
}
