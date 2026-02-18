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

    private static final Long DEFAULT_TENANT = 1L;

    /**
     * Sets the current tenant ID for the current thread.
     *
     * @param tenantId the tenant identifier
     */
    public static void setCurrentTenant(Long tenantId) {
        log.debug("Setting tenant context to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the current tenant ID from the thread context.
     * Returns the default tenant if none is set.
     *
     * @return the current tenant ID or default tenant
     */
    public static Long getCurrentTenant() {
        Long tenant = CURRENT_TENANT.get();
        if (tenant == null) {
            log.debug("No tenant in context, using default: {}", DEFAULT_TENANT);
            return DEFAULT_TENANT;
        }
        return tenant;
    }

    /**
     * Clears the tenant context for the current thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        log.debug("Clearing tenant context");
        CURRENT_TENANT.remove();
    }

    /**
     * Gets the default tenant ID.
     *
     * @return the default tenant identifier
     */
    public static Long getDefaultTenant() {
        return DEFAULT_TENANT;
    }
}


