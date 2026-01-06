package PSG.backEnd.model.entity.gasStation;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.vehicle.FuelType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fuel_loads")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FuelLoad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "branch_code", nullable = false, columnDefinition = "VARCHAR(10)")
    private String branchCode;

    @Column(name = "ticket_number", nullable = false, columnDefinition = "VARCHAR(50)")
    private String ticketNumber;

    @Column(name = "fuel_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    @Column(nullable = false)
    private BigDecimal liters;

    @Column(nullable = false)
    private BigDecimal pricePerLiter;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gas_station_id")
    private GasStation gasStation;
}
