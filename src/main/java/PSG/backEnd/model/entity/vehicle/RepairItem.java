package PSG.backEnd.model.entity.vehicle;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.enums.vehicle.RepairItemType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "repair_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class RepairItem extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_id", nullable = false)
    private Repair repair;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private RepairItemType itemType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    /** IVA percentage applied to this item (default 21). */
    @Column(name = "iva_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ivaPercentage = new BigDecimal("21.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactional_document_id")
    private TransactionalDocument transactionalDocument;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
