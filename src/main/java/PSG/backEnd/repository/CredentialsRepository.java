package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

