package PSG.backEnd.repository;

import PSG.backEnd.model.entity.BiometricLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiometricLogRepository extends JpaRepository<BiometricLog, Long> {
}
