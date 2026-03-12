package PSG.backEnd.model.entity.serviceSupplier;

import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_suppliers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ServiceSupplier extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "service_supplier_services",
            joinColumns = @JoinColumn(name = "service_supplier_id")
    )
    @Column(name = "service_type", nullable = false)
    @Builder.Default
    private List<ServiceType> providedServices = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted;
}