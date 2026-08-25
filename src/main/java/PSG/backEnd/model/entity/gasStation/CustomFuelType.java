package PSG.backEnd.model.entity.gasStation;

import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * A tenant-defined fuel type that supplements the built-in {@link PSG.backEnd.model.enums.vehicle.FuelType}
 * enum. GasStationPrice.fuelType and FuelLoad.fuelType are plain strings that can hold either
 * a built-in enum constant name or a custom type's {@code key}.
 */
@Entity
@Table(name = "custom_fuel_types", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "key"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class CustomFuelType extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key", nullable = false, length = 50)
    private String key;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
