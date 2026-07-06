package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.notification.NotificationInboxItemDTO;
import PSG.backEnd.model.dto.notification.NotificationRunReportDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.notification.NotificationSchedulerService;
import PSG.backEnd.service.port.INotificationSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestión de suscripciones a notificaciones y bandeja de entrada.")
public class NotificationSubscriptionController {

    private final INotificationSubscriptionService subscriptionService;
    private final NotificationSchedulerService schedulerService;

    // ==================== Suscripciones ====================

    @PostMapping("/subscriptions")
    @Operation(
        summary = "Crear suscripción",
        description = "Crea una suscripción a notificaciones. " +
                      "Para suscribirse uno mismo requiere NOTIFICATION_SELF_SUBSCRIBE; " +
                      "para suscribir a otro usuario requiere NOTIFICATION_ASSIGN_OTHERS. " +
                      "La validación de permisos se realiza en el servicio.")
    @ApiResponse(responseCode = "201", description = "Suscripción creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o suscripción duplicada")
    @ApiResponse(responseCode = "403", description = "Sin permisos suficientes")
    public ResponseEntity<NotificationSubscriptionResponseDTO> createSubscription(
            @Validated(OnCreate.class) @RequestBody NotificationSubscriptionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscription(dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.NOTIFICATION_ASSIGN_OTHERS + "')")
    @GetMapping("/subscriptions")
    @Operation(summary = "Todas las suscripciones", description = "Devuelve todas las suscripciones del tenant. Requiere NOTIFICATION_ASSIGN_OTHERS.")
    public ResponseEntity<List<NotificationSubscriptionResponseDTO>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/subscriptions/me")
    @Operation(summary = "Mis suscripciones", description = "Devuelve todas las suscripciones activas del usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Lista de suscripciones")
    public ResponseEntity<List<NotificationSubscriptionResponseDTO>> getMySubscriptions() {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions());
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.NOTIFICATION_ASSIGN_OTHERS + "')")
    @GetMapping("/subscriptions/user/{userId}")
    @Operation(summary = "Suscripciones de un usuario", description = "Devuelve las suscripciones de otro usuario. Requiere NOTIFICATION_ASSIGN_OTHERS.")
    @ApiResponse(responseCode = "200", description = "Lista de suscripciones del usuario")
    @ApiResponse(responseCode = "403", description = "Sin permiso NOTIFICATION_ASSIGN_OTHERS")
    public ResponseEntity<List<NotificationSubscriptionResponseDTO>> getUserSubscriptions(
            @Parameter(description = "ID del usuario") @PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(userId));
    }

    @GetMapping("/subscriptions/{id}")
    @Operation(summary = "Detalle de suscripción", description = "Devuelve el detalle de una suscripción. Solo el dueño o un usuario con NOTIFICATION_ASSIGN_OTHERS puede acceder.")
    @ApiResponse(responseCode = "200", description = "Detalle de la suscripción")
    @ApiResponse(responseCode = "403", description = "Sin permisos para ver esta suscripción")
    @ApiResponse(responseCode = "404", description = "Suscripción no encontrada")
    public ResponseEntity<NotificationSubscriptionResponseDTO> getSubscriptionById(
            @Parameter(description = "ID de la suscripción") @PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
    }

    @PatchMapping("/subscriptions/{id}")
    @Operation(
        summary = "Actualizar suscripción",
        description = "Actualiza canales, alertas y estado activo de una suscripción. " +
                      "Los campos userId, subjectType y subjectId son inmutables. " +
                      "Solo el dueño o un usuario con NOTIFICATION_ASSIGN_OTHERS puede modificar.")
    @ApiResponse(responseCode = "200", description = "Suscripción actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Sin permisos para modificar esta suscripción")
    @ApiResponse(responseCode = "404", description = "Suscripción no encontrada")
    public ResponseEntity<NotificationSubscriptionResponseDTO> updateSubscription(
            @Parameter(description = "ID de la suscripción") @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody NotificationSubscriptionDTO dto) {
        return ResponseEntity.ok(subscriptionService.updateSubscription(id, dto));
    }

    @DeleteMapping("/subscriptions/{id}")
    @Operation(
        summary = "Eliminar suscripción",
        description = "Elimina lógicamente una suscripción. " +
                      "Solo el dueño o un usuario con NOTIFICATION_ASSIGN_OTHERS puede eliminarla.")
    @ApiResponse(responseCode = "204", description = "Suscripción eliminada")
    @ApiResponse(responseCode = "403", description = "Sin permisos para eliminar esta suscripción")
    @ApiResponse(responseCode = "404", description = "Suscripción no encontrada")
    public ResponseEntity<Void> deleteSubscription(
            @Parameter(description = "ID de la suscripción") @PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Disparo manual (test / operación) ====================

    @PreAuthorize("hasAuthority('" + AppPermissions.NOTIFICATION_ASSIGN_OTHERS + "')")
    @PostMapping("/run-check")
    @Operation(
        summary = "Ejecutar el chequeo de notificaciones on-demand",
        description = "Corre el pipeline de notificaciones para el tenant actual sin esperar al cron diario. " +
                      "Devuelve un reporte con el resultado de cada aviso evaluado (dueDate, triggerDate, ventana, " +
                      "deduplicación, despacho). Requiere NOTIFICATION_ASSIGN_OTHERS.")
    @ApiResponse(responseCode = "200", description = "Reporte de la corrida")
    @ApiResponse(responseCode = "403", description = "Sin permiso NOTIFICATION_ASSIGN_OTHERS")
    public ResponseEntity<NotificationRunReportDTO> runCheck(
            @Parameter(description = "Ignora la deduplicación por ciclo (reenvía aunque ya se haya enviado)")
            @RequestParam(defaultValue = "false") boolean force,
            @Parameter(description = "Evalúa y reporta sin despachar nada")
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(schedulerService.runManual(force, dryRun));
    }

    // ==================== Bandeja de entrada ====================

    @GetMapping("/inbox")
    @Operation(
        summary = "Bandeja de entrada",
        description = "Devuelve las últimas notificaciones SYSTEM recibidas por el usuario autenticado, ordenadas por fecha descendente.")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones recibidas")
    public ResponseEntity<List<NotificationInboxItemDTO>> getInbox(
            @Parameter(description = "Cantidad máxima de notificaciones a devolver (máx. 100)")
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(subscriptionService.getInbox(Math.min(limit, 100)));
    }

    @PatchMapping("/inbox/{logId}/read")
    @Operation(
        summary = "Marcar notificación como leída",
        description = "Marca una notificación del inbox del usuario autenticado como leída.")
    @ApiResponse(responseCode = "204", description = "Notificación marcada como leída")
    @ApiResponse(responseCode = "403", description = "La notificación no pertenece al usuario")
    @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    public ResponseEntity<Void> markInboxItemRead(
            @Parameter(description = "ID del log de notificación") @PathVariable Long logId) {
        subscriptionService.markInboxItemRead(logId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/inbox/{logId}")
    @Operation(
        summary = "Eliminar notificación del inbox",
        description = "Descarta una notificación del inbox del usuario autenticado (soft-dismiss; conserva la auditoría).")
    @ApiResponse(responseCode = "204", description = "Notificación descartada")
    @ApiResponse(responseCode = "403", description = "La notificación no pertenece al usuario")
    @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    public ResponseEntity<Void> dismissInboxItem(
            @Parameter(description = "ID del log de notificación") @PathVariable Long logId) {
        subscriptionService.dismissInboxItem(logId);
        return ResponseEntity.noContent().build();
    }
}
