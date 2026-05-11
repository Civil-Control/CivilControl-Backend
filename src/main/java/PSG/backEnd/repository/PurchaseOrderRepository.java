package PSG.backEnd.repository;

import PSG.backEnd.model.dto.purchaseOrder.PurchaseOrderFilterDTO;
import PSG.backEnd.model.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
        SELECT po FROM PurchaseOrder po
        LEFT JOIN FETCH po.createdByUser
        WHERE po.deleted = false
          AND (:#{#f.status}   IS NULL OR po.status   = :#{#f.status})
          AND (:#{#f.category} IS NULL OR po.category = :#{#f.category})
          AND (:#{#f.priority} IS NULL OR po.priority = :#{#f.priority})
          AND (:#{#f.dateFrom} IS NULL OR po.date >= :#{#f.dateFrom})
          AND (:#{#f.dateTo}   IS NULL OR po.date <= :#{#f.dateTo})
          AND (:#{#f.transactionalDocumentId} IS NULL
               OR po.transactionalDocument.id = :#{#f.transactionalDocumentId})
          AND (:#{#f.search} IS NULL
               OR LOWER(CAST(po.description AS string)) LIKE LOWER(CONCAT('%', CAST(:#{#f.search} AS string), '%'))
               OR LOWER(CAST(po.requestedBy  AS string)) LIKE LOWER(CONCAT('%', CAST(:#{#f.search} AS string), '%')))
        """)
    Page<PurchaseOrder> findAllWithFilters(
        @Param("f") PurchaseOrderFilterDTO f,
        Pageable pageable
    );

    @Query("""
        SELECT po FROM PurchaseOrder po
        WHERE po.deleted = false
          AND po.createdByUser.id = :userId
          AND po.status != PSG.backEnd.model.enums.PurchaseOrderStatus.COMPRADA
          AND (:#{#f.status}   IS NULL OR po.status   = :#{#f.status})
          AND (:#{#f.category} IS NULL OR po.category = :#{#f.category})
          AND (:#{#f.search} IS NULL
               OR LOWER(CAST(po.description AS string)) LIKE LOWER(CONCAT('%', CAST(:#{#f.search} AS string), '%'))
               OR LOWER(CAST(po.requestedBy  AS string)) LIKE LOWER(CONCAT('%', CAST(:#{#f.search} AS string), '%')))
        """)
    Page<PurchaseOrder> findAllByCreatedByUserWithFilters(
        @Param("userId") Long userId,
        @Param("f") PurchaseOrderFilterDTO f,
        Pageable pageable
    );
}
