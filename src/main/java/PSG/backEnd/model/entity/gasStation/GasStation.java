package PSG.backEnd.model.entity.gasStation;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gas_stations")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GasStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @OneToMany(mappedBy = "gasStation", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<FuelLoad> fuelLoads = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "gas_station_prices",
        joinColumns = @JoinColumn(name = "gas_station_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"gas_station_id", "fuel_type"})
    )
    @Builder.Default
    private List<GasStationPrice> prices = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted;
}
