package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.BuildingType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buildings")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // Name of the building

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Unique code for the building

    @Embedded
    private Address address; // Complete address of the building

    @Enumerated(EnumType.STRING)
    @Column(name = "building_type", nullable = false)
    private BuildingType buildingType; // Type of the building (e.g., Planta, Almacén, Oficina)

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true; // Indicates if the building is active or inactive

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false; // Soft delete flag

    // Relationships
    // private List<TaxOrService> taxesAndServices;  // List of taxes and services associated with the building unused until further notice

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Stock> stockItems = new ArrayList<>(); // List of stock items in the building
}
