package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "vehicle_types", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class VehicleType extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 100)
    private String description;

    @Column(name = "requires_truck_equipment", nullable = false)
    private boolean requiresTruckEquipment = false;
}

