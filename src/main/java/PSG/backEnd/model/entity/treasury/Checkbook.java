package PSG.backEnd.model.entity.treasury;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.treasury.CheckType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checkbooks",
        uniqueConstraints = @UniqueConstraint(name = "uq_checkbooks_tenant_number_account",
                columnNames = {"tenant_id", "checkbook_number", "bank_account_id"}),
        indexes = {
                @Index(name = "idx_checkbooks_account", columnList = "bank_account_id, active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkbook extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "checkbook_number", nullable = false, length = 50)
    private String checkbookNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 15)
    private CheckType checkType;

    @Column(name = "range_from", nullable = false)
    private Long rangeFrom;

    @Column(name = "range_to", nullable = false)
    private Long rangeTo;

    /** False once every number in the range has been consumed. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
