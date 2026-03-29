package PSG.backEnd.repository;

import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SalesDocumentRepository extends JpaRepository<SalesDocument, Long> {

    Optional<SalesDocument> findByIdAndDeletedFalse(Long id);

    @Query("SELECT sd FROM SalesDocument sd " +
           "WHERE (:clientId IS NULL OR sd.client.id = :clientId) " +
           "AND (:documentType IS NULL OR sd.documentType = :documentType) " +
           "AND (:documentNumber IS NULL OR LOWER(CAST(sd.documentNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:documentNumber AS string), '%'))) " +
           "AND (CAST(:dateFrom AS LocalDate) IS NULL OR sd.date >= :dateFrom) " +
           "AND (CAST(:dateTo AS LocalDate) IS NULL OR sd.date <= :dateTo) " +
           "AND (CAST(:minTotal AS BigDecimal) IS NULL OR sd.total >= :minTotal) " +
           "AND (CAST(:maxTotal AS BigDecimal) IS NULL OR sd.total <= :maxTotal) " +
           "AND (:paid IS NULL OR sd.paid = :paid) " +
           "AND (:projectAreaId IS NULL OR sd.projectArea.id = :projectAreaId) " +
           "AND sd.deleted = false " +
           "AND (:search IS NULL OR (" +
           "     LOWER(CAST(sd.documentNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "     OR LOWER(CAST(sd.client.businessName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<SalesDocument> findAllWithFilters(
            @Param("clientId") Long clientId,
            @Param("documentType") SalesDocumentType documentType,
            @Param("documentNumber") String documentNumber,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("minTotal") BigDecimal minTotal,
            @Param("maxTotal") BigDecimal maxTotal,
            @Param("paid") Boolean paid,
            @Param("projectAreaId") Long projectAreaId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(sd.total), 0) FROM SalesDocument sd " +
           "WHERE sd.client.id = :clientId AND sd.deleted = false")
    BigDecimal sumTotalByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COALESCE(SUM(sd.total), 0) FROM SalesDocument sd " +
           "WHERE sd.client.id = :clientId AND sd.deleted = false AND sd.paid = true")
    BigDecimal sumCollectedByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COALESCE(SUM(sd.total), 0) FROM SalesDocument sd " +
           "WHERE sd.client.id = :clientId AND sd.deleted = false " +
           "AND (CAST(:fromDate AS date) IS NULL OR sd.date >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR sd.date <= :toDate)")
    BigDecimal sumTotalByClientIdAndDateRange(@Param("clientId") Long clientId,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(sd.total), 0) FROM SalesDocument sd " +
           "WHERE sd.client.id = :clientId AND sd.deleted = false AND sd.paid = true " +
           "AND (CAST(:fromDate AS date) IS NULL OR sd.date >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR sd.date <= :toDate)")
    BigDecimal sumCollectedByClientIdAndDateRange(@Param("clientId") Long clientId,
                                                  @Param("fromDate") LocalDate fromDate,
                                                  @Param("toDate") LocalDate toDate);
}
