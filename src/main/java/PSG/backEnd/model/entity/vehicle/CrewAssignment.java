package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.employee.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "crew_assignments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CrewAssignment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_report_id")
    private DailyCrewReport crewReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_task_id")
    private ProjectAreaTask projectAreaTask;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "is_driver", nullable = false)
    private boolean driver;

    @Column(name = "observation", length = 500)
    private String observation;

    @Column
    private Integer km;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "return_time")
    private LocalTime returnTime;

    @Column(nullable = false)
    private boolean deleted;
}
