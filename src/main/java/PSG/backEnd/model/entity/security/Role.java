package PSG.backEnd.model.entity.security;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a system role.
 * Roles are created dynamically by administrators and group multiple permissions.
 * A user can have multiple roles.
 */
@Entity
@Table(name = "roles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique role name (e.g., "Senior Architect", "Site Manager").
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Description of the role and its responsibilities.
     */
    @Column(length = 500, columnDefinition = "VARCHAR(500)")
    private String description;

    /**
     * Set of permissions assigned to this role.
     * EAGER fetch is used to load permissions along with the role in security checks.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Indicates whether the role is active or disabled.
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Soft delete flag to maintain history.
     */
    @Column(nullable = false)
    private Boolean deleted;
}

