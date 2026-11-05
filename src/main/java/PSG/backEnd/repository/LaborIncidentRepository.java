package PSG.backEnd.repository;

import PSG.backEnd.model.entity.employee.LaborIncident;
import PSG.backEnd.model.enums.employee.LaborIncidentStatus;
import PSG.backEnd.model.enums.employee.LaborIncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface LaborIncidentRepository extends JpaRepository<LaborIncident, Long> {

    @Query(
        value =
            "SELECT DISTINCT li FROM LaborIncident li LEFT JOIN li.employees e " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR e.id = :employeeId) " +
            "AND (:employeeSearch IS NULL OR " +
            "     LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')) " +
            "     OR LOWER(CAST(e.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%'))) " +
            "AND (:incidentType IS NULL OR li.incidentType = :incidentType) " +
            "AND (:status IS NULL OR li.status = :status) " +
            "AND (CAST(:incidentDateFrom AS date) IS NULL OR li.incidentDate >= :incidentDateFrom) " +
            "AND (CAST(:incidentDateTo AS date) IS NULL OR li.incidentDate <= :incidentDateTo) " +
            "AND (:hasFinancialImpact IS NULL OR " +
            "     (:hasFinancialImpact = true AND li.financialImpact IS NOT NULL) OR " +
            "     (:hasFinancialImpact = false AND li.financialImpact IS NULL))",
        countQuery =
            "SELECT COUNT(DISTINCT li) FROM LaborIncident li LEFT JOIN li.employees e " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR e.id = :employeeId) " +
            "AND (:employeeSearch IS NULL OR " +
            "     LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')) " +
            "     OR LOWER(CAST(e.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%'))) " +
            "AND (:incidentType IS NULL OR li.incidentType = :incidentType) " +
            "AND (:status IS NULL OR li.status = :status) " +
            "AND (CAST(:incidentDateFrom AS date) IS NULL OR li.incidentDate >= :incidentDateFrom) " +
            "AND (CAST(:incidentDateTo AS date) IS NULL OR li.incidentDate <= :incidentDateTo) " +
            "AND (:hasFinancialImpact IS NULL OR " +
            "     (:hasFinancialImpact = true AND li.financialImpact IS NOT NULL) OR " +
            "     (:hasFinancialImpact = false AND li.financialImpact IS NULL))"
    )
    Page<LaborIncident> findAllWithFilters(
            @Param("employeeId")         Long employeeId,
            @Param("employeeSearch")     String employeeSearch,
            @Param("incidentType")       LaborIncidentType incidentType,
            @Param("status")             LaborIncidentStatus status,
            @Param("incidentDateFrom")   LocalDate incidentDateFrom,
            @Param("incidentDateTo")     LocalDate incidentDateTo,
            @Param("hasFinancialImpact") Boolean hasFinancialImpact,
            Pageable pageable
    );
}
