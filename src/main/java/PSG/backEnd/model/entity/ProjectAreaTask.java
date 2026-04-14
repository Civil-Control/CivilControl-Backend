package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_area_tasks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "project_area_id", "name"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class ProjectAreaTask extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id", nullable = false)
    private ProjectArea projectArea;

    @Column(nullable = false, columnDefinition = "VARCHAR(150)")
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
