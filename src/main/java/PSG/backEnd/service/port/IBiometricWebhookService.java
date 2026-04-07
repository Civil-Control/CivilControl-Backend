package PSG.backEnd.service.port;

/**
 * Port for processing raw biometric webhook payloads.
 */
public interface IBiometricWebhookService {

    /**
     * Processes a raw payload received from a biometric clock.
     * Resolves the brand-specific parser, extracts data, looks up the employee
     * within the given tenant, and persists the record. Duplicates are silently ignored.
     * Malformed payloads are routed to the dead-letter queue.
     *
     * @param tenantId   the tenant (company) that owns the biometric clock
     * @param brand      the clock brand identifier (e.g. "hikvision")
     * @param rawPayload the raw JSON body from the device
     */
    void process(Long tenantId, String brand, String rawPayload);
}
