# Feature 20 — Incidentes Laborales (Labor Incidents)

## Resumen

Sistema de **registro de incidentes operativos** que generan costos, multas o sanciones sobre la empresa como consecuencia directa de la actuación de uno o más empleados. Cubre casos que el modelo actual de `DisciplinaryAction` no puede capturar: accidentes vehiculares, multas de tránsito, roturas de redes de servicio (gas, agua, telefonía), daño a material de trabajo, e infracciones normativas.

### Separación conceptual con `DisciplinaryAction`

| | `DisciplinaryAction` | `LaborIncident` |
|---|---|---|
| ¿Qué registra? | La sanción formal de RRHH al empleado | El evento operativo que ocurrió |
| ¿Empleados involucrados? | Siempre uno (el sancionado) | Uno o más |
| ¿Tiene impacto económico? | No | Sí (opcional) |
| ¿Tiene estado de resolución? | No aplica | Sí (Pendiente / En investigación / Resuelto) |
| ¿Siempre lleva sanción? | Sí (es la sanción en sí) | No necesariamente |

La relación entre ambas entidades es opcional y unidireccional:

```
LaborIncident (1) ←——— (0..N) DisciplinaryAction
```

Un incidente puede motivar cero o más acciones disciplinarias. Una acción disciplinaria puede o no estar vinculada a un incidente registrado.

**Caso de uso real:** Un empleado maneja un camión y choca un muro de un tercero. La empresa paga $1.500.000. El incidente se registra con tipo `ACCIDENTE_VEHICULAR`, impacto económico $1.500.000 y los empleados involucrados. Días después, RRHH decide emitir una amonestación, que se registra en `DisciplinaryAction` linkeada al incidente.

---

## 1. Modelo de Datos

### 1.1 Nueva entidad: `LaborIncident`

**Ubicación:** `model/entity/employee/LaborIncident.java`

```java
@Entity
@Table(name = "labor_incidents")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LaborIncident extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false)
    private LaborIncidentType incidentType;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "description", nullable = false, length = 1000, columnDefinition = "VARCHAR(1000)")
    private String description;

    @Column(name = "financial_impact", precision = 15, scale = 2)
    private BigDecimal financialImpact;                  // costo para la empresa, nullable

    @Column(name = "affected_asset", length = 500, columnDefinition = "VARCHAR(500)")
    private String affectedAsset;                        // bien involucrado: "Camión F-350 patente AB123"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LaborIncidentStatus status;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;                      // solo cuando status == RESUELTO

    @Column(name = "notes", length = 1000, columnDefinition = "VARCHAR(1000)")
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "labor_incident_employees",
        joinColumns = @JoinColumn(name = "labor_incident_id"),
        inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();
}
```

### 1.2 Modificación a `DisciplinaryAction`

Agregar un único campo nuevo al final de la clase:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "labor_incident_id")
private LaborIncident laborIncident;                     // vínculo opcional al incidente que originó esta acción
```

### 1.3 Enum: `LaborIncidentType`

**Ubicación:** `model/enums/employee/LaborIncidentType.java`

```java
public enum LaborIncidentType {
    ACCIDENTE_VEHICULAR ("Accidente vehicular"),
    MULTA_TRANSITO      ("Multa de tránsito"),
    DANO_INFRAESTRUCTURA("Daño a infraestructura"),
    DANO_REDES_SERVICIO ("Daño a redes de servicio"),   // gas, agua, telefonía, electricidad
    DANO_MATERIAL       ("Daño a material o equipo"),
    INFRACCION_NORMATIVA("Infracción normativa"),
    OTRO                ("Otro");

    private final String displayName;

    LaborIncidentType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
```

### 1.4 Enum: `LaborIncidentStatus`

**Ubicación:** `model/enums/employee/LaborIncidentStatus.java`

```java
public enum LaborIncidentStatus {
    PENDIENTE        ("Pendiente"),
    EN_INVESTIGACION ("En investigación"),
    RESUELTO         ("Resuelto");

    private final String displayName;

