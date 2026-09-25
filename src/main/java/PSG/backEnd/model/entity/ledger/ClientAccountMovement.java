package PSG.backEnd.model.entity.ledger;

import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.enums.ledger.ClientAccountMovementType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Client-side counterpart of {@link AccountMovement}. One signed movement per sales document
 * (invoices/debit notes positive, credit notes negative), independent of whether a credit note
 * is linked to specific invoices — this is what lets a credit note reduce the client's balance
 * from the moment it's created. No PAYMENT/RETENTION movement types exist here because there is
 * no client-side Payment entity yet.
 */
@Entity
@Table(name = "client_account_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClientAccountMovement extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "movement_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ClientAccountMovementType movementType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private SalesDocument sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private ClientAccountMovement reversalOf;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void setCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
