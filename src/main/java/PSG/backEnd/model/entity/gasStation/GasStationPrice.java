package PSG.backEnd.model.entity.gasStation;

import PSG.backEnd.model.enums.vehicle.FuelType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GasStationPrice {

    @Column(name = "fuel_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    @Column(name = "price", nullable = false, precision = 10, scale = 3)
    private BigDecimal price;
}
