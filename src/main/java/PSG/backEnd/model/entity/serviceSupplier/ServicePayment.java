package PSG.backEnd.model.entity.serviceSupplier;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "service_payments",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"tenant_id", "reference_number"}, name = "uk_service_payment_reference_number")
       })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ServicePayment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_assignment_id", nullable = false)
    private ServiceAssignment serviceAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column
    private Integer year;

    @Column
    private Integer period;

    @Column(name = "reference_number", columnDefinition = "VARCHAR(100)")
    private String referenceNumber;

    @Column(length = 500, columnDefinition = "VARCHAR(500)")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean deleted;
}
