package PSG.backEnd.model.entity.security;

import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a system user.
 * Implements UserDetails from Spring Security for integration with the security framework.
 * A user can have multiple roles, and each role contains multiple permissions.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "email"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class User extends TenantEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User authentication credentials (username and password).
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "credentials_id", referencedColumnName = "id")
    private Credentials credentials;

    /**
     * Unique user email.
     */
    @Column(nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String email;

    /**
     * User's first name.
     */
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String firstName;

    /**
     * User's last name.
     */
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String lastName;

    /**
     * User's job title or position (for display purposes, not related to security).
     */
    @Column(length = 100, columnDefinition = "VARCHAR(100)")
    private String jobTitle;

    /**
     * Indicates whether the account is enabled.
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * Soft delete flag.
     */
    @Column(nullable = false)
    private Boolean deleted;

    /**
     * Roles assigned to the user.
     * EAGER fetch is used to load roles (and their permissions) in security checks.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    // ==================== UserDetails Implementation ====================

    /**
     * Returns the user's authorities (permissions).
     * Built from:
     * 1. Role names (prefixed with "ROLE_")
     * 2. All individual permissions from each role
     *
     * This allows checking both hasRole("ADMIN") and hasAuthority("EMPLOYEE_READ").
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add roles as authorities (format: ROLE_name)
        for (Role role : roles) {
            if (role.getActive() && !role.getDeleted()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));

                // Add individual permissions from each role
                for (Permission permission : role.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getName()));
                }
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return credentials != null ? credentials.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return credentials != null ? credentials.getUsername() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled && !deleted;
    }

    /**
     * Utility method to get the full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

