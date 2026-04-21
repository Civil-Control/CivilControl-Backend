package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.documents.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;


@Entity
@Table(
        name = "transactional_documents",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "branch_code", "document_number", "supplier_id"})
        }
)@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TransactionalDocument extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "branch_code", nullable = false)
    private String branchCode;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "other_taxes", nullable = false)
    private BigDecimal otherTaxes;

    @Column(name = "net_total", nullable = false)
    private BigDecimal netTotal;

    @Column(name = "iva_total", nullable = false)
    private BigDecimal ivaTotal;

    @Column(name = "iva_exempt_total", nullable = false)
    private BigDecimal ivaExemptTotal;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "discount_percentage", nullable = false)
    private BigDecimal discountPercentage;

    @Column(columnDefinition = "VARCHAR(500)")
    private String comment;

    @Column(nullable = false)
    private Boolean paid;

    @Column(nullable = false)
    private Boolean deleted = false;

    @OneToMany(mappedBy = "document", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<ItemDetail> items = new ArrayList<>();

    /**
     * Credit note applications where this document IS the credit note.
     * Populated for documents of type CREDIT_NOTE_*; empty otherwise.
     * Cascade ALL + orphanRemoval lets the service replace the entire set on update.
     */
    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CreditNoteApplication> creditNoteApplications = new HashSet<>();

    /**
     * Credit note applications where this document IS the invoice/debit note being credited.
     * Populated for documents of type BILL_* or DEBIT_NOTE_* that have credits applied to them.
     * Read-only from this side; managed via the credit note's {@link #creditNoteApplications}.
     */
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CreditNoteApplication> appliedCredits = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_task_id")
    private ProjectAreaTask projectAreaTask;

    // Helper method to maintain bidirectional relationship
    public void addItemDetail(ItemDetail itemDetail) {
        items.add(itemDetail);
        itemDetail.setDocument(this);
    }

    // Helper method to remove item detail
    public void removeItemDetail(ItemDetail itemDetail) {
        items.remove(itemDetail);
        itemDetail.setDocument(null);
    }
}