package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_purchases")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class StockPurchase extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String notes;

    @Column(name = "transactional_document_id")
    private Long transactionalDocumentId;

    @Column(name = "document_sort_order", nullable = false)
    @Builder.Default
    private Integer documentSortOrder = 0;
}
