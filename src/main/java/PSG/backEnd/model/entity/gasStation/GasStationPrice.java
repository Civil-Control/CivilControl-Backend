package PSG.backEnd.model.entity.gasStation;

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

    /** Either a built-in FuelType enum constant name or a CustomFuelType key. */
    @Column(name = "fuel_type", nullable = false, length = 50)
    private String fuelType;

    @Column(name = "price", nullable = false, precision = 10, scale = 3)
    private BigDecimal price;
}
