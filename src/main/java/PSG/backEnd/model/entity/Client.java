package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.IvaCondition;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "clients",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "cuit"})
    }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Client extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "VARCHAR(100)")
    private String cuit;

    @Column(name = "business_name", nullable = false, columnDefinition = "VARCHAR(200)")
    private String businessName;

    @Column(name = "trade_name", columnDefinition = "VARCHAR(200)")
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "iva_condition", nullable = false, columnDefinition = "VARCHAR(50)")
    private IvaCondition ivaCondition;

    @Embedded
    private Address address;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "contact_info_id")
    private ContactInfo contactInfo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
