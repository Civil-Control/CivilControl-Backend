package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.enums.vehicle.JurisdictionType;
import PSG.backEnd.model.enums.vehicle.TruckEquipment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "license_plate"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Vehicle extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "license_plate")
    private String licensePlate;

    @Column
    private String brand;

    @Column
    private String model;

    @Column
    private Integer year;

    @Column
    private String color;

    @Column(name = "nick_name")
    private String nickName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building storedInBuilding;

    @Column(name = "vtv_expiration_date")
    private LocalDate vtvExpirationDate;

    @Column(name = "jurisdiction_type")
    @Enumerated(EnumType.STRING)
    private JurisdictionType jurisdictionType;

    @Column(name = "truck_equipment")
    @Enumerated(EnumType.STRING)
    private TruckEquipment truckEquipment;

    /** Soft-delete flag — set to true only for permanent record removal. Never use for operational status. */
    @Column
    private boolean deleted;

    /** Operational status — false means the vehicle is temporarily out of service. Separate from soft-delete. */
    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "vehicle", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<FuelLoad> fuelLoads = new ArrayList<>();
}
