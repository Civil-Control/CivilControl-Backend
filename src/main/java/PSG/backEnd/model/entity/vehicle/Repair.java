package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repairs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Repair extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Integer mileage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RepairItem> items = new ArrayList<>();

    /** Nullable — present only when this repair was completed from a RepairOrder */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_order_id")
    private RepairOrder repairOrder;

}
