package PSG.backEnd.model.entity;

import PSG.backEnd.service.util.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
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
public abstract class TenantEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * Automatically sets the tenant ID before persisting the entity.
     * Uses the current tenant from TenantContext if tenantId is null.
     */
    @PrePersist
    public void onPrePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenant();
        }
    }
}


