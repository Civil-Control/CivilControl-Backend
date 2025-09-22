package PSG.backEnd.model.entity.insurance;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "auto_policies")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AutoPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_policy_id", nullable = false)
    private InsurancePolicy insurancePolicy;

    @OneToMany(mappedBy = "autoPolicy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PolicyVehicle> policyVehicles;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
