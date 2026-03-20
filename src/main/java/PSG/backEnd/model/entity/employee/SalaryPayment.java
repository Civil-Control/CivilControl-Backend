package PSG.backEnd.model.entity.employee;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_payments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SalaryPayment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_frequency", nullable = false)
    private SalaryFrecuency salaryFrequency;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;
}
