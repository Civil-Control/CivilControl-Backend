package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "payment_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDetails extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Legacy join table — kept in place for back-compat reads (PaymentRepository uses it
     * to find which payment touched a document). Service layer dual-writes to this list
     * AND to {@link #applications}; the latter is the authoritative source for
     * "how much was applied".
     */
    @ManyToMany
    @JoinTable(name = "payment_details_paid_documents",
            joinColumns = @JoinColumn(name = "payment_details_id"),
            inverseJoinColumns = @JoinColumn(name = "transactional_document_id"))
    private List<TransactionalDocument> paidDocuments = new ArrayList<>();

    /**
     * Per-document imputation of this payment. Authoritative source of truth for how
     * the payment amount is distributed: every row carries an explicit {@code amountApplied}.
     * The sum of {@code amountApplied} across this set plus {@link #onAccountAmount} must
     * equal {@link #amount}.
     */
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PaymentApplication> applications = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * Portion of {@link #amount} that is left as supplier credit (saldo a favor del proveedor).
     * Defaults to zero. Always non-negative; constrained at the DB level too.
     */
    @Column(name = "on_account_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal onAccountAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String comment;

    @OneToOne(mappedBy = "paymentDetails")
    private CashPayment cashPayment;

    @OneToOne(mappedBy = "paymentDetails")
    private TransferPayment transferPayment;

    @OneToOne(mappedBy = "paymentDetails")
    private CheckPayment checkPayment;
}
