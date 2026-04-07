package PSG.backEnd.repository;

import PSG.backEnd.model.entity.OrphanBiometricLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrphanBiometricLogRepository extends JpaRepository<OrphanBiometricLog, Long> {
}
