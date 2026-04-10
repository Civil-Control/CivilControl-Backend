package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.IvaCondition;
import PSG.backEnd.model.enums.documents.PaymentMethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "cuit"}),
    @UniqueConstraint(columnNames = {"tenant_id", "legal_name"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Supplier extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String cuit;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "alias", length = 50)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(name = "iva_condition", length = 30)
    private IvaCondition ivaCondition;

    @Builder.Default
    @ElementCollection(targetClass = PaymentMethod.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "supplier_allowed_payment_methods",
            joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "payment_method", nullable = false)
    private List<PaymentMethod> allowedPaymentMethods = new ArrayList<>();

    @Embedded
    private Address address;

    @Builder.Default
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContactInfo> contacts = new ArrayList<>();

    @Column(name = "pending_balance")
    private BigDecimal pendingBalance;

    @Column(name = "default_discount_percentage")
    private BigDecimal defaultDiscountPercentage;

    @Column(columnDefinition = "VARCHAR(500)")
    private String comment;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean deleted;
}
