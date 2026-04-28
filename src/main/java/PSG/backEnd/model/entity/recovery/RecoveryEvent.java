package PSG.backEnd.model.entity.recovery;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.treasury.CashBox;
import PSG.backEnd.model.entity.treasury.CashBoxMovement;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable audit record for every operation performed by the Value Recovery feature.
 * Each event is paired one-to-one with a {@link CashBoxMovement} so the cash box balance
 * stays in sync with the recovery ledger.
 *
 * <p>The {@code snapshotPercentage} and {@code snapshotCashBox} are frozen copies taken at
 * the moment the event was generated. Subsequent changes to the originating
 * {@link RecoverySupplierConfig} do not affect historic events.
 */
@Entity
@Table(name = "recovery_events", indexes = {
    @Index(name = "idx_recovery_events_doc", columnList = "transactional_document_id"),
    @Index(name = "idx_recovery_events_tenant_date", columnList = "tenant_id, occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryEvent extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private RecoveryEventType eventType;

    /** Originating document. For credit-note adjustments this is still the original Factura A. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "transactional_document_id", nullable = false)
    private TransactionalDocument transactionalDocument;

    /** Set only when {@code eventType == ADJUSTED_CREDIT_NOTE}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_document_id")
    private TransactionalDocument creditNoteDocument;

    /** Set on REVERSED / ADJUSTED_CREDIT_NOTE — points to the GENERATED event being reversed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverses_event_id")
    private RecoveryEvent reversesEvent;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_config_id", nullable = false)
    private RecoverySupplierConfig supplierConfig;

    @Column(name = "snapshot_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal snapshotPercentage;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_cash_box_id", nullable = false)
    private CashBox snapshotCashBox;

    @Column(name = "document_net", nullable = false, precision = 19, scale = 2)
    private BigDecimal documentNet;

    @Column(name = "document_iva", nullable = false, precision = 19, scale = 2)
    private BigDecimal documentIva;

    /** Signed amount applied to the cash box. Positive on GENERATED, negative on REVERSED / ADJUSTED. */
    @Column(name = "recovered_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal recoveredAmount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_box_movement_id", nullable = false)
    private CashBoxMovement cashBoxMovement;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "triggered_by_user_id", nullable = false)
    private Long triggeredByUserId;
}
