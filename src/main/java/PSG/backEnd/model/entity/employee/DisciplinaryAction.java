package PSG.backEnd.model.entity.employee;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.employee.ActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "disciplinary_actions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DisciplinaryAction extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "reason", nullable = false, length = 500, columnDefinition = "VARCHAR(500)")
    private String reason;

    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "notes", length = 1000, columnDefinition = "VARCHAR(1000)")
    private String notes;
}
