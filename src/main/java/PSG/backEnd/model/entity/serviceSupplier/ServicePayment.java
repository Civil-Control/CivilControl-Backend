package PSG.backEnd.model.entity.serviceSupplier;

import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.ServiceType;
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
    @JoinColumn(name = "service_supplier_id", nullable = false)
    private ServiceSupplier serviceSupplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_number", columnDefinition = "VARCHAR(100)")
    private String referenceNumber;

    @Column(length = 500, columnDefinition = "VARCHAR(500)")
    private String comment;

    @Column(nullable = false)
    private boolean deleted;
}
