package PSG.backEnd.model.entity.employee;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee_vacations")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EmployeeVacation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(length = 500)
    private String observations;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}

