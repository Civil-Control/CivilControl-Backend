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

    /** Feature 18 — used to enforce the at-most-one recovery sector per tenant. */
    boolean existsByIsRecoverySectorTrueAndDeletedFalse();

    /** Feature 18 — convenience lookup for the tenant's recovery sector (if any). */
    Optional<ProjectArea> findFirstByIsRecoverySectorTrueAndDeletedFalse();

    @Query("SELECT p FROM ProjectArea p " +
           "WHERE p.deleted = false " +
           "AND (:name IS NULL OR LOWER(CAST(p.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:active IS NULL OR p.active = :active) " +
           "AND (:search IS NULL OR (LOWER(CAST(p.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "     OR LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<ProjectArea> findAllWithFilters (
            @Param("name") String name,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);
}
