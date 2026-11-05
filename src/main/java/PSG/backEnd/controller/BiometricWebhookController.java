package PSG.backEnd.controller;

import PSG.backEnd.service.port.IBiometricWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @RequestMapping(
            value = "/{webhookToken}/{brand}",
            method = {RequestMethod.POST, RequestMethod.PUT}
    )
    @Operation(summary = "Receive a biometric clock event",
               description = "Blind endpoint — accepts raw JSON or multipart forms, always returns 200 OK to the device. " +
                       "The webhookToken identifies which company owns the clock.")
    public ResponseEntity<Void> receive(
            @PathVariable String webhookToken,
            @PathVariable String brand,
            @RequestParam(value = "event_log", required = false) String eventLogJson,
            @RequestBody(required = false) String bodyJson) {

        // Extraemos el JSON venga como formulario (Hikvision) o como body crudo (ZKTeco/otros)
        String rawPayload = (eventLogJson != null && !eventLogJson.isEmpty()) ? eventLogJson : bodyJson;

        // Fallback de seguridad por si el reloj manda algo completamente vacío
        if (rawPayload == null) {
            rawPayload = "";
            log.warn("Se recibió un webhook vacío para token: {} y marca: {}", webhookToken, brand);
        }

        log.info("Biometric webhook received: token={}, brand={}, payloadLength={}",
                webhookToken, brand, rawPayload.length());
        
        // Log extra temporal para ver el JSON crudo en la consola y confirmar que llegó
        log.info("Contenido del payload extraído: {}", rawPayload);

        webhookService.process(webhookToken, brand, rawPayload);
        
        return ResponseEntity.ok().build();
    }
}
