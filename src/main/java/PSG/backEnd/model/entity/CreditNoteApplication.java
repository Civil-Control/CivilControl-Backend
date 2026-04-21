package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Junction table linking a Credit Note to one or more Invoices/Debit Notes
 * that it applies to. Each row represents the portion of a Credit Note's total
 * that is applied to a specific Invoice/Debit Note.
 *
 * Business rules (enforced in service layer):
 * <ul>
 *   <li>The {@code creditNote} must be of type CREDIT_NOTE_*.</li>
 *   <li>The {@code invoice} must be of type BILL_* or DEBIT_NOTE_*.</li>
 *   <li>Both documents must belong to the same supplier.</li>
 *   <li>Sum of {@code amountApplied} per credit note must not exceed the credit note's total.</li>
 *   <li>Sum of {@code amountApplied} per invoice must not exceed the invoice's total.</li>
 * </ul>
 */
@Entity
@Table(
        name = "credit_note_applications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"credit_note_id", "invoice_id"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class CreditNoteApplication extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private TransactionalDocument creditNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private TransactionalDocument invoice;

    /** Amount of the credit note that is applied to this specific invoice. */
    @Column(name = "amount_applied", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountApplied;
}
