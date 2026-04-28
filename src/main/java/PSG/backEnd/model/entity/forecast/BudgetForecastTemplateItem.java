package PSG.backEnd.model.entity.forecast;

import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Espejo simplificado de {@link BudgetForecastItem} para plantillas frecuentes.
 * Reemplaza expectedDate absoluta por dayOffset (relativo a periodFrom al instanciar)
 * y elimina el estado de aplicación.
 */
@Entity
@Table(name = "budget_forecast_template_items", indexes = {
    @Index(name = "idx_bf_tmpl_items_template", columnList = "template_id, row_order")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetForecastTemplateItem extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private BudgetForecastTemplate template;

    @Column(name = "row_order", nullable = false)
    private Integer rowOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private BudgetForecastItemType itemType;

    @Column(nullable = false, length = 500)
    private String description;

    /** Días desde periodFrom para calcular expectedDate al instanciar. */
    @Column(name = "day_offset", nullable = false)
    private Integer dayOffset;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

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
}
