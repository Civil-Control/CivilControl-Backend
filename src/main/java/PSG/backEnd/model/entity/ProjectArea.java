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

    /**
     * Hidden Feature 18 (Value Recovery) flag.
     * Set automatically by {@code ProjectAreaService.create()} when the area name is
     * exactly "Recupero" (case-insensitive, trimmed) and the tenant does not yet have
     * a recovery sector. Persistent: once set, renaming the area does not clear it.
     * Database guarantees at most one active recovery sector per tenant
     * (partial unique index {@code uk_project_areas_recovery_per_tenant}).
     */
    @Column(name = "is_recovery_sector", nullable = false)
    @Builder.Default
    private Boolean isRecoverySector = false;

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