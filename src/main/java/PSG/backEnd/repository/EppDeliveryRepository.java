package PSG.backEnd.repository;

import PSG.backEnd.model.entity.employee.EppDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EppDeliveryRepository extends JpaRepository<EppDelivery, Long> {

    List<EppDelivery> findByEmployeeIdAndDeletedFalse(Long employeeId);

    Optional<EppDelivery> findByIdAndDeletedFalse(Long id);

    @Query("SELECT ed FROM EppDelivery ed " +
            "WHERE ed.deleted = false " +
            "AND ed.employee.deleted = false " +
            "AND (CAST(:employeeId AS long) IS NULL OR ed.employee.id = :employeeId) " +
            "AND (:employeeSearch IS NULL OR (LOWER(CAST(ed.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')) " +
            "     OR LOWER(CAST(ed.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')))) " +
            "AND (CAST(:deliveryDateFrom AS date) IS NULL OR ed.deliveryDate >= :deliveryDateFrom) " +
            "AND (CAST(:deliveryDateTo AS date) IS NULL OR ed.deliveryDate <= :deliveryDateTo) " +
            "AND (:itemName IS NULL OR LOWER(CAST(ed.itemName AS string)) LIKE LOWER(CONCAT('%', CAST(:itemName AS string), '%'))) " +
            "AND (:itemType IS NULL OR LOWER(CAST(ed.itemType AS string)) LIKE LOWER(CONCAT('%', CAST(:itemType AS string), '%'))) " +
            "AND (:brand IS NULL OR LOWER(CAST(ed.brand AS string)) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%')))")
    Page<EppDelivery> findAllWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("employeeSearch") String employeeSearch,
            @Param("deliveryDateFrom") LocalDate deliveryDateFrom,
            @Param("deliveryDateTo") LocalDate deliveryDateTo,
            @Param("itemName") String itemName,
            @Param("itemType") String itemType,
            @Param("brand") String brand,
            Pageable pageable
    );
}

