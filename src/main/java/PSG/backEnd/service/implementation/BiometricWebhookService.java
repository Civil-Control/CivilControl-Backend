package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.BiometricLog;
import PSG.backEnd.model.entity.OrphanBiometricLog;
import PSG.backEnd.repository.BiometricLogRepository;
import PSG.backEnd.repository.OrphanBiometricLogRepository;
import PSG.backEnd.service.port.IBiometricWebhookService;
import PSG.backEnd.service.webhook.BiometricParseException;
import PSG.backEnd.service.webhook.BiometricParsedEvent;
import PSG.backEnd.service.webhook.BiometricPayloadParser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates the processing of raw biometric webhook payloads.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Resolve the brand-specific {@link BiometricPayloadParser}</li>
 *   <li>Parse the raw JSON → extract employee DNI and timestamp</li>
 *   <li>Resolve the tenant by looking up the employee across all tenants (native query)</li>
 *   <li>Persist the {@link BiometricLog} (idempotent — duplicates silently ignored)</li>
 *   <li>On parse failure or missing employee → route to {@link OrphanBiometricLog} dead-letter queue</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricWebhookService implements IBiometricWebhookService {

    private final List<BiometricPayloadParser> parsers;
    private final BiometricLogRepository biometricLogRepository;
    private final OrphanBiometricLogRepository orphanBiometricLogRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void process(String brand, String rawPayload) {
        String normalizedBrand = brand.toLowerCase().trim();

        // 1. Resolve parser
        BiometricPayloadParser parser = parsers.stream()
                .filter(p -> p.supports(normalizedBrand))
                .findFirst()
                .orElse(null);

        if (parser == null) {
            saveOrphan(normalizedBrand, rawPayload, "Unsupported clock brand: " + normalizedBrand);
            return;
        }

        // 2. Parse payload
        BiometricParsedEvent event;
        try {
            event = parser.parse(rawPayload);
        } catch (BiometricParseException e) {
            log.warn("Failed to parse {} payload: {}", normalizedBrand, e.getMessage());
            saveOrphan(normalizedBrand, rawPayload, e.getMessage());
            return;
        }

        // 3. Validate extracted data
        if (event.employeeDni() == null || event.employeeDni().isBlank()) {
            saveOrphan(normalizedBrand, rawPayload, "Parsed employee DNI is null or blank");
            return;
        }
        if (event.timestamp() == null) {
            saveOrphan(normalizedBrand, rawPayload, "Parsed timestamp is null");
            return;
        }

        // 4. Resolve tenant via cross-tenant employee lookup (bypasses Hibernate tenant filter)
        Long tenantId = resolveEmployeeTenant(event.employeeDni());
        if (tenantId == null) {
            log.warn("No active employee found with DNI {} — routing to orphan queue", event.employeeDni());
            saveOrphan(normalizedBrand, rawPayload,
                    "No active employee found with DNI: " + event.employeeDni());
            return;
        }

        // 5. Persist biometric log (idempotent)
        BiometricLog biometricLog = BiometricLog.builder()
                .employeeDni(event.employeeDni())
                .timestamp(event.timestamp())
                .clockBrand(normalizedBrand)
                .rawPayload(rawPayload)
                .receivedAt(LocalDateTime.now())
                .build();
        biometricLog.setTenantId(tenantId);

        try {
            biometricLogRepository.save(biometricLog);
            log.info("Biometric log saved: DNI={}, timestamp={}, brand={}",
                    event.employeeDni(), event.timestamp(), normalizedBrand);
        } catch (DataIntegrityViolationException e) {
            // Duplicate record (same tenant + DNI + timestamp) — silently ignore
            log.debug("Duplicate biometric event ignored: DNI={}, timestamp={}",
                    event.employeeDni(), event.timestamp());
        }
    }

    /**
     * Looks up an employee's tenant_id by DNI across all tenants using a native query.
     * Bypasses the Hibernate tenant filter to enable cross-tenant resolution.
     *
     * @return the tenant_id if an active employee with the given DNI exists, null otherwise
     */
    private Long resolveEmployeeTenant(String dni) {
        @SuppressWarnings("unchecked")
        List<Long> results = entityManager
                .createNativeQuery("SELECT tenant_id FROM employees WHERE dni = :dni AND deleted = false LIMIT 1")
                .setParameter("dni", dni)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private void saveOrphan(String brand, String rawPayload, String errorMessage) {
        try {
            OrphanBiometricLog orphan = OrphanBiometricLog.builder()
                    .clockBrand(brand)
                    .rawPayload(rawPayload)
                    .errorMessage(errorMessage)
                    .receivedAt(LocalDateTime.now())
                    .build();
            orphanBiometricLogRepository.save(orphan);
            log.info("Orphan biometric payload saved: brand={}, error={}", brand, errorMessage);
        } catch (Exception e) {
            log.error("CRITICAL — Failed to save orphan biometric payload: {}", e.getMessage(), e);
        }
    }
}
