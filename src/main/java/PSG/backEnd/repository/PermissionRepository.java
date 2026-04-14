package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Permission entity.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Finds a permission by its name.
     * @param name Permission name
     * @return Optional with the permission if exists
     */
    Optional<Permission> findByName(String name);

    /**
     * Checks if a permission exists with the given name.
     * @param name Permission name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Finds all permissions whose name is NOT in the given collection.
     * Used to detect stale permissions that no longer exist in AppPermissions.
     */
    List<Permission> findByNameNotIn(Collection<String> names);

    /**
     * Removes a stale permission from all role_permissions associations.
     * Must be called before deleting the permission to avoid FK violations.
     */
    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    void removeFromAllRoles(@Param("permissionId") Long permissionId);
}

