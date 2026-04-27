package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.treasury.BankAccount;
import PSG.backEnd.model.entity.treasury.Checkbook;
import PSG.backEnd.model.enums.payment.CheckStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** Persisted operational status. {@code VENCIDO} is derived at read-time, never stored. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CheckStatus status = CheckStatus.PENDIENTE;

    /** Date the check was actually settled (cobrado / rechazado / cancelado). */
    @Column(name = "settled_date")
    private LocalDate settledDate;

    /** Free-form note explaining the status transition (reason for rejection, cancellation, etc.). */
    @Column(name = "status_comment", length = 500)
    private String statusComment;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    /** ID of the user that performed the last status transition (audit trail). */
    @Column(name = "status_changed_by")
    private Long statusChangedByUserId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
