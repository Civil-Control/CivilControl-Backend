package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IBudgetForecastTemplateService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/budget-forecast-templates")
@RequiredArgsConstructor
@Tag(name = "Budget Forecast Templates", description = "Plantillas reutilizables de previsiones (Feature 17).")
public class BudgetForecastTemplateController {

    private final IBudgetForecastTemplateService service;

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_TEMPLATE_CREATE + "')")
    @PostMapping
    @Operation(summary = "Crear una plantilla de previsión.")
    public ResponseEntity<BudgetForecastTemplateResponseDTO> create(
            @Validated(OnCreate.class) @RequestBody BudgetForecastTemplateDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_TEMPLATE_UPDATE + "')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una plantilla.")
    public ResponseEntity<BudgetForecastTemplateResponseDTO> update(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody BudgetForecastTemplateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_TEMPLATE_VIEW + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener una plantilla por ID.")
    public ResponseEntity<BudgetForecastTemplateResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_TEMPLATE_VIEW + "')")
    @GetMapping
    @Operation(summary = "Listar plantillas con filtros.")
    public ResponseEntity<Page<BudgetForecastTemplateResponseDTO>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(service.getAll(active, search, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.BUDGET_FORECAST_TEMPLATE_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar (soft-delete) una plantilla.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
