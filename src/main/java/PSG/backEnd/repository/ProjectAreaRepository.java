package PSG.backEnd.repository;

import PSG.backEnd.model.entity.ProjectArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectAreaRepository extends JpaRepository<ProjectArea, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndDeletedFalse(String name);
    boolean existsByIdAndDeletedFalse(Long id);
    Optional<ProjectArea> findByIdAndDeletedFalse(Long id);
    Optional<ProjectArea> findByNameAndDeletedTrue(String name);

    @Query("SELECT p FROM ProjectArea p " +
           "WHERE p.deleted = false " +
           "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:active IS NULL OR p.active = :active)")
    Page<ProjectArea> findAllWithFilters (
            @Param("name") String name,
            @Param("active") Boolean active,
            Pageable pageable);
}
