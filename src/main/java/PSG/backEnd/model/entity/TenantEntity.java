package PSG.backEnd.model.entity;

import PSG.backEnd.exception.tenant.TenantAccessViolationException;
import PSG.backEnd.service.util.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Base entity class for multi-tenancy support.
 * All domain entities should extend this class to inherit tenant filtering.
 *
 * Uses Hibernate filters to automatically inject WHERE clauses for tenant isolation.
 * The tenantId is automatically set on entity creation using @PrePersist.
 *
 * @author Maximo Andriola
 * @since 2026-02-18
 */
@MappedSuperclass
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = Long.class)
)
@Filter(
    name = "tenantFilter",
    condition = "tenant_id = :tenantId"
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class TenantEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /**
     * Automatically sets the tenant ID before persisting the entity.
     * Uses the current tenant from TenantContext if tenantId is null.
     */
    @PrePersist
    public void onPrePersist() {
        Long currentTenant = TenantContext.getCurrentTenant();
        if (this.tenantId == null) {
            this.tenantId = currentTenant;
        }
        if (this.tenantId == null || !this.tenantId.equals(currentTenant)) {
            throw new TenantAccessViolationException("Entity tenant does not match the current tenant context");
        }
    }

    @PostLoad
    public void onPostLoad() {
        if (this.tenantId == null || !this.tenantId.equals(TenantContext.getCurrentTenant())) {
            throw new TenantAccessViolationException("Resource not found");
        }
    }
}


