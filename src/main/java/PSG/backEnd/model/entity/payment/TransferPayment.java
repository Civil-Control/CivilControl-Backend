package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.treasury.BankAccount;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transfer_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferPayment extends TenantEntity {

    @Id
    private Long id;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_details_id", nullable = false, unique = true)
    @MapsId
    private PaymentDetails paymentDetails;

    @Column(name = "transaction_number", length = 100, columnDefinition = "VARCHAR(100)")
    private String transactionNumber;

    /** Source bank account — replaces the legacy free-form bankName field. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
