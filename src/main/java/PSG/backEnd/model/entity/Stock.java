package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.StockCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stocks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Stock extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;


    @Enumerated(EnumType.STRING)
    @Column(name = "stock_category", nullable = false)
    private StockCategory stockCategory;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

}
