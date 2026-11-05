package PSG.backEnd.model.entity.treasury;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.payment.CheckPayment;
import PSG.backEnd.model.entity.payment.TransferPayment;
import PSG.backEnd.model.enums.treasury.BankAccountMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_account_movements", indexes = {
        @Index(name = "idx_bank_account_movements_acc", columnList = "bank_account_id, movement_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountMovement extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BankAccountMovementType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "signed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal signedAmount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_payment_id")
    private CheckPayment checkPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_payment_id")
    private TransferPayment transferPayment;
}
