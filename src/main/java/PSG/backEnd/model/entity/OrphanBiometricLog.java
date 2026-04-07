package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Dead-letter queue for biometric payloads that could not be parsed.
 * Does NOT extend TenantEntity — we cannot resolve a tenant from malformed data.
 * Preserves the raw payload for manual review and reprocessing.
 */
@Entity
@Table(name = "orphan_biometric_logs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OrphanBiometricLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clock_brand", nullable = false, length = 50)
    private String clockBrand;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
