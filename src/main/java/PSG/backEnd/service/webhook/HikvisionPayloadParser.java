package PSG.backEnd.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Parser for Hikvision biometric clock payloads (HTTP Listening protocol).
 *
 * <p>Expected JSON structure (real Hikvision ISAPI event push):</p>
 * <pre>
 * {
 *   "dateTime": "2024-03-04T12:48:43-03:00",
 *   "eventType": "AccessControllerEvent",
 *   "AccessControllerEvent": {
 *     "employeeNoString": "1",
 *     "name": "Caceres",
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p>Note: {@code employeeNoString} may be absent on system/non-person events;
 * in that case this parser returns {@code null} to signal "skip".</p>
 */
@Component
@Slf4j
public class HikvisionPayloadParser implements BiometricPayloadParser {

    private static final String BRAND = "hikvision";
    private final ObjectMapper objectMapper;

    public HikvisionPayloadParser() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(String brand) {
        return BRAND.equalsIgnoreCase(brand);
    }

    @Override
    public BiometricParsedEvent parse(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode event = root.path("AccessControllerEvent");

            if (event.isMissingNode()) {
                throw new BiometricParseException("Missing 'AccessControllerEvent' node in Hikvision payload");
            }

            // Extract employee DNI — absent on system events, return null to signal "skip"
            JsonNode employeeNode = event.path("employeeNoString");
            if (employeeNode.isMissingNode() || employeeNode.asText().isBlank()) {
                log.debug("Hikvision system event (no employeeNoString) — skipping");
                return null;
            }
            String employeeDni = employeeNode.asText().trim();

            // Extract timestamp from root-level "dateTime" (ISO 8601 with offset)
            JsonNode dateTimeNode = root.path("dateTime");
            if (dateTimeNode.isMissingNode() || dateTimeNode.asText().isBlank()) {
                throw new BiometricParseException("Missing or empty 'dateTime' in Hikvision payload");
            }

            LocalDateTime timestamp;
            try {
                OffsetDateTime odt = OffsetDateTime.parse(dateTimeNode.asText().trim());
                timestamp = odt.toLocalDateTime();
            } catch (DateTimeParseException e) {
                throw new BiometricParseException(
                    "Invalid 'dateTime' format in Hikvision payload: " + dateTimeNode.asText(), e);
            }

            return new BiometricParsedEvent(employeeDni, timestamp);

        } catch (BiometricParseException e) {
            throw e;
        } catch (Exception e) {
            throw new BiometricParseException("Failed to parse Hikvision payload: " + e.getMessage(), e);
        }
    }
}
