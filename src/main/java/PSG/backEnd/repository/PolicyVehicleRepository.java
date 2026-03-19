package PSG.backEnd.repository;

import PSG.backEnd.model.entity.insurance.PolicyVehicle;
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
public interface PolicyVehicleRepository extends JpaRepository<PolicyVehicle, Long> {

    List<PolicyVehicle> findAllByOrderByIdDesc();
    Optional<PolicyVehicle> findByIdAndDeletedFalse(Long id);
    List<PolicyVehicle> findByAutoPolicyId(Long autoPolicyId);
    List<PolicyVehicle> findByVehicleId(Long vehicleId);
    Optional<PolicyVehicle> findByVehicleIdAndAutoPolicyIdAndDeletedTrue(Long vehicleId, Long autoPolicyId);
    boolean existsByIdAndDeletedFalse(Long id);
    boolean existsByVehicleIdAndAutoPolicyId(Long vehicleId, Long autoPolicyId);

    @Query("SELECT pv FROM PolicyVehicle pv " +
            "JOIN pv.vehicle v " +
            "JOIN pv.autoPolicy ap " +
            "JOIN ap.insurancePolicy ip " +
            "WHERE (CAST(:vehicleId AS long) IS NULL OR pv.vehicle.id = :vehicleId) " +
            "AND (CAST(:autoPolicyId AS long) IS NULL OR pv.autoPolicy.id = :autoPolicyId) " +
            "AND (:licensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:licensePlate AS string), '%'))) " +
            "AND (:vehicleBrand IS NULL OR LOWER(CAST(v.brand AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleBrand AS string), '%'))) " +
            "AND (:vehicleModel IS NULL OR LOWER(CAST(v.model AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleModel AS string), '%'))) " +
            "AND (:policyNumber IS NULL OR LOWER(CAST(ip.policyNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:policyNumber AS string), '%'))) " +
            "AND (CAST(:effectiveFromStart AS date) IS NULL OR pv.effectiveFrom >= :effectiveFromStart) " +
            "AND (CAST(:effectiveFromEnd AS date) IS NULL OR pv.effectiveFrom <= :effectiveFromEnd) " +
            "AND (CAST(:effectiveToStart AS date) IS NULL OR pv.effectiveTo >= :effectiveToStart) " +
            "AND (CAST(:effectiveToEnd AS date) IS NULL OR pv.effectiveTo <= :effectiveToEnd) " +
            "AND (:isCancelled IS NULL OR " +
            "     (:isCancelled = true AND pv.cancellationDate IS NOT NULL) OR " +
            "     (:isCancelled = false AND pv.cancellationDate IS NULL)) " +
            "AND pv.deleted = false " +
            "ORDER BY pv.id DESC")
    Page<PolicyVehicle> findAllWithFilters(
            @Param("vehicleId") Long vehicleId,
            @Param("autoPolicyId") Long autoPolicyId,
            @Param("licensePlate") String licensePlate,
            @Param("vehicleBrand") String vehicleBrand,
            @Param("vehicleModel") String vehicleModel,
            @Param("policyNumber") String policyNumber,
            @Param("effectiveFromStart") LocalDate effectiveFromStart,
            @Param("effectiveFromEnd") LocalDate effectiveFromEnd,
            @Param("effectiveToStart") LocalDate effectiveToStart,
            @Param("effectiveToEnd") LocalDate effectiveToEnd,
            @Param("isCancelled") Boolean isCancelled,
            Pageable pageable
    );
}
