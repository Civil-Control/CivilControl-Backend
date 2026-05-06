package PSG.backEnd.repository;

import PSG.backEnd.model.entity.employee.DisciplinaryAction;
import PSG.backEnd.model.enums.employee.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisciplinaryActionRepository extends JpaRepository<DisciplinaryAction, Long> {
    List<DisciplinaryAction> findByEmployeeId(Long employeeId);
    @Query("SELECT da FROM DisciplinaryAction da " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR da.employee.id = :employeeId) " +
            "AND (:employeeSearch IS NULL OR (LOWER(CAST(da.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')) " +
            "     OR LOWER(CAST(da.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')))) " +
            "AND (:actionType IS NULL OR da.actionType = :actionType) " +
            "AND (CAST(:actionDateFrom AS date) IS NULL OR da.actionDate >= :actionDateFrom) " +
            "AND (CAST(:actionDateTo AS date) IS NULL OR da.actionDate <= :actionDateTo) " +
            "AND (CAST(:endDateFrom AS date) IS NULL OR da.endDate >= :endDateFrom) " +
            "AND (CAST(:endDateTo AS date) IS NULL OR da.endDate <= :endDateTo)")
    Page<DisciplinaryAction> findAllWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("employeeSearch") String employeeSearch,
            @Param("actionType") ActionType actionType,
            @Param("actionDateFrom") LocalDate actionDateFrom,
            @Param("actionDateTo") LocalDate actionDateTo,
            @Param("endDateFrom") LocalDate endDateFrom,
            @Param("endDateTo") LocalDate endDateTo,
            Pageable pageable
    );
}
