package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    Optional<VehicleType> findByNameAndTenantId(String name, Long tenantId);
    boolean existsByNameAndTenantId(String name, Long tenantId);
    boolean existsByNameAndTenantIdAndIdNot(String name, Long tenantId, Long id);
}

