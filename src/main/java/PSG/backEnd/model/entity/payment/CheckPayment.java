package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.treasury.BankAccount;
import PSG.backEnd.model.entity.treasury.Checkbook;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "check_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckPayment extends TenantEntity {

    @Id
    private Long id;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_details_id", nullable = false, unique = true)
    @MapsId
    private PaymentDetails paymentDetails;

    @Column(name = "check_number", length = 50, columnDefinition = "VARCHAR(50)")
    private String checkNumber;

    /** Issuing bank account — replaces the legacy free-form bankName field. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    /** Optional checkbook that restricts the allowed check numbers. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkbook_id")
    private Checkbook checkbook;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
