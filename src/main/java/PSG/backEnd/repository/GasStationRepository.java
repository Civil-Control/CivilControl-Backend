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
    List<GasStation> findBySupplierIdAndDeletedFalse(Long supplierId);

    @Query("SELECT DISTINCT gs FROM GasStation gs " +
            "LEFT JOIN gs.prices p " +
            "WHERE (:supplierId IS NULL OR gs.supplierId = :supplierId) " +
            "AND (:fuelTypes IS NULL OR " +
            "     EXISTS (SELECT p2 FROM gs.prices p2 WHERE CAST(p2.fuelType AS string) IN :fuelTypes)) " +
            "AND gs.deleted = false")
    Page<GasStation> findAllWithFilters(
            @Param("supplierId") Long supplierId,
            @Param("fuelTypes") List<String> fuelTypes,
            Pageable pageable
    );
}