    LaborIncidentStatus(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
```

---

## 2. Migración SQL

```sql
-- Tabla principal de incidentes laborales
CREATE TABLE labor_incidents (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT         NOT NULL,
    incident_type    VARCHAR(50)    NOT NULL,
    incident_date    DATE           NOT NULL,
    description      VARCHAR(1000)  NOT NULL,
    financial_impact DECIMAL(15, 2),
    affected_asset   VARCHAR(500),
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDIENTE',
    resolved_date    DATE,
    notes            VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_labor_incidents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_labor_incidents_tenant_date ON labor_incidents(tenant_id, incident_date);
CREATE INDEX idx_labor_incidents_status      ON labor_incidents(tenant_id, status);

-- Tabla de unión incidente-empleados (N a N)
-- ON DELETE CASCADE: al borrar el incidente, las filas de la unión no tienen sentido
CREATE TABLE labor_incident_employees (
    labor_incident_id BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL,
    PRIMARY KEY (labor_incident_id, employee_id),
    CONSTRAINT fk_lie_incident FOREIGN KEY (labor_incident_id) REFERENCES labor_incidents(id) ON DELETE CASCADE,
    CONSTRAINT fk_lie_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- FK en tabla existente
-- ON DELETE SET NULL: preserva el historial disciplinario aunque se borre el incidente
ALTER TABLE disciplinary_actions
    ADD COLUMN labor_incident_id BIGINT,
    ADD CONSTRAINT fk_da_labor_incident
        FOREIGN KEY (labor_incident_id) REFERENCES labor_incidents(id) ON DELETE SET NULL;
```

---

## 3. DTOs

### 3.1 `LaborIncidentDTO` (request — create / update)

```java
public record LaborIncidentDTO(
    @Schema(description = "IDs de los empleados involucrados en el incidente. Mínimo uno requerido.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @NotEmpty(message = "{validation.notEmpty}", groups = OnCreate.class)
    List<Long> employeeIds,

    @Schema(description = "Categoría del incidente.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LaborIncidentType incidentType,

    @Schema(description = "Fecha en que ocurrió el incidente. No puede ser futura.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @PastOrPresent(message = "{laborIncident.incidentDate.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
    LocalDate incidentDate,

    @Schema(description = "Descripción detallada del evento. Mínimo 10 caracteres.", minLength = 10, maxLength = 1000)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 10, max = 1000, message = "{laborIncident.description.size}", groups = {OnCreate.class, OnUpdate.class})
    String description,

    @Schema(description = "Costo económico para la empresa. Nulo si no aplica.", nullable = true)
    @DecimalMin(value = "0.00", message = "{laborIncident.financialImpact.negative}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal financialImpact,

    @Schema(description = "Bien involucrado (ej: Camión F-350 patente AB123).", nullable = true)
    @Size(max = 500, message = "{laborIncident.affectedAsset.size}", groups = {OnCreate.class, OnUpdate.class})
    String affectedAsset,

    @Schema(description = "Estado actual del incidente.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LaborIncidentStatus status,

    @Schema(description = "Fecha de resolución. Solo válida cuando status es RESUELTO.", nullable = true)
    LocalDate resolvedDate,

    @Schema(description = "Observaciones adicionales.", nullable = true)
    @Size(max = 1000, message = "{laborIncident.notes.size}", groups = {OnCreate.class, OnUpdate.class})
    String notes
) {}
```

### 3.2 `LaborIncidentFilterDTO` (filtros para el listado)

```java
public record LaborIncidentFilterDTO(
    Long employeeId,             // incidentes que involucran a este empleado
    String employeeSearch,       // búsqueda parcial en nombre/apellido de empleados
    LaborIncidentType incidentType,
    LaborIncidentStatus status,
    LocalDate incidentDateFrom,
    LocalDate incidentDateTo,
    Boolean hasFinancialImpact   // true=solo con importe, false=solo sin importe, null=todos
) {}
```

### 3.3 `LaborIncidentResponseDTO` (respuesta)

```java
public record LaborIncidentResponseDTO(
    Long id,
    LaborIncidentType incidentType,
    LocalDate incidentDate,
    String description,
    BigDecimal financialImpact,
    String affectedAsset,
    LaborIncidentStatus status,
    LocalDate resolvedDate,
    String notes,
    List<EmployeeRef> employees
) {
    public record EmployeeRef(Long id, String name, String lastName) {}
}
```

### 3.4 Modificaciones a DTOs existentes de `DisciplinaryAction`

**`DisciplinaryActionDTO`** — agregar al final del record:
```java
@Schema(description = "ID del incidente laboral que originó esta acción disciplinaria. Opcional.", nullable = true)
Long laborIncidentId
```

**`DisciplinaryActionResponseDTO`** — agregar tres campos al record:
```java
Long laborIncidentId,
LaborIncidentType laborIncidentType,
LocalDate laborIncidentDate
```
Estos tres campos permiten al frontend mostrar un chip del incidente vinculado en el detalle sin realizar un request adicional.

---

## 4. Mapper

### 4.1 `LaborIncidentMapper`

**Ubicación:** `model/mapper/LaborIncidentMapper.java`

```java
@Mapper(componentModel = "spring")
public interface LaborIncidentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)          // el servicio setea la lista manualmente
    LaborIncident toEntity(LaborIncidentDTO dto);

    // MapStruct resuelve employees → List<EmployeeRef> por convención de nombres.
    // Si no lo hace automáticamente, agregar el método default toEmployeeRef(Employee e).
    LaborIncidentResponseDTO toResponseDto(LaborIncident entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)          // el servicio actualiza si dto.employeeIds != null
    void partialUpdate(LaborIncidentDTO dto, @MappingTarget LaborIncident entity);
}
```

### 4.2 Modificaciones a `DisciplinaryActionMapper`

En `toResponseDto`, agregar:
```java
@Mapping(source = "laborIncident.id",           target = "laborIncidentId")
@Mapping(source = "laborIncident.incidentType",  target = "laborIncidentType")
@Mapping(source = "laborIncident.incidentDate",  target = "laborIncidentDate")
```

En `toEntity` y `partialUpdate`, agregar:
```java
@Mapping(target = "laborIncident", ignore = true)          // el servicio asigna la entidad completa
```

---

## 5. Repositorios

### 5.1 `LaborIncidentRepository`

**Ubicación:** `repository/LaborIncidentRepository.java`

El `DISTINCT` es obligatorio: el `LEFT JOIN` con la tabla de unión genera duplicados para incidentes con múltiples empleados. El `countQuery` explícito es necesario para que la paginación funcione correctamente con `DISTINCT`.

```java
@Repository
public interface LaborIncidentRepository extends JpaRepository<LaborIncident, Long> {

    @Query(
        value =
            "SELECT DISTINCT li FROM LaborIncident li LEFT JOIN li.employees e " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR e.id = :employeeId) " +
            "AND (:employeeSearch IS NULL OR " +
            "     LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')) " +
            "     OR LOWER(CAST(e.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%'))) " +
            "AND (:incidentType IS NULL OR li.incidentType = :incidentType) " +
            "AND (:status IS NULL OR li.status = :status) " +
            "AND (CAST(:incidentDateFrom AS date) IS NULL OR li.incidentDate >= :incidentDateFrom) " +
            "AND (CAST(:incidentDateTo AS date) IS NULL OR li.incidentDate <= :incidentDateTo) " +
            "AND (:hasFinancialImpact IS NULL OR " +
            "     (:hasFinancialImpact = true AND li.financialImpact IS NOT NULL) OR " +
            "     (:hasFinancialImpact = false AND li.financialImpact IS NULL))",
        countQuery =
            "SELECT COUNT(DISTINCT li) FROM LaborIncident li LEFT JOIN li.employees e " +
            "WHERE (CAST(:employeeId AS long) IS NULL OR e.id = :employeeId) " +
            "AND (:employeeSearch IS NULL OR " +
            "     LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%')) " +
            "     OR LOWER(CAST(e.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:employeeSearch AS string), '%'))) " +
            "AND (:incidentType IS NULL OR li.incidentType = :incidentType) " +
            "AND (:status IS NULL OR li.status = :status) " +
            "AND (CAST(:incidentDateFrom AS date) IS NULL OR li.incidentDate >= :incidentDateFrom) " +
            "AND (CAST(:incidentDateTo AS date) IS NULL OR li.incidentDate <= :incidentDateTo) " +
            "AND (:hasFinancialImpact IS NULL OR " +
            "     (:hasFinancialImpact = true AND li.financialImpact IS NOT NULL) OR " +
            "     (:hasFinancialImpact = false AND li.financialImpact IS NULL))"
    )
    Page<LaborIncident> findAllWithFilters(
            @Param("employeeId")        Long employeeId,
            @Param("employeeSearch")    String employeeSearch,
            @Param("incidentType")      LaborIncidentType incidentType,
            @Param("status")            LaborIncidentStatus status,
            @Param("incidentDateFrom")  LocalDate incidentDateFrom,
            @Param("incidentDateTo")    LocalDate incidentDateTo,
            @Param("hasFinancialImpact") Boolean hasFinancialImpact,
            Pageable pageable
    );
}
```

### 5.2 Modificaciones a `DisciplinaryActionRepository`

Agregar el método de nullificación, invocado por `LaborIncidentService` antes de eliminar un incidente:

```java
@Modifying
@Query("UPDATE DisciplinaryAction da SET da.laborIncident = null WHERE da.laborIncident.id = :incidentId")
void nullifyLaborIncident(@Param("incidentId") Long incidentId);
```

---

## 6. Servicio

### 6.1 `ILaborIncidentService`

**Ubicación:** `service/port/ILaborIncidentService.java`

```java
public interface ILaborIncidentService {
    LaborIncidentResponseDTO createLaborIncident(LaborIncidentDTO dto);
    LaborIncidentResponseDTO getLaborIncidentById(Long id);
    LaborIncidentResponseDTO updateLaborIncident(Long id, LaborIncidentDTO dto);
    void deleteLaborIncident(Long id);
    Page<LaborIncidentResponseDTO> getAllLaborIncidents(LaborIncidentFilterDTO filterDTO, Pageable pageable);
}
```

### 6.2 `LaborIncidentService` — reglas de negocio

**Ubicación:** `service/implementation/LaborIncidentService.java`

`@Service @RequiredArgsConstructor`. Inyecciones: `LaborIncidentRepository`, `EmployeeRepository`, `DisciplinaryActionRepository`, `LaborIncidentMapper`, `MessageSourceHelper`.

**Validaciones en CREATE:**

1. `incidentDate` no puede ser futura → `LaborIncidentNotValidException` (mensaje `laborIncident.incidentDate.future`)
2. `employeeIds` debe tener al menos un elemento (reforzado en servicio además del Bean Validation)
3. Cada `employeeId` debe existir y no estar soft-deleted → `EmployeeNotFoundException` por cada ID inválido
4. `financialImpact`, si no es null, debe ser `>= 0` → `LaborIncidentNotValidException` (mensaje `laborIncident.financialImpact.negative`)
5. `resolvedDate` solo puede estar presente si `status == RESUELTO` → `LaborIncidentNotValidException` (mensaje `laborIncident.resolvedDate.onlyResolved`)
6. Si `resolvedDate` está presente, debe ser `>= incidentDate` → `LaborIncidentNotValidException` (mensaje `laborIncident.resolvedDate.afterIncidentDate`)
7. **Auto-fill:** si `status == RESUELTO && resolvedDate == null` → setear `entity.setResolvedDate(LocalDate.now())`

**Flujo de `createLaborIncident`:**
```
validateBusinessRules(dto)
→ List<Employee> employees = fetchAndValidateEmployees(dto.employeeIds())
→ LaborIncident entity = mapper.toEntity(dto)       // sin employees, sin id
→ entity.setEmployees(employees)
→ if (RESUELTO && resolvedDate == null) entity.setResolvedDate(LocalDate.now())
→ saved = repository.save(entity)
→ return mapper.toResponseDto(saved)
```

**Validaciones en UPDATE:**

Mismo patrón que `DisciplinaryActionService.validateBusinessRulesForUpdate`: al no recibir un campo en el DTO, se usa el valor ya existente en la entidad para evaluar las reglas cruzadas.

```
LaborIncidentType  effectiveType   = dto.incidentType()  != null ? dto.incidentType()  : existing.getIncidentType();
LocalDate effectiveDate            = dto.incidentDate()  != null ? dto.incidentDate()  : existing.getIncidentDate();
LaborIncidentStatus effectiveStatus = dto.status()       != null ? dto.status()        : existing.getStatus();
LocalDate effectiveResolvedDate    = dto.resolvedDate()  != null ? dto.resolvedDate()  : existing.getResolvedDate();
```

Reglas adicionales en UPDATE:
- Si `dto.employeeIds() != null` → **reemplazar** la lista completa (no merge parcial).
- Si el status efectivo pasa a `RESUELTO` y `effectiveResolvedDate` es null → auto-fill con `LocalDate.now()`.
- Si el status cambia **desde** `RESUELTO` a otro estado → NO limpiar `resolvedDate` automáticamente; el usuario puede hacerlo explícitamente enviando `resolvedDate: null`.

**Flujo de `deleteLaborIncident`:**
```
find existing or throw LaborIncidentNotFoundException
disciplinaryActionRepository.nullifyLaborIncident(id)   // desconectar DAs vinculadas antes de borrar
repository.delete(existing)
```

### 6.3 Modificaciones a `DisciplinaryActionService`

Inyectar `LaborIncidentRepository`.

En `createDisciplinaryAction`, luego de `validateBusinessRules` y antes del `save`:
```java
if (disciplinaryActionDTO.laborIncidentId() != null) {
    LaborIncident incident = laborIncidentRepository.findById(disciplinaryActionDTO.laborIncidentId())
        .orElseThrow(() -> new LaborIncidentNotFoundException(disciplinaryActionDTO.laborIncidentId()));
    disciplinaryAction.setLaborIncident(incident);
}
```

Mismo bloque en `updateDisciplinaryAction`, aplicado a `existingDisciplinaryAction`.

---

## 7. Endpoints REST

### 7.1 `LaborIncidentController`

**Ubicación:** `controller/LaborIncidentController.java`

`@RestController @RequestMapping("/api/v1/labor-incidents") @RequiredArgsConstructor`

`@Tag(name = "Labor Incidents", description = "API para el registro de incidentes laborales con impacto económico sobre la empresa.")`

| Método | Path | Permiso | Status |
|---|---|---|---|
| `POST`   | `/api/v1/labor-incidents`       | `LABOR_INCIDENT_WRITE`  | 201 |
| `GET`    | `/api/v1/labor-incidents`       | `LABOR_INCIDENT_READ`   | 200 |
| `GET`    | `/api/v1/labor-incidents/{id}`  | `LABOR_INCIDENT_READ`   | 200 |
| `PATCH`  | `/api/v1/labor-incidents/{id}`  | `LABOR_INCIDENT_WRITE`  | 200 |
| `DELETE` | `/api/v1/labor-incidents/{id}`  | `LABOR_INCIDENT_DELETE` | 204 |

**Parámetros del `GET` lista:**

`employeeId`, `employeeSearch`, `incidentType`, `status`, `incidentDateFrom`, `incidentDateTo`, `hasFinancialImpact`, `page` (default 0), `size` (default 10), `sortBy` (default `incidentDate`), `sortDir` (default `desc`).

**`mapSortField` en el controller:**

```java
private String mapSortField(String sortBy) {
    return switch (sortBy) {
        case "incidentType"    -> "incidentType";
        case "status"          -> "status";
        case "financialImpact" -> "financialImpact";
        default -> "incidentDate";  // sort por campo de empleado no soportado en ManyToMany
    };
}
```

El sort por campos de `employees` no se soporta: las relaciones ManyToMany no admiten ordenación directa en JPQL sin subqueries complejas. Cualquier valor de `sortBy` no reconocido cae al default `incidentDate`.

---

## 8. Excepciones

### 8.1 `LaborIncidentNotFoundException`

**Ubicación:** `exception/laborIncident/LaborIncidentNotFoundException.java`

```java
public class LaborIncidentNotFoundException extends RuntimeException {
    public LaborIncidentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("laborIncident.notFound", id));
    }
}
```

Extender la misma clase base que usa `DisciplinaryActionNotFoundException` para que `GlobalExceptionHandler` la mapee a HTTP 404 automáticamente.

### 8.2 `LaborIncidentNotValidException`

**Ubicación:** `exception/laborIncident/LaborIncidentNotValidException.java`

```java
public class LaborIncidentNotValidException extends RuntimeException {
    public LaborIncidentNotValidException(String message) {
        super(message);
    }
}
```

---

## 9. Permisos

```java
public class AppPermissions {
    // ... existentes
    public static final String LABOR_INCIDENT_READ   = "LABOR_INCIDENT_READ";
    public static final String LABOR_INCIDENT_WRITE  = "LABOR_INCIDENT_WRITE";
    public static final String LABOR_INCIDENT_DELETE = "LABOR_INCIDENT_DELETE";
}
```

El `PermissionSeeder` sincroniza automáticamente los nuevos permisos en la base de datos al arrancar. Asignarlos al rol administrador junto a los permisos existentes de `DISCIPLINARY_ACTION_*`.

---

## 10. Mensajes i18n

```properties
# ====== Feature 20 - Labor Incidents ======
laborIncident.notFound=Incidente laboral con id {0} no encontrado
laborIncident.incidentDate.future=La fecha del incidente no puede ser futura
laborIncident.incidentDate.pastOrPresent=La fecha del incidente no puede ser futura
laborIncident.employees.required=Debe especificar al menos un empleado involucrado
laborIncident.financialImpact.negative=El impacto económico no puede ser negativo
laborIncident.resolvedDate.onlyResolved=La fecha de resolución solo aplica cuando el estado es RESUELTO
laborIncident.resolvedDate.afterIncidentDate=La fecha de resolución debe ser posterior o igual a la fecha del incidente
laborIncident.description.size=La descripción debe tener entre 10 y 1000 caracteres
laborIncident.affectedAsset.size=El bien afectado no puede superar los 500 caracteres
laborIncident.notes.size=Las notas no pueden superar los 1000 caracteres
```

---

## 11. Frontend

### 11.1 Modelo: `labor-incident.model.ts`

**Ubicación:** `src/app/shared/models/labor-incident.model.ts`

```typescript
export interface LaborIncidentEmployee {
  id: number;
  name: string;
  lastName: string;
}

export interface LaborIncident {
  id?: number;
  incidentType: LaborIncidentType;
  incidentDate: string;
  description: string;
  financialImpact?: number | null;
  affectedAsset?: string | null;
  status: LaborIncidentStatus;
  resolvedDate?: string | null;
  notes?: string | null;
  employees: LaborIncidentEmployee[];
}

export enum LaborIncidentType {
  ACCIDENTE_VEHICULAR  = 'ACCIDENTE_VEHICULAR',
  MULTA_TRANSITO       = 'MULTA_TRANSITO',
  DANO_INFRAESTRUCTURA = 'DANO_INFRAESTRUCTURA',
  DANO_REDES_SERVICIO  = 'DANO_REDES_SERVICIO',
  DANO_MATERIAL        = 'DANO_MATERIAL',
  INFRACCION_NORMATIVA = 'INFRACCION_NORMATIVA',
  OTRO                 = 'OTRO',
}

export const LaborIncidentTypeLabels: Record<LaborIncidentType, string> = {
  [LaborIncidentType.ACCIDENTE_VEHICULAR]:  'Accidente vehicular',
  [LaborIncidentType.MULTA_TRANSITO]:       'Multa de tránsito',
  [LaborIncidentType.DANO_INFRAESTRUCTURA]: 'Daño a infraestructura',
  [LaborIncidentType.DANO_REDES_SERVICIO]:  'Daño a redes de servicio',
  [LaborIncidentType.DANO_MATERIAL]:        'Daño a material o equipo',
  [LaborIncidentType.INFRACCION_NORMATIVA]: 'Infracción normativa',
  [LaborIncidentType.OTRO]:                 'Otro',
};

export enum LaborIncidentStatus {
  PENDIENTE        = 'PENDIENTE',
  EN_INVESTIGACION = 'EN_INVESTIGACION',
  RESUELTO         = 'RESUELTO',
}

export const LaborIncidentStatusLabels: Record<LaborIncidentStatus, string> = {
  [LaborIncidentStatus.PENDIENTE]:        'Pendiente',
  [LaborIncidentStatus.EN_INVESTIGACION]: 'En investigación',
  [LaborIncidentStatus.RESUELTO]:         'Resuelto',
};

export interface LaborIncidentFilters {
  employeeId?:         number | null;
  employeeSearch?:     string;
  incidentType?:       LaborIncidentType | null;
  status?:             LaborIncidentStatus | null;
  incidentDateFrom?:   string;
  incidentDateTo?:     string;
  hasFinancialImpact?: boolean | null;
}
```

El uso de `Record<Enum, string>` con los objetos de labels permite obtener el texto legible desde el valor del enum sin switch ni if-else en los templates.

Modificar `disciplinary-action.model.ts` — agregar tres campos opcionales a la interfaz `DisciplinaryAction`:
```typescript
laborIncidentId?:   number | null;
laborIncidentType?: string | null;
laborIncidentDate?: string | null;
```

Modificar `src/app/shared/models/index.ts` — agregar:
```typescript
export * from './labor-incident.model';
```

### 11.2 Estructura del módulo

```
src/app/domains/human-resources/labor-incident/
├── services/
│   ├── labor-incident.service.ts           — HTTP: getAll, create, update, delete, getById
│   └── labor-incident-form.service.ts      — Subjects: created$, updated$, viewDetails$, edit$, closeDetails$, activeId$
├── labor-incident-page/
│   ├── labor-incident-page.ts
│   ├── labor-incident-page.html
│   └── labor-incident-page.scss            — vacío (estilos globales)
├── labor-incident-table/
│   ├── labor-incident-table.ts
│   ├── labor-incident-table.html
│   └── labor-incident-table.scss           — vacío
├── labor-incident-detail/
│   ├── labor-incident-detail.ts
│   ├── labor-incident-detail.html
│   └── labor-incident-detail.scss          — copiar base de disciplinary-action-detail.scss
└── labor-incident-form/
    ├── labor-incident-form.ts
    ├── labor-incident-form.html
    └── labor-incident-form.scss
```

### 11.3 Servicio HTTP: `labor-incident.service.ts`

Mismo patrón que `DisciplinaryActionService`. URL base: `/labor-incidents`. Mensajes toastr: `'Incidente laboral creado correctamente'`, `'...modificado...'`, `'...eliminado...'`. Tipado estricto con `LaborIncident` importado desde `../../../../shared/models`.

Mapeo de filtros en `getAll`: todos los campos de `LaborIncidentFilters`. Para `hasFinancialImpact` (boolean), convertir con `String(value)` al armar los `HttpParams`.

### 11.4 Form service: `labor-incident-form.service.ts`

Mismo patrón que `DisciplinaryActionFormService` pero **sin** `editBatch$` ni `pendingBatch`. Un incidente es un evento único; la edición batch no aplica semánticamente.

### 11.5 Page: `labor-incident-page`

Patrón idéntico a `disciplinary-action-page`. Signals: `showForm` y `showDetails`. Suscripciones a `created$` y `edit$` del form service. Cleanup con `DestroyRef`.

### 11.6 Table: `labor-incident-table`

Extiende `BaseTableDirective`. Permisos: `LABOR_INCIDENT_READ`, `LABOR_INCIDENT_WRITE`, `LABOR_INCIDENT_DELETE`. **Sin bulk edit** — un incidente es un evento único, la edición masiva no aplica. Sort default: `incidentDate` DESC.

**Columnas:**

| Key | Label | Sortable | Renderizado |
|---|---|---|---|
| `employees` | `Empleado(s)` | No | Apellidos concatenados: `"García, López"` |
| `incidentType` | `Tipo` | Sí | `LaborIncidentTypeLabels[row.incidentType]` |
| `incidentDate` | `Fecha` | Sí | `dd/MM/yyyy` |
| `financialImpact` | `Impacto económico` | Sí | Formato moneda si existe, `"-"` si null |
| `status` | `Estado` | Sí | Badge: PENDIENTE=naranja, EN_INVESTIGACION=amarillo, RESUELTO=verde |

**Filtros:**

| Campo | Tipo de input | Param backend |
|---|---|---|
| Empleado | Searchable select | `employeeId` / `employeeSearch` |
| Tipo | Select (opciones del enum) | `incidentType` |
| Estado | Select (opciones del enum) | `status` |
| Fecha desde | Date | `incidentDateFrom` |
| Fecha hasta | Date | `incidentDateTo` |
| Con impacto económico | Toggle booleano | `hasFinancialImpact` |

Cargar lista de empleados en `ngOnInit` desde `ReferenceService.getEmployees()` para el filtro de empleado (mismo patrón que `disciplinary-action-table`).

### 11.7 Detail: `labor-incident-detail`

Signal `item = signal<LaborIncident | null>(null)`. Métodos: `loadItem`, `onEdit`, `onClose`. Botón editar condicional por permiso `LABOR_INCIDENT_WRITE`.

**Secciones del HTML (en orden):**
1. Header: tipo de incidente con label legible + badge de estado con color
2. Empleados involucrados: lista `@for` con `empleado.lastName + ', ' + empleado.name`
3. Fecha del incidente (formato `dd/MM/yyyy`)
4. Fecha de resolución — solo si `item().resolvedDate` (formato `dd/MM/yyyy`)
5. Impacto económico formateado como moneda — solo si `item().financialImpact != null`
6. Bien afectado — solo si `item().affectedAsset`
7. Descripción
8. Notas — solo si `item().notes`
9. Footer: botones Cerrar + Editar

El scss se copia de `disciplinary-action-detail.scss` como base. Ajustar colores de los badges de status para que coincidan con los del sistema de colores global.

### 11.8 Form: `labor-incident-form`

Este formulario es más simple que el de `disciplinary-action`:
- **No soporta múltiples items** — un incidente = un único formulario
- **No soporta batch edit**
- **No tiene sección de defaults** (no tiene sentido para un evento único)

**Estado del componente (campos relevantes en el `.ts`):**

```typescript
// Multi-select de empleados
selectedEmployees: ReferenceItem[] = []
employeeQuery = ''
employeeDropdownOpen = false
employeeActiveIndex = -1

// Campos del incidente
incidentType = ''
incidentDate = ''
description = ''
financialImpact = ''       // string para el input, parseado a number en buildPayload
affectedAsset = ''
status = ''
resolvedDate = ''
notes = ''

isEditMode = false
editingId: number | null = null
loading = false
submitted = false
```

**Multi-select de empleados:** el usuario escribe en el input, el dropdown muestra empleados filtrados. Al seleccionar uno, se agrega a `selectedEmployees` (ignorando duplicados). Los empleados seleccionados se renderizan como chips con botón `×` para removerlos individualmente. El dropdown tiene navegación por teclado (ArrowDown/ArrowUp/Enter/Escape) idéntica a la de `disciplinary-action-form`.

**Visibilidad de `resolvedDate`:** visible solo cuando `status === 'RESUELTO'`. Al cambiar el status a un valor distinto de RESUELTO, limpiar `resolvedDate = ''`.

**`isValid`:**
```typescript
get isValid(): boolean {
  const impact = parseFloat(this.financialImpact);
  return this.selectedEmployees.length > 0
    && !!this.incidentType
    && !!this.incidentDate
    && this.description.trim().length >= 10
    && (this.status !== 'RESUELTO' || !!this.resolvedDate)
    && (this.financialImpact === '' || (!isNaN(impact) && impact >= 0));
}
```

**`loadItem(v: LaborIncident)`:** puebla todos los campos y mapea `v.employees` a `ReferenceItem[]` con formato `{ id: e.id, label: e.lastName + ', ' + e.name }`.

**`buildPayload()`:**
```typescript
{
  employeeIds:     this.selectedEmployees.map(e => Number(e.id)),
  incidentType:    this.incidentType,
  incidentDate:    this.incidentDate,
  description:     this.description,
  financialImpact: this.financialImpact !== '' ? parseFloat(this.financialImpact) : null,
  affectedAsset:   this.affectedAsset   || null,
  status:          this.status,
  resolvedDate:    this.resolvedDate     || null,
  notes:           this.notes            || null,
}
```

### 11.9 Modificaciones al módulo `disciplinary-action`

**`disciplinary-action-detail.html`** — agregar sección condicional entre el tipo de acción y la fecha de acción:

```html
@if (item().laborIncidentId) {
  <div class="detail-section">
    <span class="detail-label">Incidente asociado</span>
    <span class="incident-chip">
      {{ item().laborIncidentType }} — {{ item().laborIncidentDate | date:'dd/MM/yyyy' }}
    </span>
  </div>
}
```

Agregar estilo `.incident-chip` (badge neutro/gris) en el scss del componente.

**`disciplinary-action-form.ts`** — agregar:
- Campo `selectedIncident: { id: number; label: string } | null = null`
- Campo `incidentQuery = ''` e `incidentDropdownOpen = false`
- Array `incidents: { id: number; label: string }[] = []`
- Inyectar `LaborIncidentService`
- En `ngOnInit`: cargar incidentes con `getAll({ size: 200 })` y mapear a `{ id, label }` donde `label = tipoIncidente + ' — ' + fechaFormateada`
- `buildPayload()` incluye `laborIncidentId: this.selectedIncident?.id ?? null`
- `loadItem()` puebla `selectedIncident` si `v.laborIncidentId` existe
- `resetForm()` limpia `selectedIncident = null`, `incidentQuery = ''`

**`disciplinary-action-form.html`** — agregar campo opcional al final del formulario (antes del textarea de notas), con el mismo patrón de dropdown que los demás campos de búsqueda existentes. Mostrar la selección actual como chip removible.

### 11.10 Routing

Agregar una ruta lazy-loaded con el mismo patrón que la de `disciplinary-action` en el módulo de human-resources. Path sugerido: `incidentes-laborales`. Agregar la entrada correspondiente en el menú de navegación lateral con permiso `LABOR_INCIDENT_READ`.

---

## 12. Decisiones técnicas

| Decisión | Elección | Motivo |
|---|---|---|
| Relación empleados | ManyToMany con tabla de unión | Integridad referencial + queries eficientes + escalable |
| Tipo `financialImpact` | `BigDecimal` / `DECIMAL(15,2)` | Precisión monetaria exacta, sin errores de punto flotante |
| ON DELETE en tabla de unión | CASCADE | Sin el incidente, las relaciones intermedias no tienen sentido |
| ON DELETE en `disciplinary_actions` | SET NULL | Preserva el historial disciplinario aunque se borre el incidente |
| Batch edit en formulario | No soportado | Un incidente es un evento único; editar varios a la vez no tiene coherencia semántica |
| `resolvedDate` auto-fill | Sí, en el servicio | Reduce fricción del usuario; lógica centralizada en backend |
| Sort por campo de empleado | No soportado | ManyToMany no admite sort directo en JPQL sin subqueries complejas |
| Endpoint `/references` dedicado | No por ahora | Usar `getAll` con `size=200` es suficiente para el volumen actual |

---

## 13. Checklist de Implementación

### Backend

- [ ] Crear `LaborIncidentType.java` (enum)
- [ ] Crear `LaborIncidentStatus.java` (enum)
- [ ] Crear `LaborIncident.java` (entity)
- [ ] Modificar `DisciplinaryAction.java` — campo `laborIncident`
- [ ] Ejecutar migración SQL (3 bloques: tabla principal, tabla de unión, ALTER en `disciplinary_actions`)
- [ ] Crear `LaborIncidentDTO.java`
- [ ] Crear `LaborIncidentFilterDTO.java`
- [ ] Crear `LaborIncidentResponseDTO.java`
- [ ] Modificar `DisciplinaryActionDTO.java` — campo `laborIncidentId`
- [ ] Modificar `DisciplinaryActionResponseDTO.java` — 3 campos nuevos
- [ ] Crear `LaborIncidentMapper.java`
- [ ] Modificar `DisciplinaryActionMapper.java` — mappings del nuevo campo
- [ ] Crear `LaborIncidentRepository.java`
- [ ] Modificar `DisciplinaryActionRepository.java` — método `nullifyLaborIncident`
- [ ] Crear `LaborIncidentNotFoundException.java`
- [ ] Crear `LaborIncidentNotValidException.java`
- [ ] Crear `ILaborIncidentService.java`
- [ ] Crear `LaborIncidentService.java`
- [ ] Modificar `DisciplinaryActionService.java` — inyección + validación de FK
- [ ] Crear `LaborIncidentController.java`
- [ ] Modificar `AppPermissions.java` — 3 constantes nuevas
- [ ] Modificar `messages.properties` — 10 mensajes nuevos
- [ ] `mvn -q -Dmaven.test.skip=true compile` → 0 errores

### Frontend

- [ ] Crear `labor-incident.model.ts`
- [ ] Modificar `shared/models/index.ts` — agregar export
- [ ] Modificar `disciplinary-action.model.ts` — 3 campos en interfaz `DisciplinaryAction`
- [ ] Crear `labor-incident.service.ts`
- [ ] Crear `labor-incident-form.service.ts`
- [ ] Crear `labor-incident-page` (ts + html + scss)
- [ ] Crear `labor-incident-table` (ts + html + scss)
- [ ] Crear `labor-incident-detail` (ts + html + scss)
- [ ] Crear `labor-incident-form` (ts + html + scss)
- [ ] Modificar `disciplinary-action-detail.html` — chip de incidente
- [ ] Modificar `disciplinary-action-detail.scss` — estilo `.incident-chip`
- [ ] Modificar `disciplinary-action-form.ts` — dropdown de incidente
- [ ] Modificar `disciplinary-action-form.html` — campo incidente
- [ ] Agregar ruta lazy-loaded en human-resources routing module
- [ ] Agregar entrada en menú de navegación con permiso `LABOR_INCIDENT_READ`
- [ ] `npx tsc --noEmit` → 0 errores
