package PSG.backEnd.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;
}
