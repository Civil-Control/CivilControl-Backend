package PSG.backEnd.model.entity.contracts;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "certifications")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Certification extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certification_number", nullable = false)
    private Integer certificationNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "work_contract_id", nullable = false)
    private WorkContract contract;

    @Column(name = "certification_date", nullable = false)
    private LocalDate certificationDate;

    @Column(name = "certified_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal certifiedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_document_id")
    private SalesDocument salesDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private CertificationStatus status = CertificationStatus.PRESENTADO;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
