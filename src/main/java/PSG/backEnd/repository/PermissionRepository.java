package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

