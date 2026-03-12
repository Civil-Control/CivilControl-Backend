package PSG.backEnd.config.seeder;

import PSG.backEnd.service.implementation.TenantProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeder that provisions the default tenant (ESEA SA) with initial roles and admin user.
 * Runs automatically when the application starts.
 *
 * This seeder now delegates all provisioning logic to TenantProvisioningService,
 * making it reusable for creating new tenants dynamically via API.
 *
 * Creates:
 * 1. ROOT role with all permissions (for Tenant ID: 1)
 * 2. ADMIN role with most permissions (for Tenant ID: 1)
 * 3. USER role with basic read permissions (for Tenant ID: 1)
 * 4. Admin user with ROOT role (for Tenant ID: 1)
 *
 * Order: Runs after PermissionSeeder (@Order(2))
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RoleAndUserSeeder implements CommandLineRunner {

    private final TenantProvisioningService tenantProvisioningService;

    // Default tenant configuration
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String DEFAULT_ADMIN_EMAIL = "admin@psg.com";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123";

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Starting default tenant (ESEA SA) provisioning ===");

        // Provision default tenant using the reusable service
        tenantProvisioningService.provisionNewTenant(
                DEFAULT_TENANT_ID,
                DEFAULT_ADMIN_EMAIL,
                DEFAULT_ADMIN_USERNAME,
                DEFAULT_ADMIN_PASSWORD
        );

        log.info("=== Default tenant provisioning completed ===");
    }
}

