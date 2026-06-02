package PSG.backEnd.controller;

import PSG.backEnd.model.dto.notification.CustomReminderDTO;
import PSG.backEnd.model.dto.notification.CustomReminderResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ICustomReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "Custom Reminders", description = "Gestión de recordatorios personalizados.")
public class CustomReminderController {

    private final ICustomReminderService reminderService;

    @PostMapping
    @Operation(summary = "Crear recordatorio", description = "Crea un recordatorio personalizado con su suscripción de notificación.")
    public ResponseEntity<CustomReminderResponseDTO> createReminder(
            @Validated(OnCreate.class) @RequestBody CustomReminderDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reminderService.createReminder(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "Mis recordatorios", description = "Devuelve los recordatorios del usuario autenticado.")
    public ResponseEntity<List<CustomReminderResponseDTO>> getMyReminders() {
        return ResponseEntity.ok(reminderService.getMyReminders());
    }

    @GetMapping
    @Operation(summary = "Todos los recordatorios", description = "Devuelve todos los recordatorios del tenant. Requiere NOTIFICATION_ASSIGN_OTHERS.")
    public ResponseEntity<List<CustomReminderResponseDTO>> getAllReminders() {
        return ResponseEntity.ok(reminderService.getAllReminders());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Recordatorios de un usuario", description = "Devuelve los recordatorios de otro usuario. Requiere NOTIFICATION_ASSIGN_OTHERS.")
    public ResponseEntity<List<CustomReminderResponseDTO>> getUserReminders(@PathVariable Long userId) {
        return ResponseEntity.ok(reminderService.getUserReminders(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de recordatorio")
    public ResponseEntity<CustomReminderResponseDTO> getReminderById(@PathVariable Long id) {
        return ResponseEntity.ok(reminderService.getReminderById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar recordatorio")
    public ResponseEntity<CustomReminderResponseDTO> updateReminder(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody CustomReminderDTO dto) {
        return ResponseEntity.ok(reminderService.updateReminder(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar recordatorio", description = "Elimina lógicamente el recordatorio y su suscripción asociada.")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id) {
        reminderService.deleteReminder(id);
        return ResponseEntity.noContent().build();
    }
}
