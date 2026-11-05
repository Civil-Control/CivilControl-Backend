package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.treasury.CashBox;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cash_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashPayment extends TenantEntity {

    @Id
    private Long id;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_details_id", nullable = false, unique = true)
    @MapsId
    private PaymentDetails paymentDetails;

    /**
     * Optional source cash box. When {@code null} no cash box movement is registered for this payment.
     * When provided, it must reference an active cash box.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_box_id")
    private CashBox cashBox;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
