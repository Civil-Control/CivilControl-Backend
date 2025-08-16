package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String cuit;

    @Column(name = "legal_name", nullable = false, unique = true)
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_payment_methods", nullable = false)
    private List<PaymentMethod> allowedPaymentMethods;

    @Embedded
    private Address address;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private ContactInfo contactInfo;

    @Column(name = "pending_balance")
    private BigDecimal pendingBalance;

    @Column(name = "default_discount_percentage")
    private BigDecimal defaultDiscountPercentage;

    @Column
    private String comment;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean deleted;
}
