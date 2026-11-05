package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Credentials entity.
 */
@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {

    /**
     * Finds credentials by username (including deleted).
     */
    Optional<Credentials> findByUsername(String username);

    /**
     * Finds credentials by username excluding deleted.
     */
    Optional<Credentials> findByUsernameAndDeletedFalse(String username);

    /**
     * Checks if credentials exist with the given username (including deleted).
     */
    boolean existsByUsername(String username);

    /**
     * Checks if credentials exist with the given username (excluding deleted).
     */
    boolean existsByUsernameAndDeletedFalse(String username);

    /**
     * Checks if credentials exist with the given username for a specific tenant.
     */
    boolean existsByUsernameAndTenantId(String username, Long tenantId);

    @Query(value = "SELECT DISTINCT tenant_id FROM credentials WHERE username = :username AND deleted = false",
            nativeQuery = true)
    List<Long> findActiveTenantIdsByUsername(@Param("username") String username);

    @Query(value = "SELECT COUNT(*) > 0 FROM credentials c LEFT JOIN users u ON u.credentials_id = c.id " +
            "WHERE c.username = :username AND c.deleted = false AND (u.id IS NULL OR u.id <> :excludedUserId)",
            nativeQuery = true)
    boolean existsActiveUsernameInAnyTenant(@Param("username") String username,
                                            @Param("excludedUserId") Long excludedUserId);
}

