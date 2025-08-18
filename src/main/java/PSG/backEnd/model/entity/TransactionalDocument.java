package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "transactional_documents")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TransactionalDocument {

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
    private BigDecimal otherTaxes = BigDecimal.ZERO;

    @Column(name = "net_total", nullable = false)
    private BigDecimal netTotal = BigDecimal.ZERO;

    @Column(name = "iva_total", nullable = false)
    private BigDecimal ivaTotal = BigDecimal.ZERO;

    @Column(name = "iva_exempt_total", nullable = false)
    private BigDecimal ivaExemptTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "discount_percentage", nullable = false)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    private String comment;

    @Column(nullable = false)
    private Boolean deleted = false;
}