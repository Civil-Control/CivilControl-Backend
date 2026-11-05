package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    Optional<VehicleType> findByNameAndTenantId(String name, Long tenantId);
    boolean existsByNameAndTenantId(String name, Long tenantId);
    boolean existsByNameAndTenantIdAndIdNot(String name, Long tenantId, Long id);

    @Query("SELECT vt FROM VehicleType vt " +
           "WHERE (:name IS NULL OR LOWER(CAST(vt.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:requiresTruckEquipment IS NULL OR vt.requiresTruckEquipment = :requiresTruckEquipment)")
    Page<VehicleType> findAllWithFilters(
            @Param("name") String name,
            @Param("requiresTruckEquipment") Boolean requiresTruckEquipment,
            Pageable pageable
    );
}

