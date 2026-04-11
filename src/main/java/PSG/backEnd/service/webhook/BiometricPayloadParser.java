package PSG.backEnd.service.webhook;

/**
 * Strategy interface for parsing raw JSON payloads from different biometric clock brands.
 * Each supported brand must provide a concrete implementation.
 *
 * <p>Implementations must be Spring beans whose {@link #supports(String)} method returns true
 * for the brand identifier they handle (case-insensitive).</p>
 */
public interface BiometricPayloadParser {

    /**
     * Whether this parser handles the given brand identifier.
     *
     * @param brand lowercase brand name received in the URL path
     * @return true if this parser can process payloads from the given brand
     */
    boolean supports(String brand);

    /**
     * Extracts the employee identifier and timestamp from a raw JSON payload.
     *
     * @param rawPayload the full JSON body as received from the device
     * @return parsed event containing employee DNI and timestamp,
     *         or {@code null} if the event is a system/non-person event that should be silently skipped
     * @throws BiometricParseException if the payload is malformed or required fields are missing
     */
    BiometricParsedEvent parse(String rawPayload);
}
