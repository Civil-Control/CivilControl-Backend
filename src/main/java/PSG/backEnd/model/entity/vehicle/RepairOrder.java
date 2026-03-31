package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repair_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class RepairOrder extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "repair_order_items", joinColumns = @JoinColumn(name = "repair_order_id"))
    @Column(name = "item", nullable = false)
    @Builder.Default
    private List<String> items = new ArrayList<>();

    @Column(name = "reported_by", length = 100)
    private String reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RepairOrderStatus status = RepairOrderStatus.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
