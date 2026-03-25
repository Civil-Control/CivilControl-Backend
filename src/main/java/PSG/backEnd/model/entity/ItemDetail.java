package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "item_details")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ItemDetail extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private TransactionalDocument document;

    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "iva_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPercentage;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "document_sort_order", nullable = false)
    @Builder.Default
    private Integer documentSortOrder = 0;

    @PrePersist
    @PreUpdate
    private void calculateTotalAmount() {
        // Calculate totalAmount if not set
        if (this.totalAmount == null) {
            BigDecimal subtotal = unitAmount.multiply(BigDecimal.valueOf(quantity));
            BigDecimal ivaFactor = BigDecimal.ONE.add(ivaPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            this.totalAmount = subtotal.multiply(ivaFactor).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
