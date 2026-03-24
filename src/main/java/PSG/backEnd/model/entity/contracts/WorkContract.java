package PSG.backEnd.model.entity.contracts;

import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.contracts.WorkContractStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "work_contracts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "contract_number"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class WorkContract extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", nullable = false, length = 50)
    private String contractNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "contracted_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal contractedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    @Builder.Default
    private Currency currency = Currency.ARS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private WorkContractStatus status = WorkContractStatus.ACTIVO;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
