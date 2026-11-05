package PSG.backEnd.model.entity.security;

import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a system role.
 * Roles are created dynamically by administrators and group multiple permissions.
 * A user can have multiple roles.
 */
@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Role extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Role name (e.g., "ROOT", "ADMIN", "USER", or custom roles).
     * Unique per tenant.
     */
    @Column(nullable = false, length = 100)
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

    /**
     * Indicates if this is a system-defined role (OWNER, ADMIN, LECTOR).
     * System roles cannot be modified, renamed, or deleted.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean systemRole = false;

    /**
     * Hierarchical position of the role within the tenant.
     * Position 1 = highest authority (OWNER). Higher numbers = lower authority.
     * Uniqueness per tenant is enforced in business logic, not via DB constraint.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;
}

