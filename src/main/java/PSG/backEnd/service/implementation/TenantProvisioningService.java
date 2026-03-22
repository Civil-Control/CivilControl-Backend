package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.security.Credentials;
import PSG.backEnd.model.entity.security.Permission;
import PSG.backEnd.model.entity.security.Role;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.repository.CredentialsRepository;
import PSG.backEnd.repository.PermissionRepository;
import PSG.backEnd.repository.RoleRepository;
import PSG.backEnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for provisioning new tenants with their initial data structure.
 * Creates three immutable system roles (OWNER, ADMIN, LECTOR) and the owner user.
 *
 * System Roles:
 * - OWNER: All permissions. Assigned to the first user of the tenant. Cannot be modified.
 * - ADMIN: Same permissions as OWNER except cannot modify OWNER users/role. Cannot be modified.
 * - LECTOR: Read-only access to all modules. Cannot be modified.
 *
 * @author Maximo Andriola
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_LECTOR = "LECTOR";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Provisions a new tenant with system roles and the owner user.
     * This method is idempotent - it will skip creation if roles/users already exist.
     *
     * @param tenantId The ID of the tenant to provision
     * @param ownerEmail Email for the owner user
     * @param ownerUsername Username for the owner user
     * @param ownerPassword Plain text password (will be encoded)
     * @param ownerFirstName First name of the owner
     * @param ownerLastName Last name of the owner
     */
    @Transactional
    public void provisionNewTenant(Long tenantId, String ownerEmail, String ownerUsername,
                                   String ownerPassword, String ownerFirstName, String ownerLastName) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }

        log.info("=== Starting tenant provisioning for Tenant ID: {} ===", tenantId);

        // Create immutable system roles
        Role ownerRole = createOwnerRole(tenantId);
        Role adminRole = createAdminRole(tenantId);
        Role lectorRole = createLectorRole(tenantId);

        // Create owner user with OWNER role
        createOwnerUser(tenantId, ownerEmail, ownerUsername, ownerPassword,
                ownerFirstName, ownerLastName, ownerRole);

        log.info("=== Tenant provisioning completed for Tenant ID: {} ===", tenantId);
    }

    /**
     * Creates OWNER role with all permissions for the specified tenant.
     * This is a system role and cannot be modified, renamed, or deleted.
     */
    private Role createOwnerRole(Long tenantId) {
        if (roleRepository.existsByNameAndTenantId(ROLE_OWNER, tenantId)) {
            log.info("OWNER role already exists for tenant {}, skipping...", tenantId);
            return roleRepository.findByNameAndTenantId(ROLE_OWNER, tenantId)
                    .orElseThrow(() -> new IllegalStateException("Role exists but cannot be retrieved"));
        }

        log.info("Creating OWNER role for tenant {}...", tenantId);

        List<Permission> allPermissions = permissionRepository.findAll();

        Role ownerRole = Role.builder()
                .name(ROLE_OWNER)
                .description("Propietario del tenant con todos los permisos. Rol del sistema, no se puede modificar ni eliminar.")
                .permissions(new HashSet<>(allPermissions))
                .active(true)
                .deleted(false)
                .systemRole(true)
                .build();

        ownerRole.setTenantId(tenantId);
        ownerRole = roleRepository.save(ownerRole);
        log.info("OWNER role created for tenant {} with {} permissions", tenantId, allPermissions.size());

        return ownerRole;
    }

    /**
     * Creates ADMIN role with the same permissions as OWNER, except it cannot modify
     * OWNER users or the OWNER role. This is a system role.
     */
    private Role createAdminRole(Long tenantId) {
        if (roleRepository.existsByNameAndTenantId(ROLE_ADMIN, tenantId)) {
            log.info("ADMIN role already exists for tenant {}, skipping...", tenantId);
            return roleRepository.findByNameAndTenantId(ROLE_ADMIN, tenantId)
                    .orElseThrow(() -> new IllegalStateException("Role exists but cannot be retrieved"));
        }

        log.info("Creating ADMIN role for tenant {}...", tenantId);

        List<Permission> allPermissions = permissionRepository.findAll();
        Set<Permission> adminPermissions = new HashSet<>();

        for (Permission permission : allPermissions) {
            // Exclude system-internal and developer-only permissions
            if (!permission.getName().contains("SYSTEM_") &&
                !permission.getName().contains("AUDIT_") &&
                !permission.getName().contains("EXCEPTION_LOG_")) {
                adminPermissions.add(permission);
            }
        }

        Role adminRole = Role.builder()
                .name(ROLE_ADMIN)
                .description("Administrador con la mayoría de permisos. Rol del sistema, no se puede modificar ni eliminar.")
                .permissions(adminPermissions)
                .active(true)
                .deleted(false)
                .systemRole(true)
                .build();

        adminRole.setTenantId(tenantId);
        adminRole = roleRepository.save(adminRole);
        log.info("ADMIN role created for tenant {} with {} permissions", tenantId, adminPermissions.size());

        return adminRole;
    }

    /**
     * Creates LECTOR role with read-only permissions for all modules.
     * This is a system role.
     */
    private Role createLectorRole(Long tenantId) {
        if (roleRepository.existsByNameAndTenantId(ROLE_LECTOR, tenantId)) {
            log.info("LECTOR role already exists for tenant {}, skipping...", tenantId);
            return roleRepository.findByNameAndTenantId(ROLE_LECTOR, tenantId)
                    .orElseThrow(() -> new IllegalStateException("Role exists but cannot be retrieved"));
        }

        log.info("Creating LECTOR role for tenant {}...", tenantId);

        List<Permission> allPermissions = permissionRepository.findAll();
        Set<Permission> readPermissions = new HashSet<>();

        for (Permission permission : allPermissions) {
            if (permission.getName().endsWith("_READ") ||
                permission.getName().equals("REPORT_VIEW")) {
                readPermissions.add(permission);
            }
        }

        Role lectorRole = Role.builder()
                .name(ROLE_LECTOR)
                .description("Lectura de todos los módulos. Rol del sistema, no se puede modificar ni eliminar.")
                .permissions(readPermissions)
                .active(true)
                .deleted(false)
                .systemRole(true)
                .build();

        lectorRole.setTenantId(tenantId);
        lectorRole = roleRepository.save(lectorRole);
        log.info("LECTOR role created for tenant {} with {} permissions", tenantId, readPermissions.size());

        return lectorRole;
    }

    /**
     * Creates the owner user for the specified tenant with the OWNER role.
     */
    private void createOwnerUser(Long tenantId, String email, String username,
                                 String password, String firstName, String lastName, Role ownerRole) {
        if (credentialsRepository.existsByUsernameAndTenantId(username, tenantId)) {
            log.info("Owner user '{}' already exists for tenant {}, skipping...", username, tenantId);
            return;
        }

        log.info("Creating owner user '{}' for tenant {}...", username, tenantId);

        Credentials credentials = Credentials.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .deleted(false)
                .build();
        credentials.setTenantId(tenantId);

        User ownerUser = User.builder()
                .credentials(credentials)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .jobTitle("Propietario")
                .enabled(true)
                .deleted(false)
                .roles(Set.of(ownerRole))
                .build();
        ownerUser.setTenantId(tenantId);

        credentials.setUser(ownerUser);
        userRepository.save(ownerUser);

        log.info("Owner user created successfully for tenant {}", tenantId);
        log.info("  Username: {}", username);
        log.info("  Email: {}", email);
        log.info("  Role: OWNER");
    }
}

