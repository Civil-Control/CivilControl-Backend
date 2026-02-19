package PSG.backEnd.model.entity.security;

import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing user authentication credentials.
 * Separated from User entity to improve modularity and security.
 */
@Entity
@Table(name = "credentials", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "username"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Credentials extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for login.
     */
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String username;

    /**
     * Hashed password (BCrypt).
     */
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    private String password;

    /**
     * Soft delete flag.
     */
    @Column(nullable = false)
    private Boolean deleted;

    /**
     * One-to-one relationship with User.
     */
    @OneToOne(mappedBy = "credentials", cascade = CascadeType.ALL)
    private User user;
}

