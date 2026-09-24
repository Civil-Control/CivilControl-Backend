package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.recovery.RecoveryEventFilterDTO;
import PSG.backEnd.model.dto.recovery.RecoveryEventResponseDTO;
import PSG.backEnd.model.dto.recovery.RecoverySupplierConfigDTO;
import PSG.backEnd.model.dto.recovery.RecoverySupplierConfigResponseDTO;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IRecoveryService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST surface for the hidden Value Recovery feature (Feature 18).
 *
 * <p>The endpoints are intentionally only useful when a "Recupero" project area exists
 * (the magic-name auto-flag). The frontend should hide the entire module otherwise,
 * but the backend still enforces authorization via {@link AppPermissions#RECOVERY_VIEW}
 * / {@link AppPermissions#RECOVERY_MANAGE} so direct API access is also gated.
 */
@RestController
@RequestMapping("/api/v1/recovery")
@Tag(name = "Value Recovery", description = "Configuración y eventos de recupero de valor (oculto)")
@RequiredArgsConstructor
public class RecoveryController {

    private final IRecoveryService recoveryService;

    // ── Active sector probe ─────────────────────────────────────────────────

    /**
     * Returns {@code {"projectAreaId": <id>}} when a recovery sector exists for the tenant,
     * or {@code 204 No Content} otherwise. Used by the frontend to decide whether to render
     * the hidden module entry point.
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_VIEW + "')")
    public ResponseEntity<Map<String, Long>> getActiveRecoverySector() {
        return recoveryService.findActiveRecoverySectorId()
                .<ResponseEntity<Map<String, Long>>>map(id -> ResponseEntity.ok(Map.of("projectAreaId", id)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ── Supplier configs ────────────────────────────────────────────────────

    @GetMapping("/sectors/{projectAreaId}/supplier-configs")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_VIEW + "')")
    public List<RecoverySupplierConfigResponseDTO> listConfigs(@PathVariable Long projectAreaId) {
        return recoveryService.listConfigsForArea(projectAreaId);
    }

    @PostMapping("/sectors/{projectAreaId}/supplier-configs")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_MANAGE + "')")
    public ResponseEntity<RecoverySupplierConfigResponseDTO> createConfig(
            @PathVariable Long projectAreaId,
            @Validated(OnCreate.class) @RequestBody RecoverySupplierConfigDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recoveryService.createConfig(projectAreaId, dto));
    }

    @PutMapping("/supplier-configs/{configId}")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_MANAGE + "')")
    public RecoverySupplierConfigResponseDTO updateConfig(
            @PathVariable Long configId,
            @Validated(OnUpdate.class) @RequestBody RecoverySupplierConfigDTO dto) {
        return recoveryService.updateConfig(configId, dto);
    }

    @DeleteMapping("/supplier-configs/{configId}")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_MANAGE + "')")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long configId) {
        recoveryService.deleteConfig(configId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/supplier-configs/{configId}/events")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_VIEW + "')")
    public List<RecoveryEventResponseDTO> listEventsForConfig(@PathVariable Long configId) {
        return recoveryService.listEventsForConfig(configId);
    }

    // ── Sector-wide ledger ("Todos los movimientos") ────────────────────────

    /**
     * Full recovery ledger for the sector — every supplier's events, not just one — with
     * optional filters. Powers the "Todos los movimientos" view, as opposed to
     * {@link #listEventsForConfig}, which is scoped to a single supplier config.
     */
    @GetMapping("/sectors/{projectAreaId}/events")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_VIEW + "')")
    public Page<RecoveryEventResponseDTO> listAllEvents(
            @PathVariable Long projectAreaId,
            @Parameter(description = "Filter movements from this date (inclusive)") @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "Filter movements to this date (inclusive)") @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "Filter by destination cash box") @RequestParam(required = false) Long cashBoxId,
            @Parameter(description = "Filter by supplier") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by event type (GENERATED, REVERSED, ADJUSTED_CREDIT_NOTE)") @RequestParam(required = false) RecoveryEventType eventType,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by: movementDate, occurredAt, recoveredAmount") @RequestParam(defaultValue = "movementDate") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mapEventSortField(sortBy));
        Pageable pageable = PageRequest.of(page, size, sort);
        RecoveryEventFilterDTO filters = new RecoveryEventFilterDTO(fromDate, toDate, cashBoxId, supplierId, eventType);
        return recoveryService.listAllEvents(projectAreaId, filters, pageable);
    }

    private String mapEventSortField(String sortBy) {
        return switch (sortBy) {
            case "movementDate" -> "cashBoxMovement.movementDate";
            case "recoveredAmount" -> "recoveredAmount";
            default -> "occurredAt";
        };
    }

    // ── Document timeline ───────────────────────────────────────────────────

    @GetMapping("/documents/{documentId}/events")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_VIEW + "')")
    public List<RecoveryEventResponseDTO> listEventsForDocument(@PathVariable Long documentId) {
        return recoveryService.listEventsForDocument(documentId);
    }

    // ── One-time data cleanup ────────────────────────────────────────────────

    /**
     * Reverses every duplicate active GENERATED event left over from before
     * {@code RecoveryService.generateForDocument} became idempotent. Keeps the most recent
     * active event per affected document, reverses the rest (cash box balance corrected too).
     * Idempotent — safe to call more than once; a no-op once there's nothing left to fix.
     */
    @PostMapping("/admin/dedupe-generated-events")
    @PreAuthorize("hasAuthority('" + AppPermissions.RECOVERY_MANAGE + "')")
    public ResponseEntity<Map<String, Object>> dedupeGeneratedEvents() {
        List<Long> fixedDocumentIds = recoveryService.dedupeDuplicateGeneratedEvents();
        return ResponseEntity.ok(Map.of(
                "fixedDocumentCount", fixedDocumentIds.size(),
                "documentIds", fixedDocumentIds));
    }
}
