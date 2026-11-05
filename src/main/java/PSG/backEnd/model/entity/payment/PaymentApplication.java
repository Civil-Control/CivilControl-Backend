package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.TransactionalDocument;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Junction table linking a {@link PaymentDetails} to one or more
 * {@link TransactionalDocument}s, recording the exact portion of the payment that is
 * imputed to each document.
 *
 * <p>Business rules (enforced in the service layer):
 * <ul>
 *   <li>{@code amountApplied} must be strictly positive.</li>
 *   <li>For each document, the sum of {@code amountApplied} across all non-deleted payments
 *       plus the sum of credit-note applications must not exceed {@code document.total}.</li>
 *   <li>For each payment, the sum of {@code amountApplied} across all its applications
 *       plus {@code paymentDetails.onAccountAmount} must equal {@code paymentDetails.amount}.</li>
 *   <li>The document and the payment must belong to the same supplier.</li>
 * </ul>
 *
 * <p>Mirrors the {@link PSG.backEnd.model.entity.CreditNoteApplication} pattern.
 */
@Entity
@Table(
        name = "payment_applications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"payment_details_id", "document_id"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentApplication extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_details_id", nullable = false)
    private PaymentDetails payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private TransactionalDocument document;

    @Column(name = "amount_applied", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountApplied;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;
}
