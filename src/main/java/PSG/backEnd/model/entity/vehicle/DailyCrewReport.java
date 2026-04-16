package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.vehicle.CrewReportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_crew_reports")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DailyCrewReport extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private CrewReportType type;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "return_time")
    private LocalTime returnTime;

    @Column(nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "crewReport", fetch = FetchType.LAZY)
    @Builder.Default
    private List<CrewAssignment> assignments = new ArrayList<>();
}
