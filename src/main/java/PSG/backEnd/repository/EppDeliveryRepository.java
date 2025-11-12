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
            "AND (:employeeId IS NULL OR ed.employee.id = :employeeId) " +
            "AND (:deliveryDateFrom IS NULL OR ed.deliveryDate >= :deliveryDateFrom) " +
            "AND (:deliveryDateTo IS NULL OR ed.deliveryDate <= :deliveryDateTo) " +
            "AND (:itemName IS NULL OR LOWER(ed.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))) " +
            "AND (:itemType IS NULL OR LOWER(ed.itemType) LIKE LOWER(CONCAT('%', :itemType, '%'))) " +
            "AND (:brand IS NULL OR LOWER(ed.brand) LIKE LOWER(CONCAT('%', :brand, '%')))")
    Page<EppDelivery> findAllWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("deliveryDateFrom") LocalDate deliveryDateFrom,
            @Param("deliveryDateTo") LocalDate deliveryDateTo,
            @Param("itemName") String itemName,
            @Param("itemType") String itemType,
            @Param("brand") String brand,
            Pageable pageable
    );
}

