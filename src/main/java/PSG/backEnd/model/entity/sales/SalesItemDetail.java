package PSG.backEnd.model.entity.sales;

import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "sales_item_details")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SalesItemDetail extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_document_id", nullable = false)
    private SalesDocument salesDocument;

    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "iva_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPercentage;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @PrePersist
    @PreUpdate
    private void calculateTotalAmount() {
        if (this.totalAmount == null) {
            BigDecimal subtotal = unitAmount.multiply(BigDecimal.valueOf(quantity));
            BigDecimal ivaFactor = BigDecimal.ONE.add(
                ivaPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            this.totalAmount = subtotal.multiply(ivaFactor).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
