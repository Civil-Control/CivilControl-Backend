package PSG.backEnd.service.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for tenant ID.
 * Manages the current tenant context for multi-tenancy support.
 *
 * @author Maximo Andriola
 * @since 2026-02-18
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    public static final Long PLATFORM_TENANT_ID = 1L;

    /**
     * Sets the current tenant ID for the current thread.
     *
     * @param tenantId the tenant identifier
     */
    public static void setCurrentTenant(Long tenantId) {
        log.debug("Setting tenant context to: {}", tenantId);
        if (tenantId == null) {
            CURRENT_TENANT.remove();
            return;
        }
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the current tenant ID from the thread context.
     *
     * @return the current tenant ID or {@code null} if none is bound
     */
    public static Long getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static boolean isPlatformTenant() {
        return PLATFORM_TENANT_ID.equals(CURRENT_TENANT.get());
    }

    /**
     * Clears the tenant context for the current thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        log.debug("Clearing tenant context");
        CURRENT_TENANT.remove();
    }
}
