package PSG.backEnd.repository;

import PSG.backEnd.model.entity.ProjectAreaTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAreaTaskRepository extends JpaRepository<ProjectAreaTask, Long> {

    Optional<ProjectAreaTask> findByIdAndDeletedFalse(Long id);

    List<ProjectAreaTask> findByProjectAreaIdAndDeletedFalseOrderByNameAsc(Long projectAreaId);

    boolean existsByProjectAreaIdAndNameAndDeletedFalse(Long projectAreaId, String name);

    boolean existsByIdAndDeletedFalse(Long id);

    Optional<ProjectAreaTask> findByProjectAreaIdAndNameAndDeletedTrue(Long projectAreaId, String name);
}
