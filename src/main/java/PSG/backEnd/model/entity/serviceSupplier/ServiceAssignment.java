package PSG.backEnd.model.entity.serviceSupplier;

import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_assignments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ServiceAssignment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubjectType subjectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_supplier_id", nullable = false)
    private ServiceSupplier serviceSupplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "estimated_due_day")
    private Integer estimatedDueDay;

    @Column(name = "account_holder", length = 200)
    private String accountHolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_location_id")
    private Building paymentLocation;

    @Column(nullable = false)
    private boolean deleted;
}
