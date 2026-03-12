package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TenantEntity;
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

    @Column(name = "bank_name", length = 100, columnDefinition = "VARCHAR(100)")
    private String bankName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
