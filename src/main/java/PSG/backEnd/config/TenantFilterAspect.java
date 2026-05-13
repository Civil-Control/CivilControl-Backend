package PSG.backEnd.config;

import PSG.backEnd.service.util.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Aspect to enable tenant filtering on repository operations.
 * Automatically enables the Hibernate 'tenantFilter' before any repository method execution.
 *
 * Excludes repositories whose entities do NOT have @Filter / @FilterDef
 * (e.g., PermissionRepository, TenantRepository) to avoid unnecessary filter activation.
 *
 * @author Maximo Andriola
 * @since 2026-02-18
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilterAspect {

    private final EntityManager entityManager;

    /**
     * Enables tenant filter before any repository method execution,
     * excluding repositories that operate on non-tenant entities.
     * Sets the current tenant ID from TenantContext as the filter parameter.
     */
    @Before("execution(* PSG.backEnd.repository..*(..)) " +
            "&& !execution(* PSG.backEnd.repository.PermissionRepository.*(..)) " +
            "&& !execution(* PSG.backEnd.repository.TenantRepository.*(..)) " +
            "&& !execution(* PSG.backEnd.repository.OrphanBiometricLogRepository.*(..)) " +
            "&& !execution(* PSG.backEnd.repository.NotificationAlertRepository.*(..))")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.getCurrentTenant();

        Session session = entityManager.unwrap(Session.class);

        // Enable the filter and set the parameter
        session.enableFilter("tenantFilter")
               .setParameter("tenantId", tenantId);

        log.debug("Tenant filter enabled for tenant: {}", tenantId);
    }
}


