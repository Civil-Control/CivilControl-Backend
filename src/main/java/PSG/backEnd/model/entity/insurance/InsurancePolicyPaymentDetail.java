package PSG.backEnd.model.entity.insurance;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "insurance_policy_payment_details")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class InsurancePolicyPaymentDetail extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_policy_id", nullable = false)
    private InsurancePolicy insurancePolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_details_id", nullable = false)
    private PaymentDetails paymentDetails;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
