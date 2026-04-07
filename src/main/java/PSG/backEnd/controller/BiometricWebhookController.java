package PSG.backEnd.controller;

import PSG.backEnd.service.port.IBiometricWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Blind webhook endpoint for receiving biometric clock events from external hardware.
 *
 * <p>Accepts raw JSON payloads to prevent Jackson deserialization failures from
 * breaking the request before we can log the payload for auditing.</p>
 *
 * <p>Always returns 200 OK to the device, regardless of processing outcome,
 * to prevent unnecessary retries from the hardware.</p>
 */
@RestController
@RequestMapping("/api/webhook/biometric")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Biometric Webhook", description = "Receives attendance events from biometric clock hardware")
public class BiometricWebhookController {

    private final IBiometricWebhookService webhookService;

    @PostMapping(value = "/{tenantId}/{brand}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Receive a biometric clock event",
               description = "Blind endpoint — accepts raw JSON, always returns 200 OK to the device. " +
                       "The tenantId identifies which company owns the clock.")
    public ResponseEntity<Void> receive(
            @PathVariable Long tenantId,
            @PathVariable String brand,
            @RequestBody String rawPayload) {

        log.info("Biometric webhook received: tenantId={}, brand={}, payloadLength={}",
                tenantId, brand, rawPayload.length());
        webhookService.process(tenantId, brand, rawPayload);
        return ResponseEntity.ok().build();
    }
}
