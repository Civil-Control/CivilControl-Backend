package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.CrewScheduleDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrewScheduleDefaultRepository extends JpaRepository<CrewScheduleDefault, Long> {

    @Query("SELECT d FROM CrewScheduleDefault d JOIN FETCH d.projectArea WHERE d.projectArea.deleted = false ORDER BY d.projectArea.name")
    List<CrewScheduleDefault> findAllWithProjectArea();

    Optional<CrewScheduleDefault> findByProjectAreaId(Long projectAreaId);
}
