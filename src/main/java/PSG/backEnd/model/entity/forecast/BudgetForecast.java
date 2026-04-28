package PSG.backEnd.model.entity.forecast;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.forecast.BudgetForecastStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Previsión de gastos por período. Funciona como una "planilla" separada de los registros
 * reales: cargar una previsión NO crea SalaryPayment/ServicePayment/etc. Para materializarla
 * el usuario aplica cada item (o batch), lo que delega en un Applier que crea el registro
 * real en la entidad correspondiente y deja el item trazado al ID generado.
 */
@Entity
@Table(name = "budget_forecasts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetForecast extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BudgetForecastStatus status = BudgetForecastStatus.BORRADOR;

    /** Cacheado, recalculado al modificar items. */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Suma de items con applicationStatus == APLICADO. */
    @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    /** FK informativa a BudgetForecastTemplate (opcional). */
    @Column(name = "created_from_template_id")
    private Long createdFromTemplateId;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by_user_id")
    private Long confirmedByUserId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "budgetForecast", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetForecastItem> items = new ArrayList<>();
}
