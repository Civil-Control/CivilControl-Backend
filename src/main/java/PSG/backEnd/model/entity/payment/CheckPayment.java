package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TenantEntity;
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

    @Column(name = "bank_name", length = 100, columnDefinition = "VARCHAR(100)")
    private String bankName;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
