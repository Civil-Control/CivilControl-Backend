package PSG.backEnd.repository;

import PSG.backEnd.model.entity.employee.AttendanceRecord;
import PSG.backEnd.model.enums.employee.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByEmployeeIdAndDateAndTimeAndMovementType(
            Long employeeId, LocalDate date, LocalTime time, MovementType movementType);

    List<AttendanceRecord> findByEmployeeIdAndDateOrderByTimeAsc(Long employeeId, LocalDate date);

    @Query("SELECT ar FROM AttendanceRecord ar " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR ar.employee.id = :employeeId) " +
            "AND (:firstName IS NULL OR LOWER(CAST(ar.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:firstName AS string), '%'))) " +
            "AND (:lastName IS NULL OR LOWER(CAST(ar.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) " +
            "AND (:dni IS NULL OR LOWER(CAST(ar.employee.dni AS string)) LIKE LOWER(CONCAT('%', CAST(:dni AS string), '%'))) " +
            "AND (:movementType IS NULL OR ar.movementType = :movementType) " +
            "AND (CAST(:buildingId AS long) IS NULL OR ar.building.id = :buildingId) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR ar.employee.projectArea.id = :projectAreaId) " +
            "AND (CAST(:dateFrom AS date) IS NULL OR ar.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR ar.date <= :dateTo) " +
            "AND (CAST(:timeFrom AS time) IS NULL OR ar.time >= :timeFrom) " +
            "AND (CAST(:timeTo AS time) IS NULL OR ar.time <= :timeTo) " +
            "AND (:search IS NULL OR (LOWER(CAST(ar.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(ar.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<AttendanceRecord> findAllWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("dni") String dni,
            @Param("movementType") MovementType movementType,
            @Param("buildingId") Long buildingId,
            @Param("projectAreaId") Long projectAreaId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("timeFrom") LocalTime timeFrom,
            @Param("timeTo") LocalTime timeTo,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT ar FROM AttendanceRecord ar " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR ar.employee.id = :employeeId) " +
            "AND (:firstName IS NULL OR LOWER(CAST(ar.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:firstName AS string), '%'))) " +
            "AND (:lastName IS NULL OR LOWER(CAST(ar.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) " +
            "AND (:dni IS NULL OR LOWER(CAST(ar.employee.dni AS string)) LIKE LOWER(CONCAT('%', CAST(:dni AS string), '%'))) " +
            "AND (:movementType IS NULL OR ar.movementType = :movementType) " +
            "AND (CAST(:buildingId AS long) IS NULL OR ar.building.id = :buildingId) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR ar.employee.projectArea.id = :projectAreaId) " +
            "AND (CAST(:dateFrom AS date) IS NULL OR ar.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR ar.date <= :dateTo) " +
            "AND (CAST(:timeFrom AS time) IS NULL OR ar.time >= :timeFrom) " +
            "AND (CAST(:timeTo AS time) IS NULL OR ar.time <= :timeTo) " +
            "AND (:search IS NULL OR (LOWER(CAST(ar.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(ar.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    List<AttendanceRecord> findAllWithFiltersNoPage(
            @Param("employeeId") Long employeeId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("dni") String dni,
            @Param("movementType") MovementType movementType,
            @Param("buildingId") Long buildingId,
            @Param("projectAreaId") Long projectAreaId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("timeFrom") LocalTime timeFrom,
            @Param("timeTo") LocalTime timeTo,
            @Param("search") String search
    );

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR ar.employee.id = :employeeId) " +
            "AND (:firstName IS NULL OR LOWER(CAST(ar.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:firstName AS string), '%'))) " +
            "AND (:lastName IS NULL OR LOWER(CAST(ar.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) " +
            "AND (:dni IS NULL OR LOWER(CAST(ar.employee.dni AS string)) LIKE LOWER(CONCAT('%', CAST(:dni AS string), '%'))) " +
            "AND (:movementType IS NULL OR ar.movementType = :movementType) " +
            "AND (CAST(:buildingId AS long) IS NULL OR ar.building.id = :buildingId) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR ar.employee.projectArea.id = :projectAreaId) " +
            "AND (CAST(:dateFrom AS date) IS NULL OR ar.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR ar.date <= :dateTo) " +
            "AND (CAST(:timeFrom AS time) IS NULL OR ar.time >= :timeFrom) " +
            "AND (CAST(:timeTo AS time) IS NULL OR ar.time <= :timeTo) " +
            "AND (:search IS NULL OR (LOWER(CAST(ar.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(ar.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    long countWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("dni") String dni,
            @Param("movementType") MovementType movementType,
            @Param("buildingId") Long buildingId,
            @Param("projectAreaId") Long projectAreaId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("timeFrom") LocalTime timeFrom,
            @Param("timeTo") LocalTime timeTo,
            @Param("search") String search
    );

    List<AttendanceRecord> findByEmployeeIdInAndDateBetweenOrderByDateAscTimeAsc(
            List<Long> employeeIds, LocalDate dateFrom, LocalDate dateTo);
}
