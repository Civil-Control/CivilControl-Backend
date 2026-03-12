package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.vehicle.JurisdictionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "licence_plate_payments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LicencePlatePayment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer period;

    @Column(name = "jurisdiction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private JurisdictionType jurisdictionType;
}
