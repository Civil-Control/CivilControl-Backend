package PSG.backEnd.model.entity.payment;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cash_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashPayment {

    @Id
    private Long id;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_details_id", nullable = false, unique = true)
    @MapsId
    private PaymentDetails paymentDetails;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
