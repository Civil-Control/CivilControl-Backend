package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column
    private Boolean active;

    @Column
    private Boolean deleted;
}