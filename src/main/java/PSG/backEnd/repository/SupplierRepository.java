package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Supplier;
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
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByDeletedFalse();
    Optional<Supplier> findByIdAndDeletedFalse(Long id);
    Optional<Supplier> findByCuitAndDeletedTrue(String cuit);
    Optional<Supplier> findByLegalNameAndDeletedTrue(String legalName);
    boolean existsByCuitAndDeletedFalse(String cuit);
    boolean existsByLegalNameAndDeletedFalse(String legalName);
    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT s FROM Supplier s " +
            "WHERE (:cuit IS NULL OR s.cuit LIKE %:cuit%) " +
            "AND (:legalName IS NULL OR LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:legalName AS string), '%'))) " +
            "AND (:tradeName IS NULL OR LOWER(CAST(s.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:tradeName AS string), '%'))) " +
            "AND (:city IS NULL OR LOWER(CAST(s.address.city AS string)) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%'))) " +
            "AND (CAST(:minDiscountPercentage AS BigDecimal) IS NULL OR s.defaultDiscountPercentage >= :minDiscountPercentage) " +
            "AND (CAST(:maxDiscountPercentage AS BigDecimal) IS NULL OR s.defaultDiscountPercentage <= :maxDiscountPercentage) " +
            "AND (:active IS NULL OR s.active = :active) " +
            "AND s.deleted = false " +
            "AND (:search IS NULL OR (s.cuit LIKE CONCAT('%', :search, '%') " +
            "     OR LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(s.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<Supplier> findAllWithFilters(
            @Param("cuit") String cuit,
            @Param("legalName") String legalName,
            @Param("tradeName") String tradeName,
            @Param("city") String city,
            @Param("minDiscountPercentage") BigDecimal minDiscountPercentage,
            @Param("maxDiscountPercentage") BigDecimal maxDiscountPercentage,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
