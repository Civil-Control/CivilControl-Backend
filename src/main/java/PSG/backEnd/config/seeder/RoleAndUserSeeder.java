package PSG.backEnd.config.seeder;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Seeder that creates initial roles and admin user.
 * Runs automatically when the application starts.
 *
 * Creates:
 * 1. ROOT role with all permissions
 * 2. ADMIN role with most permissions
 * 3. USER role with basic read permissions
 * 4. Admin user with ROOT role
 *
 * Order: Runs after PermissionSeeder (@Order(2))
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RoleAndUserSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== Starting role and user seeding ===");

        // Create roles
        Role rootRole = createRootRole();
        Role adminRole = createAdminRole();
        Role userRole = createUserRole();

        // Create admin user
        createAdminUser(rootRole);

        log.info("=== Role and user seeding completed ===");
    }

    /**
     * Creates ROOT role with all permissions.
     */
    private Role createRootRole() {
        if (roleRepository.existsByName("ROOT")) {
            log.info("ROOT role already exists, skipping...");
            return roleRepository.findByName("ROOT").orElseThrow();
        }

        log.info("Creating ROOT role with all permissions...");

        // Get all permissions
        List<Permission> allPermissions = permissionRepository.findAll();

        Role rootRole = Role.builder()
                .name("ROOT")
                .description("Super administrator with all permissions. Cannot be deleted or modified.")
                .permissions(new HashSet<>(allPermissions))
                .active(true)
                .deleted(false)
                .build();

        rootRole = roleRepository.save(rootRole);
        log.info("ROOT role created with {} permissions", allPermissions.size());

        return rootRole;
    }

    /**
     * Creates ADMIN role with most administrative permissions.
     */
    private Role createAdminRole() {
        if (roleRepository.existsByName("ADMIN")) {
            log.info("ADMIN role already exists, skipping...");
            return roleRepository.findByName("ADMIN").orElseThrow();
        }

        log.info("Creating ADMIN role...");

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

        adminRole = roleRepository.save(adminRole);
        log.info("ADMIN role created with {} permissions", adminPermissions.size());

        return adminRole;
    }

    /**
     * Creates USER role with basic read permissions.
     */
    private Role createUserRole() {
        Optional<Role> existingRole = roleRepository.findByName("USER");
        if (existingRole.isPresent()) {
            log.info("USER role already exists, skipping...");
            return existingRole.get();
        }

        log.info("Creating USER role...");

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

        userRole = roleRepository.save(userRole);
        log.info("USER role created with {} permissions", userPermissions.size());

        return userRole;
    }

    /**
     * Creates the initial admin user.
     * Username: admin
     * Password: Admin123
     */
    private void createAdminUser(Role rootRole) {
        // Check if admin user already exists
        if (credentialsRepository.existsByUsername("admin")) {
            log.info("Admin user already exists, skipping...");
            return;
        }

        log.info("Creating admin user...");

        // Create credentials
        Credentials credentials = Credentials.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin123"))
                .deleted(false)
                .build();

        // Create user
        User adminUser = User.builder()
                .credentials(credentials)
                .email("admin@psg.com")
                .firstName("System")
                .lastName("Administrator")
                .jobTitle("System Administrator")
                .enabled(true)
                .deleted(false)
                .roles(Set.of(rootRole))
                .build();

        // Set bidirectional relationship
        credentials.setUser(adminUser);

        // Save user (cascades to credentials)
        userRepository.save(adminUser);

        log.info("Admin user created successfully");
        log.info("  Username: admin");
        log.info("  Password: Admin123");
        log.info("  Email: admin@psg.com");
        log.info("  Role: ROOT");
        log.info("=== IMPORTANT: Change the admin password after first login! ===");
    }
}

