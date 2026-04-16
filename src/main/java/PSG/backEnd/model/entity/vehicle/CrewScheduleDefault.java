package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "crew_schedule_defaults")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CrewScheduleDefault extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id", nullable = false)
    private ProjectArea projectArea;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "return_time")
    private LocalTime returnTime;
}
