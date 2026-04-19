package PSG.backEnd.model.entity.insurance;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "insurance_policies", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "policy_number"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class InsurancePolicy extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(name = "policy_number", nullable = false, length = 50)
    private String policyNumber;

    @Column(name = "term_number", length = 20)
    private String termNumber;

    @Column(name = "endorsement_sequence", length = 20)
    private String endorsementSecuence;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_status", nullable = false)
    private PolicyStatus policyStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency")
    private PaymentFrequency paymentFrequency;

    @Column(name = "sum_insured", precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;

    @Column(name = "number_of_installments")
    private Integer numberOfInstallments;

    @Column(name = "premio_total", precision = 15, scale = 2)
    private BigDecimal premioTotal;

    @Column(name = "premio_mensual", precision = 15, scale = 2)
    private BigDecimal premioMensual;

    @Column(name = "periodic_due_day")
    private Integer periodicDueDay;

    @Builder.Default
    @Column(name = "due_at_start_of_period", nullable = false)
    private Boolean dueAtStartOfPeriod = false;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @OneToOne(mappedBy = "insurancePolicy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AutoPolicy autoPolicy;
}
