package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.enums.vehicle.RepairType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repairs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Repair extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column()
    private BigDecimal cost;

    @Column(columnDefinition = "TEXT")
    private String description;

    // if the repair was done by an employee
    @Column(columnDefinition = "VARCHAR(100)")
    private String employee;

    // if the repair was done by an external supplier
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ElementCollection
    @CollectionTable(name = "repair_types", joinColumns = @JoinColumn(name = "repair_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<RepairType> repairTypes = new ArrayList<>();

    /** Nullable — present only when this repair was completed from a RepairOrder */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_order_id")
    private RepairOrder repairOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactional_document_id")
    private TransactionalDocument transactionalDocument;

    @Column(name = "document_sort_order", nullable = false)
    @Builder.Default
    private Integer documentSortOrder = 0;

}
