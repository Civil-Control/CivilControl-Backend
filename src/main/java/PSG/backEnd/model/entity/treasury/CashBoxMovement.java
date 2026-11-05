package PSG.backEnd.model.entity.treasury;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.payment.CashPayment;
import PSG.backEnd.model.enums.treasury.CashBoxMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_box_movements", indexes = {
        @Index(name = "idx_cash_box_movements_box", columnList = "cash_box_id, movement_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashBoxMovement extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_box_id", nullable = false)
    private CashBox cashBox;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CashBoxMovementType type;

    /** Always positive; the sign is derived from the movement type (or from the adjustment delta). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** The actual signed delta applied to the cash box (positive or negative). */
    @Column(name = "signed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal signedAmount;

    /** Snapshot of the cash box balance after this movement was applied. */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    /** Required when {@code type == AJUSTE}; optional otherwise. */
    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    /** Origin payment if the movement was triggered by a {@link CashPayment}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_payment_id")
    private CashPayment cashPayment;
}
