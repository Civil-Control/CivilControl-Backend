package PSG.backEnd.model.entity.forecast;

import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ítem individual dentro de una previsión. Sus campos opcionales (employee, supplier,
 * serviceAssignment, vehicle, stock) se requieren según el {@link BudgetForecastItemType}.
 * La validación se realiza en {@code BudgetForecastItemValidator}.
 */
@Entity
@Table(name = "budget_forecast_items", indexes = {
    @Index(name = "idx_bf_items_forecast", columnList = "budget_forecast_id, row_order"),
    @Index(name = "idx_bf_items_status", columnList = "application_status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetForecastItem extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_forecast_id", nullable = false)
    private BudgetForecast budgetForecast;

    @Column(name = "row_order", nullable = false)
    private Integer rowOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private BudgetForecastItemType itemType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    // ── Referencias opcionales según itemType ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Referencia única que define proveedor + servicio + (edificio | vehículo) — usada por SERVICIO y PATENTE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_assignment_id")
    private ServiceAssignment serviceAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(name = "stock_quantity", precision = 19, scale = 4)
    private BigDecimal stockQuantity;

    // ── Estado de aplicación ──

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 30)
    @Builder.Default
    private BudgetForecastItemApplicationStatus applicationStatus = BudgetForecastItemApplicationStatus.PENDIENTE;

    @Column(name = "applied_entity_type", length = 30)
    private String appliedEntityType;

    @Column(name = "applied_entity_id")
    private Long appliedEntityId;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "applied_by_user_id")
    private Long appliedByUserId;

    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
