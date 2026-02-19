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
 * This includes creating base roles (ROOT, ADMIN, USER) and an admin user.
 *
 * This service is designed to work in multi-tenant environments where each tenant
 * needs isolated data. It explicitly assigns tenantId to all entities, bypassing
 * the TenantContext which may not be available during seeding or API-driven provisioning.
 *
 * @author Maximo Andriola
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Provisions a new tenant with initial roles and admin user.
     * This method is idempotent - it will skip creation if roles/users already exist.
     *
     * @param tenantId The ID of the tenant to provision
     * @param adminEmail Email for the admin user
     * @param username Username for the admin user
     * @param password Plain text password (will be encoded)
     * @throws IllegalArgumentException if tenantId is null or invalid
     */
    @Transactional
    public void provisionNewTenant(Long tenantId, String adminEmail, String username, String password) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }

        log.info("=== Starting tenant provisioning for Tenant ID: {} ===", tenantId);

        // Create base roles
        Role rootRole = createRootRole(tenantId);
        Role adminRole = createAdminRole(tenantId);
        Role userRole = createUserRole(tenantId);

        // Create admin user
        createAdminUser(tenantId, adminEmail, username, password, rootRole);

        log.info("=== Tenant provisioning completed for Tenant ID: {} ===", tenantId);
    }

    /**
     * Creates ROOT role with all permissions for the specified tenant.
     * ROOT role is for system administrators and developers only.
     */
    private Role createRootRole(Long tenantId) {
        // Check if ROOT role already exists for this tenant
        if (roleRepository.existsByNameAndTenantId("ROOT", tenantId)) {
            log.info("ROOT role already exists for tenant {}, skipping...", tenantId);
            return roleRepository.findByNameAndTenantId("ROOT", tenantId)
                    .orElseThrow(() -> new IllegalStateException("Role exists but cannot be retrieved"));
        }

        log.info("Creating ROOT role for tenant {}...", tenantId);

        // Get all permissions (permissions are shared across tenants)
        List<Permission> allPermissions = permissionRepository.findAll();

        Role rootRole = Role.builder()
                .name("ROOT")
                .description("Super administrator with all permissions. Cannot be deleted or modified.")
                .permissions(new HashSet<>(allPermissions))
                .active(true)
                .deleted(false)
                .build();

        // CRITICAL: Explicitly set tenantId (bypasses TenantContext)
        rootRole.setTenantId(tenantId);

        rootRole = roleRepository.save(rootRole);
        log.info("ROOT role created for tenant {} with {} permissions", tenantId, allPermissions.size());

        return rootRole;
    }

    /**
     * Creates ADMIN role with most administrative permissions for the specified tenant.
     * ADMIN role excludes system-critical and developer-only permissions.
     */
    private Role createAdminRole(Long tenantId) {
        // Check if ADMIN role already exists for this tenant
        if (roleRepository.existsByNameAndTenantId("ADMIN", tenantId)) {
            log.info("ADMIN role already exists for tenant {}, skipping...", tenantId);
            return roleRepository.findByNameAndTenantId("ADMIN", tenantId)
                    .orElseThrow(() -> new IllegalStateException("Role exists but cannot be retrieved"));
        }

        log.info("Creating ADMIN role for tenant {}...", tenantId);

        // Get permissions for admin (exclude ROOT-only permissions)
        List<Permission> allPermissions = permissionRepository.findAll();
        Set<Permission> adminPermissions = new HashSet<>();

        for (Permission permission : allPermissions) {
            // Exclude ROOT-only permissions (system-critical and developer tools)
            if (!permission.getName().contains("SYSTEM_") &&
                !permission.getName().contains("AUDIT_") &&
                !permission.getName().contains("EXCEPTION_LOG_")) {
                adminPermissions.add(permission);
            }
        }

        Role adminRole = Role.builder()
                .name("ADMIN")
                .description("Administrator with most permissions for daily operations")
                .permissions(adminPermissions)
                .active(true)
                .deleted(false)
                .build();

        // CRITICAL: Explicitly set tenantId
        adminRole.setTenantId(tenantId);

        adminRole = roleRepository.save(adminRole);
        log.info("ADMIN role created for tenant {} with {} permissions", tenantId, adminPermissions.size());

        return adminRole;
    }

    /**
     * Creates USER role with basic read permissions for the specified tenant.
     */
    private Role createUserRole(Long tenantId) {
        // Check if USER role already exists for this tenant
        if (roleRepository.existsByNameAndTenantId("USER", tenantId)) {
            log.info("USER role already exists for tenant {}, skipping...", tenantId);
            return roleRepository.findByNameAndTenantId("USER", tenantId)
                    .orElseThrow(() -> new IllegalStateException("Role exists but cannot be retrieved"));
        }

        log.info("Creating USER role for tenant {}...", tenantId);

        // Get only READ permissions
        List<Permission> allPermissions = permissionRepository.findAll();
        Set<Permission> userPermissions = new HashSet<>();

        for (Permission permission : allPermissions) {
            if (permission.getName().contains("_READ")) {
                userPermissions.add(permission);
            }
        }

        Role userRole = Role.builder()
                .name("USER")
                .description("Basic user with read-only permissions")
                .permissions(userPermissions)
                .active(true)
                .deleted(false)
                .build();

        // CRITICAL: Explicitly set tenantId
        userRole.setTenantId(tenantId);

        userRole = roleRepository.save(userRole);
        log.info("USER role created for tenant {} with {} permissions", tenantId, userPermissions.size());

        return userRole;
    }

    /**
     * Creates the admin user for the specified tenant.
     *
     * @param tenantId The tenant ID
     * @param adminEmail Email for the admin user
     * @param username Username for authentication
     * @param password Plain text password (will be encoded)
     * @param rootRole The ROOT role to assign to this user
     */
    private void createAdminUser(Long tenantId, String adminEmail, String username, String password, Role rootRole) {
        // Check if admin user already exists for this tenant
        if (credentialsRepository.existsByUsernameAndTenantId(username, tenantId)) {
            log.info("Admin user '{}' already exists for tenant {}, skipping...", username, tenantId);
            return;
        }

        log.info("Creating admin user '{}' for tenant {}...", username, tenantId);

        // Create credentials
        Credentials credentials = Credentials.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .deleted(false)
                .build();

        // CRITICAL: Explicitly set tenantId
        credentials.setTenantId(tenantId);

        // Create user
        User adminUser = User.builder()
                .credentials(credentials)
                .email(adminEmail)
                .firstName("System")
                .lastName("Administrator")
                .jobTitle("System Administrator")
                .enabled(true)
                .deleted(false)
                .roles(Set.of(rootRole))
                .build();

        // CRITICAL: Explicitly set tenantId
        adminUser.setTenantId(tenantId);

        // Set bidirectional relationship
        credentials.setUser(adminUser);

        // Save user (cascades to credentials)
        userRepository.save(adminUser);

        log.info("Admin user created successfully for tenant {}", tenantId);
        log.info("  Username: {}", username);
        log.info("  Email: {}", adminEmail);
        log.info("  Role: ROOT");
        log.info("=== IMPORTANT: Change the admin password after first login! ===");
    }
}

