package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.CrewAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrewAssignmentRepository extends JpaRepository<CrewAssignment, Long> {

    Optional<CrewAssignment> findByIdAndDeletedFalse(Long id);

    boolean existsByEmployeeIdAndDateAndDeletedFalse(Long employeeId, LocalDate date);

    boolean existsByVehicleIdAndDateAndDeletedFalse(Long vehicleId, LocalDate date);

    @Query("SELECT ca FROM CrewAssignment ca WHERE ca.vehicle.id = :vehicleId AND ca.date = :date AND ca.driver = true AND ca.deleted = false")
    Optional<CrewAssignment> findDriverByVehicleAndDate(
            @Param("vehicleId") Long vehicleId,
            @Param("date") LocalDate date);

    long countByVehicleIdAndDateAndDeletedFalse(Long vehicleId, LocalDate date);

    @Query("SELECT ca FROM CrewAssignment ca " +
           "WHERE ca.date = :date AND ca.deleted = false " +
           "ORDER BY ca.vehicle.licensePlate ASC, ca.employee.lastName ASC")
    List<CrewAssignment> findByDateAndDeletedFalseOrdered(@Param("date") LocalDate date);

    @Query("SELECT ca.date, COUNT(DISTINCT ca.vehicle.id), COUNT(ca.id) " +
           "FROM CrewAssignment ca " +
           "WHERE ca.deleted = false " +
           "AND ca.date BETWEEN :startDate AND :endDate " +
           "GROUP BY ca.date " +
           "ORDER BY ca.date")
    List<Object[]> countByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ca FROM CrewAssignment ca " +
           "WHERE ca.deleted = false " +
           "AND (CAST(:employeeId AS long) IS NULL OR ca.employee.id = :employeeId) " +
           "AND (:employeeName IS NULL OR LOWER(CAST(ca.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeName AS string), '%'))) " +
           "AND (:employeeLastName IS NULL OR LOWER(CAST(ca.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeLastName AS string), '%'))) " +
           "AND (:employeeDni IS NULL OR CAST(ca.employee.dni AS string) LIKE CONCAT('%', CAST(:employeeDni AS string), '%')) " +
           "AND (CAST(:vehicleId AS long) IS NULL OR ca.vehicle.id = :vehicleId) " +
           "AND (:vehicleLicensePlate IS NULL OR LOWER(CAST(ca.vehicle.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleLicensePlate AS string), '%'))) " +
           "AND (CAST(:projectAreaId AS long) IS NULL OR ca.projectArea.id = :projectAreaId) " +
           "AND (CAST(:dateFrom AS date) IS NULL OR ca.date >= :dateFrom) " +
           "AND (CAST(:dateTo AS date) IS NULL OR ca.date <= :dateTo) " +
           "AND (CAST(:dateExact AS date) IS NULL OR ca.date = :dateExact) " +
           "AND (CAST(:isDriver AS boolean) IS NULL OR ca.driver = :isDriver) " +
           "AND (:search IS NULL OR (" +
           "   LOWER(CAST(ca.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "   OR LOWER(CAST(ca.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "   OR CAST(ca.employee.dni AS string) LIKE CONCAT('%', CAST(:search AS string), '%') " +
           "   OR LOWER(CAST(ca.vehicle.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "))")
    Page<CrewAssignment> findAllWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("employeeName") String employeeName,
            @Param("employeeLastName") String employeeLastName,
            @Param("employeeDni") String employeeDni,
            @Param("vehicleId") Long vehicleId,
            @Param("vehicleLicensePlate") String vehicleLicensePlate,
            @Param("projectAreaId") Long projectAreaId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("dateExact") LocalDate dateExact,
            @Param("isDriver") Boolean isDriver,
            @Param("search") String search,
            Pageable pageable);

    @Modifying
    @Query("UPDATE CrewAssignment ca SET ca.deleted = true WHERE ca.date = :date AND ca.deleted = false")
    int softDeleteByDate(@Param("date") LocalDate date);

    @Query("SELECT ca.employee.id FROM CrewAssignment ca WHERE ca.date = :date AND ca.deleted = false")
    List<Long> findAssignedEmployeeIdsByDate(@Param("date") LocalDate date);
}
