package PSG.backEnd.model.entity.ledger;

import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Client-side counterpart of {@link AccountImputation}. Links a credit note's movement (origin)
 * to an invoice/debit-note's movement (destination) with the amount applied. No {@code onAccount}
 * flag here — there is no client-side payment/on-account flow yet.
 */
@Entity
@Table(
        name = "client_account_imputations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"origin_movement_id", "destination_movement_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClientAccountImputation extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_movement_id", nullable = false)
    private ClientAccountMovement originMovement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_movement_id", nullable = false)
    private ClientAccountMovement destinationMovement;

    @Column(name = "amount_applied", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountApplied;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void setCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
