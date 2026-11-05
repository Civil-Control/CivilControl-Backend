package PSG.backEnd.repository;

import PSG.backEnd.model.entity.gasStation.CustomFuelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomFuelTypeRepository extends JpaRepository<CustomFuelType, Long> {
    List<CustomFuelType> findAllByTenantIdAndDeletedFalseOrderByLabel(Long tenantId);
    List<CustomFuelType> findAllByTenantIdOrderByLabel(Long tenantId);
    Optional<CustomFuelType> findByTenantIdAndKeyAndDeletedFalse(Long tenantId, String key);
    /** Ignores the deleted flag, so a since-deleted type's label can still be resolved for historical records. */
    Optional<CustomFuelType> findByTenantIdAndKey(Long tenantId, String key);
    boolean existsByTenantIdAndKeyAndDeletedFalse(Long tenantId, String key);
}
