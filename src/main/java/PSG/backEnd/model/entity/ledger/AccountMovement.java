package PSG.backEnd.model.entity.ledger;

import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.enums.ledger.AccountMovementType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountMovement extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "movement_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountMovementType movementType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private TransactionalDocument sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_payment_id")
    private PaymentDetails sourcePayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private AccountMovement reversalOf;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void setCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
