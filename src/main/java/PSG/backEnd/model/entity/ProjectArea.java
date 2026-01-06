package PSG.backEnd.model.entity;

import PSG.backEnd.model.entity.gasStation.FuelLoad;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_areas")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProjectArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(100)")
    private String name;

    @Column(columnDefinition = "VARCHAR(500)")
    private String description;

    @Column
    private Boolean active;

    @Column
    private Boolean deleted;

    @OneToMany(mappedBy = "projectArea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionalDocument> transactionalDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "projectArea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FuelLoad> fuelLoads = new ArrayList<>();
}