package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Vehicle;
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
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByDeletedFalse();
    Optional<Vehicle> findByIdAndDeletedFalse(Long id);
    Optional<Vehicle> findByLicensePlateAndDeletedTrue(String licensePlate);
    boolean existsByLicensePlateAndDeletedFalse(String licensePlate);
    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT v FROM Vehicle v " +
            "LEFT JOIN v.projectArea pa " +
            "WHERE (:licensePlate IS NULL OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%'))) " +
            "AND (:brand IS NULL OR LOWER(v.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) " +
            "AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
            "AND (:year IS NULL OR v.year = :year) " +
            "AND (:color IS NULL OR LOWER(v.color) LIKE LOWER(CONCAT('%', :color, '%'))) " +
            "AND (:nickName IS NULL OR LOWER(v.nickName) LIKE LOWER(CONCAT('%', :nickName, '%'))) " +
            "AND (:vehicleType IS NULL OR LOWER(CAST(v.vehicleType AS string)) LIKE LOWER(CONCAT('%', :vehicleType, '%'))) " +
            "AND (:projectAreaName IS NULL OR LOWER(pa.name) LIKE LOWER(CONCAT('%', :projectAreaName, '%'))) " +
            "AND (:storedIn IS NULL OR LOWER(v.storedIn) LIKE LOWER(CONCAT('%', :storedIn, '%'))) " +
            "AND (:vtvExpirationDate IS NULL OR v.vtvExpirationDate = :vtvExpirationDate) " +
            "AND v.deleted = false")
    Page<Vehicle> findAllWithFilters(
            @Param("licensePlate") String licensePlate,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("year") Integer year,
            @Param("color") String color,
            @Param("nickName") String nickName,
            @Param("vehicleType") String vehicleType,
            @Param("projectAreaName") String projectAreaName,
            @Param("storedIn") String storedIn,
            @Param("vtvExpirationDate") LocalDate vtvExpirationDate,
            Pageable pageable
    );
}
