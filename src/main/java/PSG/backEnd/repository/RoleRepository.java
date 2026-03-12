package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Role entity.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Checks if a role exists with the given name.
     */
    boolean existsByName(String name);

    /**
     * Finds a role by ID excluding deleted roles.
     */
    Optional<Role> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a role by name excluding deleted roles.
     */
    Optional<Role> findByNameAndDeletedFalse(String name);

    /**
     * Finds a role by name (including deleted).
     */
    Optional<Role> findByName(String name);

    /**
     * Checks if a role exists with the given name (excluding deleted).
     */
    boolean existsByNameAndDeletedFalse(String name);

    /**
     * Checks if a role exists by ID (excluding deleted).
     */
    boolean existsByIdAndDeletedFalse(Long id);

    /**
     * Checks if a role exists with the given name for a specific tenant.
     */
    boolean existsByNameAndTenantId(String name, Long tenantId);

    /**
     * Finds a role by name and tenant ID.
     */
    Optional<Role> findByNameAndTenantId(String name, Long tenantId);

    /**
     * Gets all non-deleted roles.
     */
    Page<Role> findByDeletedFalse(Pageable pageable);

    /**
     * Gets roles by active status.
     */
    Page<Role> findByActiveAndDeletedFalse(Boolean active, Pageable pageable);

    /**
     * Search with filters.
     * Excludes the 'root' role from results as it's reserved for developers.
     */
    @Query("SELECT r FROM Role r " +
            "WHERE r.deleted = false " +
            "AND LOWER(CAST(r.name AS string)) != 'root' " +
            "AND (:name IS NULL OR LOWER(CAST(r.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
            "AND (CAST(:active AS boolean) IS NULL OR r.active = :active)")
    Page<Role> findAllWithFilters(
            @Param("name") String name,
            @Param("active") Boolean active,
            Pageable pageable
    );

}

