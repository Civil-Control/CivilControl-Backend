package PSG.backEnd.model.entity.insurance;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policy_vehicles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PolicyVehicle extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auto_policy_id", nullable = false)
    private AutoPolicy autoPolicy;

    @Column(name = "sum_insured", precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;

    @Column(name = "number_of_installments")
    private Integer numberOfInstallments;

    @Column(name = "premio_total", precision = 15, scale = 2)
    private BigDecimal premioTotal;

    @Column(name = "premio_mensual", precision = 15, scale = 2)
    private BigDecimal premioMensual;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
