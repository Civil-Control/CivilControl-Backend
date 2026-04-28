package PSG.backEnd.repository.recovery;

import PSG.backEnd.model.entity.recovery.RecoverySupplierConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecoverySupplierConfigRepository extends JpaRepository<RecoverySupplierConfig, Long> {

    Optional<RecoverySupplierConfig> findByIdAndDeletedFalse(Long id);

    List<RecoverySupplierConfig> findByProjectAreaIdAndDeletedFalseOrderBySupplier_LegalNameAsc(Long projectAreaId);

    Optional<RecoverySupplierConfig> findByProjectAreaIdAndSupplierIdAndDeletedFalse(Long projectAreaId, Long supplierId);

    boolean existsByProjectAreaIdAndSupplierIdAndDeletedFalse(Long projectAreaId, Long supplierId);
}
