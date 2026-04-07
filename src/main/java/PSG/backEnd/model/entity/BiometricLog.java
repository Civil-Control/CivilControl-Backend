package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores a successfully parsed biometric clock event.
 * Multi-tenant: each record is scoped to the tenant that owns the employee.
 * Idempotency enforced via UNIQUE(tenant_id, employee_dni, timestamp).
 */
@Entity
@Table(name = "biometric_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "employee_dni", "timestamp"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BiometricLog extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_dni", nullable = false, length = 20)
    private String employeeDni;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "clock_brand", nullable = false, length = 50)
    private String clockBrand;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
