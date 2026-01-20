package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by credentials ID.
     */
    Optional<User> findByCredentialsId(Long credentialsId);

    /**
     * Finds a user by username (through credentials).
     */
    Optional<User> findByCredentialsUsername(String username);

    /**
     * Finds a user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by ID excluding deleted users.
     */
    Optional<User> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a user by username excluding deleted users.
     */
    Optional<User> findByCredentialsUsernameAndDeletedFalse(String username);

    /**
     * Checks if a user exists with the given username (through credentials).
     */
    boolean existsByCredentialsUsername(String username);

    /**
     * Checks if a user exists with the given email.
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists with the given username (excluding deleted).
     */
    boolean existsByCredentialsUsernameAndDeletedFalse(String username);

    /**
     * Checks if a user exists with the given email (excluding deleted).
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Checks if a user exists by ID (excluding deleted).
     */
    boolean existsByIdAndDeletedFalse(Long id);

    /**
     * Gets all non-deleted users.
     */
    Page<User> findByDeletedFalse(Pageable pageable);

    /**
     * Gets users by enabled status.
     */
    Page<User> findByEnabledAndDeletedFalse(Boolean enabled, Pageable pageable);

    /**
     * Search with filters.
     * Excludes the root user (ID 1) from results as it's reserved for developers.
     */
    @Query("SELECT u FROM User u " +
            "WHERE u.deleted = false " +
            "AND u.id != 1 " +
            "AND (:username IS NULL OR LOWER(CAST(u.credentials.username AS string)) LIKE LOWER(CONCAT('%', CAST(:username AS string), '%'))) " +
            "AND (:email IS NULL OR LOWER(CAST(u.email AS string)) LIKE LOWER(CONCAT('%', CAST(:email AS string), '%'))) " +
            "AND (:firstName IS NULL OR LOWER(CAST(u.firstName AS string)) LIKE LOWER(CONCAT('%', CAST(:firstName AS string), '%'))) " +
            "AND (:lastName IS NULL OR LOWER(CAST(u.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) " +
            "AND (:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> findAllWithFilters(
            @Param("username") String username,
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );

    /**
     * Search with filters, excluding a specific username.
     * Excludes the root user (ID 1) and the specified username from results.
     */
    @Query("SELECT u FROM User u " +
            "WHERE u.deleted = false " +
            "AND u.id != 1 " +
            "AND (:excludeUsername IS NULL OR u.credentials.username != :excludeUsername) " +
            "AND (:username IS NULL OR LOWER(CAST(u.credentials.username AS string)) LIKE LOWER(CONCAT('%', CAST(:username AS string), '%'))) " +
            "AND (:email IS NULL OR LOWER(CAST(u.email AS string)) LIKE LOWER(CONCAT('%', CAST(:email AS string), '%'))) " +
            "AND (:firstName IS NULL OR LOWER(CAST(u.firstName AS string)) LIKE LOWER(CONCAT('%', CAST(:firstName AS string), '%'))) " +
            "AND (:lastName IS NULL OR LOWER(CAST(u.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) " +
            "AND (:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> findAllWithFiltersExcludingUsername(
            @Param("excludeUsername") String excludeUsername,
            @Param("username") String username,
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
