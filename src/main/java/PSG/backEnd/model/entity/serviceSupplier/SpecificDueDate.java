package PSG.backEnd.model.entity.serviceSupplier;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "specific_due_dates")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SpecificDueDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_assignment_id", nullable = false)
    private ServiceAssignment serviceAssignment;
}
