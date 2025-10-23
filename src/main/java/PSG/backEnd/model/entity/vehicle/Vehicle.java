package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.enums.vehicle.JurisdictionType;
import PSG.backEnd.model.enums.vehicle.TruckEquipment;
import PSG.backEnd.model.enums.vehicle.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, name = "license_plate")
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

    @Column(name = "vehicle_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @Column(name = "stored_in")
    private String storedIn;

    @Column(name = "vtv_expiration_date")
    private LocalDate vtvExpirationDate;

    @Column(name = "jurisdiction_type")
    private JurisdictionType jurisdictionType;

    @Column(name = "truck_equipment")
    @Enumerated(EnumType.STRING)
    private TruckEquipment truckEquipment;

    @Column
    private boolean deleted;

    @OneToMany(mappedBy = "vehicle", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<FuelLoad> fuelLoads = new ArrayList<>();
}
