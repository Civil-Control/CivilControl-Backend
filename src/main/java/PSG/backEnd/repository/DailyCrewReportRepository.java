package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.DailyCrewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCrewReportRepository extends JpaRepository<DailyCrewReport, Long> {

    Optional<DailyCrewReport> findByIdAndDeletedFalse(Long id);

    @Query("SELECT r FROM DailyCrewReport r " +
           "LEFT JOIN FETCH r.projectArea " +
           "WHERE r.date = :date AND r.deleted = false " +
           "ORDER BY r.type ASC, r.projectArea.name ASC")
    List<DailyCrewReport> findByDateAndDeletedFalse(@Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE DailyCrewReport r SET r.deleted = true WHERE r.id = :id AND r.deleted = false")
    int softDeleteById(@Param("id") Long id);

    @Query("SELECT r.date, COUNT(DISTINCT r.id), " +
           "(SELECT COUNT(ca.id) FROM CrewAssignment ca WHERE ca.crewReport.id IN " +
           "  (SELECT r2.id FROM DailyCrewReport r2 WHERE r2.date = r.date AND r2.deleted = false) " +
           "  AND ca.deleted = false), " +
           "(SELECT COUNT(DISTINCT ca2.vehicle.id) FROM CrewAssignment ca2 WHERE ca2.crewReport.id IN " +
           "  (SELECT r3.id FROM DailyCrewReport r3 WHERE r3.date = r.date AND r3.deleted = false) " +
           "  AND ca2.deleted = false) " +
           "FROM DailyCrewReport r " +
           "WHERE r.deleted = false AND r.date BETWEEN :startDate AND :endDate " +
           "GROUP BY r.date ORDER BY r.date")
    List<Object[]> countByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
