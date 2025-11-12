package PSG.backEnd.model.entity.employee;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "epp_deliveries")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EppDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "item_type", nullable = false, length = 100)
    private String itemType;

    @Column(length = 100)
    private String brand;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}

