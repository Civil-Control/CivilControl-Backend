package PSG.backEnd.config.seeder;

import PSG.backEnd.model.entity.Address;
import PSG.backEnd.model.entity.Tenant;
import PSG.backEnd.repository.TenantRepository;
import PSG.backEnd.service.implementation.TenantProvisioningService;
import PSG.backEnd.service.util.TenantContext;
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
 * 1. Default Tenant entity (ESEA SA) if it doesn't exist
 * 2. ROOT role with all permissions (for Tenant ID: 1)
 * 3. ADMIN role with most permissions (for Tenant ID: 1)
 * 4. USER role with basic read permissions (for Tenant ID: 1)
 * 5. Admin user with ROOT role (for Tenant ID: 1)
 *
 * Order: Runs after PermissionSeeder (@Order(2))
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RoleAndUserSeeder implements CommandLineRunner {

    private final TenantProvisioningService tenantProvisioningService;
    private final TenantRepository tenantRepository;

    // Default tenant configuration
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String DEFAULT_ADMIN_EMAIL = "admin@psg.com";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123";

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Starting default tenant (ESEA SA) provisioning ===");

        // Explicitly set TenantContext for seeding
        TenantContext.setCurrentTenant(DEFAULT_TENANT_ID);

        try {
            // Ensure default tenant entity exists
            seedDefaultTenant();

            // Provision default tenant using the reusable service
            tenantProvisioningService.provisionNewTenant(
                    DEFAULT_TENANT_ID,
                    DEFAULT_ADMIN_EMAIL,
                    DEFAULT_ADMIN_USERNAME,
                    DEFAULT_ADMIN_PASSWORD
            );
        } finally {
            TenantContext.clear();
        }

        log.info("=== Default tenant provisioning completed ===");
    }

    /**
     * Seeds the default Tenant entity (ESEA SA) if it doesn't already exist.
     * This is required for the company settings page (/api/v1/tenants/me).
     */
    private void seedDefaultTenant() {
        if (tenantRepository.existsByIdAndDeletedFalse(DEFAULT_TENANT_ID)) {
            log.info("Default tenant already exists (ID: {}), skipping...", DEFAULT_TENANT_ID);
            return;
        }

        // Also check if it exists but is deleted
        if (tenantRepository.findById(DEFAULT_TENANT_ID).isPresent()) {
            log.info("Default tenant exists (ID: {}), skipping...", DEFAULT_TENANT_ID);
            return;
        }

        log.info("Creating default tenant (ESEA SA)...");

        Tenant defaultTenant = Tenant.builder()
                .name("ESEA SA")
                .cuit("30-12345678-9")
                .legalName("ESEA Sociedad Anónima")
                .address(Address.builder()
                        .city("Buenos Aires")
                        .state("Buenos Aires")
                        .country("Argentina")
                        .build())
                .active(true)
                .deleted(false)
                .build();

        tenantRepository.save(defaultTenant);
        log.info("Default tenant created: ESEA SA (ID: {})", DEFAULT_TENANT_ID);
    }
}

