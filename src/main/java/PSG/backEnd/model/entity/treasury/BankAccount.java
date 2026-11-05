package PSG.backEnd.model.entity.treasury;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.treasury.BankAccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bank_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uq_bank_accounts_tenant_account_bank",
                columnNames = {"tenant_id", "account_number", "bank_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    @Builder.Default
    private BankAccountType accountType = BankAccountType.CUENTA_CORRIENTE;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(length = 22)
    private String cbu;

    @Column(length = 30)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    @Builder.Default
    private Currency currency = Currency.ARS;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BankAccountMovement> movements = new ArrayList<>();
}
