package PSG.backEnd.model.entity;

import PSG.backEnd.model.entity.gasStation.FuelLoad;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

@Entity
@Table(name = "project_areas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProjectArea extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "VARCHAR(100)")
    private String name;

    @Column(columnDefinition = "VARCHAR(500)")
    private String description;

    @Column
    private Boolean active;

    @Column
    private Boolean deleted;

    @Column(columnDefinition = "VARCHAR(20)")
    private String color;

    @OneToMany(mappedBy = "projectArea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionalDocument> transactionalDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "projectArea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FuelLoad> fuelLoads = new ArrayList<>();

    @OneToMany(mappedBy = "projectArea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProjectAreaTask> tasks = new ArrayList<>();
}