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
// Uniqueness on (tenant_id, key) is enforced by a partial index scoped to deleted = false
// (see V100), so a soft-deleted row's key can be reused. JPA can't express a partial
// unique constraint declaratively, so it isn't declared here.
@Entity
@Table(name = "custom_fuel_types")
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
