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
 * <p>Expected JSON structure:</p>
 * <pre>
 * {
 *   "AccessControllerEvent": {
 *     "employeeNoString": "12345678",
 *     "time": "2026-04-07T08:30:00-03:00"
 *   }
 * }
 * </pre>
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

            // Extract employee DNI
            JsonNode employeeNode = event.path("employeeNoString");
            if (employeeNode.isMissingNode() || employeeNode.asText().isBlank()) {
                throw new BiometricParseException("Missing or empty 'employeeNoString' in Hikvision payload");
            }
            String employeeDni = employeeNode.asText().trim();

            // Extract timestamp (ISO with timezone, e.g. "2026-04-07T08:30:00-03:00")
            JsonNode timeNode = event.path("time");
            if (timeNode.isMissingNode() || timeNode.asText().isBlank()) {
                throw new BiometricParseException("Missing or empty 'time' in Hikvision payload");
            }

            LocalDateTime timestamp;
            try {
                OffsetDateTime odt = OffsetDateTime.parse(timeNode.asText().trim());
                timestamp = odt.toLocalDateTime();
            } catch (DateTimeParseException e) {
                throw new BiometricParseException(
                    "Invalid 'time' format in Hikvision payload: " + timeNode.asText(), e);
            }

            return new BiometricParsedEvent(employeeDni, timestamp);

        } catch (BiometricParseException e) {
            throw e;
        } catch (Exception e) {
            throw new BiometricParseException("Failed to parse Hikvision payload: " + e.getMessage(), e);
        }
    }
}
