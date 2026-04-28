package PSG.backEnd.model.entity.recovery;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.treasury.CashBox;
import PSG.backEnd.model.enums.recovery.RecoveryBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-supplier configuration for the hidden Feature 18 (Value Recovery).
 * <p>Belongs to a {@link ProjectArea} flagged as the tenant's recovery sector
 * ({@code isRecoverySector = true}). Defines the percentage of the invoice's net to
 * recover and the destination {@link CashBox} where the automatic movement will be
 * registered. The IVA portion is always recovered at 100 %.
 *
 * <p>Configurations are soft-deleted via the {@code deleted} flag and can be temporarily
 * disabled by flipping {@code active} to {@code false} without losing historic events.
 */
@Entity
@Table(
    name = "recovery_supplier_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recovery_supplier_configs",
        columnNames = {"tenant_id", "project_area_id", "supplier_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoverySupplierConfig extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id", nullable = false)
    private ProjectArea projectArea;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_box_id", nullable = false)
    private CashBox cashBox;

    /** Percentage to recover, applied over {@link #recoveryBase}. Range [0.00, 100.00]. */
    @Column(name = "recovery_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal recoveryPercentage;

    /**
     * Calculation base the {@link #recoveryPercentage} applies to. Defaults to
     * {@link RecoveryBase#NET} (legacy formula: {@code (net * %) + iva}). Switching to
     * {@link RecoveryBase#TOTAL} produces a flat {@code (net + iva) * %} computation.
     * The chosen base is snapshotted on each generated event so historic recoveries are
     * never affected by later edits to this config.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_base", nullable = false, length = 10)
    @Builder.Default
    private RecoveryBase recoveryBase = RecoveryBase.NET;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
