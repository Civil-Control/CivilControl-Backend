package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentDTO;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentFilterDTO;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentResponseDTO;
import PSG.backEnd.model.enums.employee.LaborIncidentStatus;
import PSG.backEnd.model.enums.employee.LaborIncidentType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ILaborIncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@RestController
@RequestMapping("/api/v1/labor-incidents")
@RequiredArgsConstructor
@Tag(name = "Labor Incidents", description = "API para el registro de incidentes laborales con impacto económico sobre la empresa.")
public class LaborIncidentController {

    private final ILaborIncidentService iLaborIncidentService;

    @PreAuthorize("hasAuthority('" + AppPermissions.LABOR_INCIDENT_WRITE + "')")
    @PostMapping
    @Operation(summary = "Crear un incidente laboral")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Incidente laboral creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<LaborIncidentResponseDTO> createLaborIncident(
            @Validated(OnCreate.class) @RequestBody LaborIncidentDTO dto) {
        return new ResponseEntity<>(iLaborIncidentService.createLaborIncident(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyAuthority('" + AppPermissions.LABOR_INCIDENT_READ + "','" + AppPermissions.DISCIPLINARY_ACTION_WRITE + "')")
    @GetMapping
    @Operation(summary = "Listar incidentes laborales con filtros y paginación")
    @ApiResponse(responseCode = "200", description = "Lista de incidentes obtenida correctamente")
    public ResponseEntity<Page<LaborIncidentResponseDTO>> getLaborIncidents(
            @Parameter(description = "Filtrar por ID de empleado") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Búsqueda parcial en nombre/apellido de empleados") @RequestParam(required = false) String employeeSearch,
            @Parameter(description = "Filtrar por tipo de incidente") @RequestParam(required = false) LaborIncidentType incidentType,
            @Parameter(description = "Filtrar por estado") @RequestParam(required = false) LaborIncidentStatus status,
            @Parameter(description = "Fecha de incidente desde") @RequestParam(required = false) LocalDate incidentDateFrom,
            @Parameter(description = "Fecha de incidente hasta") @RequestParam(required = false) LocalDate incidentDateTo,
            @Parameter(description = "true = solo con impacto económico, false = solo sin impacto, null = todos") @RequestParam(required = false) Boolean hasFinancialImpact,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "incidentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mapSortField(sortBy));
        Pageable pageable = PageRequest.of(page, size, sort);

        LaborIncidentFilterDTO filterDTO = new LaborIncidentFilterDTO(
                employeeId, employeeSearch, incidentType, status,
                incidentDateFrom, incidentDateTo, hasFinancialImpact
        );

        return ResponseEntity.ok(iLaborIncidentService.getAllLaborIncidents(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LABOR_INCIDENT_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener un incidente laboral por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incidente encontrado"),
            @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    public ResponseEntity<LaborIncidentResponseDTO> getLaborIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(iLaborIncidentService.getLaborIncidentById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LABOR_INCIDENT_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar un incidente laboral")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incidente actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Incidente o empleado no encontrado")
    })
    public ResponseEntity<LaborIncidentResponseDTO> updateLaborIncident(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody LaborIncidentDTO dto) {
        return ResponseEntity.ok(iLaborIncidentService.updateLaborIncident(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.LABOR_INCIDENT_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un incidente laboral")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Incidente eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    public ResponseEntity<Void> deleteLaborIncident(@PathVariable Long id) {
        iLaborIncidentService.deleteLaborIncident(id);
        return ResponseEntity.noContent().build();
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "incidentType"    -> "incidentType";
            case "status"          -> "status";
            case "financialImpact" -> "financialImpact";
            default                -> "incidentDate";
        };
    }
}
