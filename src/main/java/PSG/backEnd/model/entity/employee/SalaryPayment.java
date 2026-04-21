package PSG.backEnd.model.entity.employee;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.TransactionalDocument;
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

    /** IVA percentage applied to this salary payment (default 21). */
    @Column(name = "iva_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ivaPercentage = new BigDecimal("21.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_task_id")
    private ProjectAreaTask projectAreaTask;

    /** Nullable — linked purchase document for this salary payment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactional_document_id")
    private TransactionalDocument transactionalDocument;

    @Column(name = "document_sort_order", nullable = false)
    @Builder.Default
    private Integer documentSortOrder = 0;
}
