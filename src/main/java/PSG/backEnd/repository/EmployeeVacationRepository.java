package PSG.backEnd.repository;
import PSG.backEnd.model.entity.employee.EmployeeVacation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
@Repository
public interface EmployeeVacationRepository extends JpaRepository<EmployeeVacation, Long> {
    Optional<EmployeeVacation> findByIdAndDeletedFalse(Long id);
    @Query("SELECT ev FROM EmployeeVacation ev " +
            "WHERE ev.deleted = false " +
            "AND ev.employee.deleted = false " +
            "AND (:employeeId IS NULL OR ev.employee.id = :employeeId) " +
            "AND (:employeeLastName IS NULL OR LOWER(CAST(ev.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeLastName AS string), '%'))) " +
            "AND (:startDateFrom IS NULL OR ev.startDate >= :startDateFrom) " +
            "AND (:startDateTo IS NULL OR ev.startDate <= :startDateTo) " +
            "AND (:endDateFrom IS NULL OR ev.endDate >= :endDateFrom) " +
            "AND (:endDateTo IS NULL OR ev.endDate <= :endDateTo) " +
            "AND (:minTotalDays IS NULL OR ev.totalDays >= :minTotalDays) " +
            "AND (:maxTotalDays IS NULL OR ev.totalDays <= :maxTotalDays)")
    Page<EmployeeVacation> findAllWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("employeeLastName") String employeeLastName,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            @Param("endDateFrom") LocalDate endDateFrom,
            @Param("endDateTo") LocalDate endDateTo,
            @Param("minTotalDays") Integer minTotalDays,
            @Param("maxTotalDays") Integer maxTotalDays,
            Pageable pageable
    );
    @Query("SELECT COUNT(ev) > 0 FROM EmployeeVacation ev " +
            "WHERE ev.deleted = false " +
            "AND ev.employee.deleted = false " +
            "AND ev.employee.id = :employeeId " +
            "AND (CAST(:excludeId AS long) IS NULL OR ev.id <> :excludeId) " +
            "AND ev.startDate <= :endDate " +
            "AND ev.endDate >= :startDate")
    boolean existsOverlappingVacation(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );
}
