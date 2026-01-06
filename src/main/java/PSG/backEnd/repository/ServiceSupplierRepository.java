package PSG.backEnd.repository;

import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import PSG.backEnd.model.enums.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceSupplierRepository extends JpaRepository<ServiceSupplier, Long> {
    List<ServiceSupplier> findByDeletedFalse();
    Optional<ServiceSupplier> findByIdAndDeletedFalse(Long id);
    boolean existsByIdAndDeletedFalse(Long id);
    List<ServiceSupplier> findBySupplierIdAndDeletedFalse(Long supplierId);
    Optional<ServiceSupplier> findBySupplierIdAndDeletedTrue(Long supplierId);
    boolean existsBySupplierIdAndDeletedFalse(Long supplierId);

    @Query("SELECT DISTINCT ss FROM ServiceSupplier ss " +
            "JOIN ss.supplier s " +
            "LEFT JOIN ss.providedServices ps " +
            "WHERE (:supplierName IS NULL OR " +
            "       LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')) OR " +
            "       LOWER(CAST(s.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
            "AND (:serviceType IS NULL OR :serviceType IN (SELECT ps2 FROM ss.providedServices ps2)) " +
            "AND (:cuit IS NULL OR s.cuit LIKE %:cuit%) " +
            "AND ss.deleted = false")
    Page<ServiceSupplier> findAllWithFilters(
            @Param("supplierName") String supplierName,
            @Param("serviceType") ServiceType serviceType,
            @Param("cuit") String cuit,
            Pageable pageable
    );
}

