package PSG.backEnd.service.webhook;

import java.time.LocalDateTime;

/**
 * Result of parsing a biometric clock payload.
 * Contains the extracted employee identifier and event timestamp.
 */
public record BiometricParsedEvent(
    String employeeDni,
    LocalDateTime timestamp
) {}
