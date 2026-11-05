package PSG.backEnd.repository;

import PSG.backEnd.model.entity.gasStation.GasStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GasStationRepository extends JpaRepository<GasStation, Long> {
    List<GasStation> findByDeletedFalse();
    Optional<GasStation> findByIdAndDeletedFalse(Long id);
    boolean existsByIdAndDeletedFalse(Long id);
    List<GasStation> findBySupplier_IdAndDeletedFalse(Long supplierId);

    @Query("SELECT DISTINCT gs FROM GasStation gs " +
            "LEFT JOIN FETCH gs.supplier s " +
            "LEFT JOIN gs.prices p " +
            "WHERE (:supplierId IS NULL OR gs.supplier.id = :supplierId) " +
            "AND (:supplierName IS NULL OR LOWER(CAST(gs.supplier.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')) " +
            "     OR :supplierName IS NULL OR LOWER(CAST(gs.supplier.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
            "AND (:supplierTradeName IS NULL OR LOWER(CAST(gs.supplier.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierTradeName AS string), '%'))) " +
            "AND (:supplierCuit IS NULL OR LOWER(CAST(gs.supplier.cuit AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierCuit AS string), '%'))) " +
            "AND (:supplierActive IS NULL OR gs.supplier.active = :supplierActive) " +
            "AND (:fuelTypes IS NULL OR " +
            "     EXISTS (SELECT p2 FROM gs.prices p2 WHERE CAST(p2.fuelType AS string) IN :fuelTypes)) " +
            "AND gs.deleted = false")
    Page<GasStation> findAllWithFilters(
            @Param("supplierId") Long supplierId,
            @Param("supplierName") String supplierName,
            @Param("fuelTypes") List<String> fuelTypes,
            @Param("supplierTradeName") String supplierTradeName,
            @Param("supplierCuit") String supplierCuit,
            @Param("supplierActive") Boolean supplierActive,
            Pageable pageable
    );
}
