package PSG.backEnd.model.entity.employee;

import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.employee.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "employee_id", "date", "time", "movement_type"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AttendanceRecord extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType movementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "observation", length = 500)
    private String observation;
}
