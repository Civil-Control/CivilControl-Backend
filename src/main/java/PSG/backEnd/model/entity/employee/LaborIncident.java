package PSG.backEnd.model.entity.employee;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.employee.LaborIncidentStatus;
import PSG.backEnd.model.enums.employee.LaborIncidentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "labor_incidents")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LaborIncident extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false)
    private LaborIncidentType incidentType;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "description", nullable = false, length = 1000, columnDefinition = "VARCHAR(1000)")
    private String description;

    @Column(name = "financial_impact", precision = 15, scale = 2)
    private BigDecimal financialImpact;

    @Column(name = "affected_asset", length = 500, columnDefinition = "VARCHAR(500)")
    private String affectedAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LaborIncidentStatus status;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "notes", length = 1000, columnDefinition = "VARCHAR(1000)")
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "labor_incident_employees",
        joinColumns = @JoinColumn(name = "labor_incident_id"),
        inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();
}
