package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.forecast.*;
import PSG.backEnd.model.enums.forecast.BudgetForecastStatus;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IBudgetForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST de previsiones de gastos (Feature 17).
 * Expone CRUD, transiciones de estado, gestión de items, aplicación
 * (single/batch/revert/skip), instanciación desde plantilla e import/export.
 */
@RestController
@RequestMapping("/api/v1/budget-forecasts")
@RequiredArgsConstructor
@Tag(name = "Budget Forecasts", description = "Previsiones de gastos (Feature 17).")
public class BudgetForecastController {

    private final IBudgetForecastService service;

    // ─────────────────────── CRUD principal ───────────────────────

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_CREATE + "')")
    @PostMapping
    @Operation(summary = "Crear una previsión de gastos en estado BORRADOR.")
    public ResponseEntity<BudgetForecastResponseDTO> create(
            @Validated(OnCreate.class) @RequestBody BudgetForecastDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_UPDATE + "')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una previsión BORRADOR (cabecera + reemplazo de items).")
    public ResponseEntity<BudgetForecastResponseDTO> update(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody BudgetForecastDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_VIEW + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener una previsión por ID.")
    public ResponseEntity<BudgetForecastResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_VIEW + "')")
    @GetMapping
    @Operation(summary = "Listar previsiones con filtros y paginación.")
    public ResponseEntity<Page<BudgetForecastSummaryDTO>> getAll(
            @RequestParam(required = false) LocalDate periodFromGte,
            @RequestParam(required = false) LocalDate periodToLte,
            @RequestParam(required = false) List<BudgetForecastStatus> statuses,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean hasAppliedItems,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "periodFrom") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        BudgetForecastFilterDTO filter = new BudgetForecastFilterDTO(
                periodFromGte, periodToLte, statuses, search, hasAppliedItems);
        return ResponseEntity.ok(service.getAll(filter, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar (soft-delete) una previsión sin items aplicados.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────── Estado ───────────────────────

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_CONFIRM + "')")
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Pasar de BORRADOR a CONFIRMADA.")
    public ResponseEntity<BudgetForecastResponseDTO> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirm(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_REOPEN + "')")
    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reabrir CONFIRMADA → BORRADOR (sin items aplicados).")
    public ResponseEntity<BudgetForecastResponseDTO> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(service.reopen(id));
    }

    // ─────────────────────── Items ───────────────────────

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_UPDATE + "')")
    @PostMapping("/{id}/items")
    @Operation(summary = "Agregar un item a una previsión BORRADOR.")
    public ResponseEntity<BudgetForecastItemResponseDTO> addItem(
            @PathVariable Long id,
            @Validated(OnCreate.class) @RequestBody BudgetForecastItemDTO itemDTO) {
        return new ResponseEntity<>(service.addItem(id, itemDTO), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_UPDATE + "')")
    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "Actualizar un item existente (solo BORRADOR).")
    public ResponseEntity<BudgetForecastItemResponseDTO> updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Validated(OnUpdate.class) @RequestBody BudgetForecastItemDTO itemDTO) {
        return ResponseEntity.ok(service.updateItem(id, itemId, itemDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_UPDATE + "')")
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Eliminar un item (solo BORRADOR).")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        service.deleteItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────── Aplicación ───────────────────────

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_APPLY + "')")
    @PostMapping("/{id}/items/{itemId}/apply")
    @Operation(summary = "Aplicar un item: crea el registro real (SalaryPayment / ServicePayment / etc).")
    public ResponseEntity<BudgetForecastItemApplyResultDTO> applyItem(
            @PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.ok(service.applyItem(id, itemId));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_APPLY + "')")
    @PostMapping("/{id}/items/batch-apply")
    @Operation(summary = "Aplicar varios items en una sola operación (admite dryRun).")
    public ResponseEntity<BudgetForecastBatchApplyResponseDTO> batchApply(
            @PathVariable Long id,
            @Validated @RequestBody BudgetForecastItemBatchApplyDTO batchDTO) {
        return ResponseEntity.ok(service.applyBatch(id, batchDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_APPLY + "')")
    @PostMapping("/{id}/items/{itemId}/revert")
    @Operation(summary = "Revertir aplicación: borra el registro real y deja el item PENDIENTE.")
    public ResponseEntity<BudgetForecastItemResponseDTO> revertItem(
            @PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.ok(service.revertItem(id, itemId));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_APPLY + "')")
    @PostMapping("/{id}/items/{itemId}/skip")
    @Operation(summary = "Omitir un item indicando un motivo.")
    public ResponseEntity<BudgetForecastItemResponseDTO> skipItem(
            @PathVariable Long id, @PathVariable Long itemId,
            @Validated @RequestBody BudgetForecastItemSkipDTO skipDTO) {
        return ResponseEntity.ok(service.skipItem(id, itemId, skipDTO));
    }

    // ─────────────────────── Instanciar plantilla ───────────────────────

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_CREATE + "')")
    @PostMapping("/instantiate")
    @Operation(summary = "Instanciar una nueva previsión a partir de una plantilla.")
    public ResponseEntity<BudgetForecastResponseDTO> instantiate(
            @Validated @RequestBody BudgetForecastInstantiateDTO dto) {
        return new ResponseEntity<>(service.instantiateFromTemplate(dto), HttpStatus.CREATED);
    }

    // ─────────────────────── Excel / PDF ───────────────────────

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_UPDATE + "')")
    @PostMapping(value = "/{id}/items/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importar items desde Excel (.xlsx). Soporta dryRun para previsualizar.")
    public ResponseEntity<BudgetForecastImportResultDTO> importItems(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(service.importItemsFromExcel(id, file, dryRun));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_VIEW + "')")
    @GetMapping(value = "/{id}/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Exportar la previsión a Excel.")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        byte[] data = service.exportToExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prevision_" + id + ".xlsx\"")
                .body(data);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_VIEW + "')")
    @GetMapping(value = "/{id}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Exportar la previsión a PDF.")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] data = service.exportToPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prevision_" + id + ".pdf\"")
                .body(data);
    }
}
