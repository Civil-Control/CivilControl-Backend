package PSG.backEnd.model.entity;

import PSG.backEnd.model.entity.sales.SalesDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Junction table linking a sales Credit Note to one or more Invoices/Debit Notes that it
 * applies to. Mirrors {@link CreditNoteApplication} (purchases side) for {@link SalesDocument}.
 *
 * Business rules (enforced in service layer):
 * <ul>
 *   <li>The {@code creditNote} must be of type NOTA_CREDITO_*.</li>
 *   <li>The {@code invoice} must be of type FACTURA_* or NOTA_DEBITO_*.</li>
 *   <li>Both documents must belong to the same client.</li>
 *   <li>Sum of {@code amountApplied} per credit note must not exceed the credit note's total.</li>
 *   <li>Sum of {@code amountApplied} per invoice must not exceed the invoice's total.</li>
 * </ul>
 */
@Entity
@Table(
        name = "sales_credit_note_applications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"credit_note_id", "invoice_id"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class SalesCreditNoteApplication extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private SalesDocument creditNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private SalesDocument invoice;

    /** Amount of the credit note that is applied to this specific invoice. */
    @Column(name = "amount_applied", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountApplied;
}
