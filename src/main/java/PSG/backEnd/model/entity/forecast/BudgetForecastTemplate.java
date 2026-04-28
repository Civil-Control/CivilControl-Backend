package PSG.backEnd.model.entity.forecast;

import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla frecuente de previsión. Permite instanciar nuevas previsiones con items
 * pre-cargados (clonados) y fechas calculadas a partir de un offset de días.
 */
@Entity
@Table(name = "budget_forecast_templates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"}, name = "uk_bf_template_tenant_name")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetForecastTemplate extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Sugerencia al crear desde plantilla; el usuario puede ajustar el rango. */
    @Column(name = "default_period_days", nullable = false)
    @Builder.Default
    private Integer defaultPeriodDays = 7;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetForecastTemplateItem> items = new ArrayList<>();
}
