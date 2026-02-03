package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.enums.StockCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByDeletedFalse();
    Optional<Stock> findByIdAndDeletedFalse(Long id);
    Optional<Stock> findByNameAndDeletedTrue(String name);
    boolean existsByNameAndDeletedFalse(String name);
    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT s FROM Stock s " +
            "WHERE (:name IS NULL OR LOWER(CAST(s.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
            "AND (CAST(:buildingId AS long) IS NULL OR s.building.id = :buildingId) " +
            "AND (CAST(:stockCategory AS string) IS NULL OR s.stockCategory = :stockCategory) " +
            "AND (CAST(:minQuantity AS BigDecimal) IS NULL OR s.quantity >= :minQuantity) " +
            "AND (CAST(:maxQuantity AS BigDecimal) IS NULL OR s.quantity <= :maxQuantity) " +
            "AND s.deleted = false")
    Page<Stock> findAllWithFilters(
            @Param("name") String name,
            @Param("buildingId") Long buildingId,
            @Param("stockCategory") StockCategory stockCategory,
            @Param("minQuantity") BigDecimal minQuantity,
            @Param("maxQuantity") BigDecimal maxQuantity,
            Pageable pageable
    );
}

