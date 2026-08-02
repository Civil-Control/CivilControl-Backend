package PSG.backEnd.model.entity.sales;

import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "sales_documents",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "branch_code", "document_number", "client_id"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SalesDocument extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private SalesDocumentType documentType;

    @Column(name = "branch_code", columnDefinition = "VARCHAR(5)")
    private String branchCode;

    @Column(name = "document_number", columnDefinition = "VARCHAR(8)")
    private String documentNumber;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "purchase_order_reference", columnDefinition = "VARCHAR(100)")
    private String purchaseOrderReference;

    @Column(name = "net_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal netTotal;

    @Column(name = "iva_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal ivaTotal;

    @Column(name = "iva_exempt_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal ivaExemptTotal;

    @Column(name = "other_taxes", nullable = false, precision = 19, scale = 2)
    private BigDecimal otherTaxes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_task_id")
    private ProjectAreaTask projectAreaTask;

    @OneToMany(mappedBy = "salesDocument", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<SalesItemDetail> items = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean paid = false;

    @Column(columnDefinition = "VARCHAR(500)")
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
