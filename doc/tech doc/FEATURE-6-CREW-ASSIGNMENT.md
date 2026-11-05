# Feature 6 — Parte Diario: Asignación de Cuadrillas a Vehículos

## Contexto

En una constructora, cada mañana se decide qué empleados viajan en qué vehículo y a qué zona de obra se dirigen. Este acto operativo se denomina "parte diario" o "partes de cuadrilla". Antes de esta feature, la asignación se hacía en papel y no quedaba registro digital.

El módulo de parte diario permite:

1. **Armar cuadrillas** — Asignar empleados a vehículos para una fecha determinada, indicando zona de proyecto, sub-tarea (opcional) y quién conduce.
2. **Vista constructor (Builder)** — Interfaz drag-and-drop para armar el parte diario arrastrando empleados a tarjetas de vehículo.
3. **Resumen diario** — Vista de solo lectura que muestra las cuadrillas conformadas para cualquier fecha.
4. **Calendario** — Vista mensual con indicadores de cantidad de vehículos y empleados asignados por día.
5. **Historial** — Tabla convencional con filtros, paginación y ordenamiento.
6. **Creación batch** — Un solo POST envía todas las asignaciones del día, con validaciones y advertencias no bloqueantes.

---

## 1. Modelo de Datos

### 1.1 Entidad: `CrewAssignment`

Cada fila representa la asignación de **un empleado a un vehículo** en una fecha.

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `id` | `Long` (PK, auto) | No | Identificador |
| `employee` | FK → `Employee` | No | Empleado asignado |
| `vehicle` | FK → `Vehicle` | No | Vehículo al que se asigna |
| `projectArea` | FK → `ProjectArea` | No | Zona de proyecto destino |
| `projectAreaTask` | FK → `ProjectAreaTask` | Sí | Sub-tarea dentro de la zona (opcional) |
| `date` | `LocalDate` | No | Fecha de la asignación |
| `driver` | `boolean` (`is_driver`) | No | Indica si el empleado es el conductor. Default `false` |
| `observation` | `String(500)` | Sí | Nota libre |
| `km` | `Integer` | Sí | Lectura de odómetro (ver Feature 10) |
| `deleted` | `boolean` | — | Soft-delete |

### 1.2 Tabla SQL: `crew_assignments`

```sql
CREATE TABLE crew_assignments (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    employee_id      BIGINT       NOT NULL REFERENCES employees(id),
    vehicle_id       BIGINT       NOT NULL REFERENCES vehicles(id),
    project_area_id  BIGINT       NOT NULL REFERENCES project_areas(id),
    project_area_task_id BIGINT   NULL     REFERENCES project_area_tasks(id),
    date             DATE         NOT NULL,
    is_driver        BOOLEAN      NOT NULL DEFAULT FALSE,
    observation      VARCHAR(500) NULL,
    km               INTEGER      NULL,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_crew_date ON crew_assignments(date);
CREATE INDEX idx_crew_employee ON crew_assignments(employee_id);
CREATE INDEX idx_crew_vehicle ON crew_assignments(vehicle_id);
```

---

## 2. Backend — Capas

### 2.1 DTOs

#### `CrewAssignmentDTO` (record, CREATE/UPDATE)

```java
public record CrewAssignmentDTO(
    @NotNull Long vehicleId,
    @NotNull Long employeeId,
    @NotNull LocalDate date,
    Long projectAreaId,
    Long projectAreaTaskId,
    Boolean isDriver,
    @Size(max = 500) String observation,
    Integer km
) {}
```

#### `CrewAssignmentResponseDTO` (record)

```java
public record CrewAssignmentResponseDTO(
    Long id,
    Long vehicleId, String vehicleLicensePlate, String vehicleBrand, String vehicleModel,
    Long employeeId, String employeeName, String employeeLastName,
    LocalDate date,
    Long projectAreaId, String projectAreaName, String projectAreaColor,
    Long projectAreaTaskId, String projectAreaTaskName,
    Boolean isDriver,
    Integer km
) {}
```

#### `CrewAssignmentBatchDTO` (record)

```java
public record CrewAssignmentBatchDTO(
    @NotNull @Size(min = 1, max = 200) @Valid
    List<CrewAssignmentDTO> assignments
) {}
```

#### `CrewAssignmentBatchResponseDTO` (record)

```java
public record CrewAssignmentBatchResponseDTO(
    List<CrewAssignmentResponseDTO> created,
    List<CrewAssignmentWarningDTO> warnings
) {}
```

#### `CrewAssignmentWarningDTO` (record)

```java
public record CrewAssignmentWarningDTO(
    String code,    // W1, W2, W3, W4
    String message,
    Long employeeId,
    Long vehicleId
) {}
```

**Códigos de advertencia:**

| Código | Significado |
|---|---|
| `W1` | Empleado asignado como conductor pero no tiene rol CHOFER |
| `W2` | Vehículo con más de 2 ocupantes asignados |
| `W3` | Empleado duplicado en el mismo día |
| `W4` | Vehículo duplicado en el mismo lote |

#### `DailyCrewSummaryDTO` (record)

```java
public record DailyCrewSummaryDTO(
    LocalDate date,
    List<VehicleCrewDTO> vehicles
) {}
```

#### `VehicleCrewDTO` (record)

```java
public record VehicleCrewDTO(
    Long vehicleId, String licensePlate, String brand, String model,
    Integer km,
    Long projectAreaId, String projectAreaName, String projectAreaColor,
    Long projectAreaTaskId, String projectAreaTaskName,
    List<ProjectAreaTask> projectAreaTasks,
    List<CrewMemberDTO> members
) {}
```

#### `CrewMemberDTO` (record)

```java
public record CrewMemberDTO(
    Long assignmentId,
    Long employeeId, String name, String lastName, String dni,
    List<RoleResponseDTO> roles,
    Boolean isDriver,
    String observation
) {}
```

#### `CrewCalendarDTO` y `CrewCalendarDayDTO` (records)

```java
public record CrewCalendarDTO(int year, int month, List<CrewCalendarDayDTO> days) {}
public record CrewCalendarDayDTO(LocalDate date, int vehicleCount, int employeeCount) {}
```

#### `CrewAssignmentFilterDTO` (record)

12 campos de filtro: `employeeId`, `firstName`, `lastName`, `dni`, `vehicleId`, `licensePlate`, `projectAreaId`, `projectAreaTaskId`, `dateFrom`, `dateTo`, `isDriver`, `search`.

### 2.2 Controller: `CrewAssignmentController`

Ruta base: `/api/v1/crew-assignments`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/` | `CREW_ASSIGNMENT_WRITE` | Crear una asignación individual |
| `POST` | `/batch` | `CREW_ASSIGNMENT_WRITE` | Crear lote de asignaciones (max 200). Retorna warnings no bloqueantes |
| `GET` | `/` | `CREW_ASSIGNMENT_READ` | Listar con filtros + paginación |
| `GET` | `/{id}` | `CREW_ASSIGNMENT_READ` | Obtener por ID |
| `GET` | `/daily?date=` | `CREW_ASSIGNMENT_READ` | Resumen diario agrupado por vehículo |
| `GET` | `/calendar?year=&month=` | `CREW_ASSIGNMENT_READ` | Resumen mensual (vehículos y empleados por día) |
| `PATCH` | `/{id}` | `CREW_ASSIGNMENT_WRITE` | Actualizar asignación |
| `PATCH` | `/{id}/driver` | `CREW_ASSIGNMENT_WRITE` | Toggle flag conductor (con uniqueness check) |
| `DELETE` | `/{id}` | `CREW_ASSIGNMENT_DELETE` | Eliminar asignación individual |
| `DELETE` | `/daily?date=` | `CREW_ASSIGNMENT_DELETE` | Eliminar todas las asignaciones de una fecha (bulk soft-delete) |

### 2.3 Service: `CrewAssignmentService`

Implementa `ICrewAssignmentService`. Métodos principales:

- `createCrewAssignment(dto)` — Valida entidades referenciadas, persiste, sincroniza km del vehículo.
- `createBatchCrewAssignments(batchDTO)` — Itera asignaciones, genera warnings (W1-W4), retorna lista de creados + lista de advertencias.
- `getDailyCrewSummary(date)` — Agrupa asignaciones por vehículo para la fecha indicada, retorna estructura anidada.
- `getCalendarSummary(year, month)` — Agrega conteos de vehículos y empleados por día para un mes.
- `toggleDriver(id)` — Alterna el flag `driver`. Si ya hay otro conductor en el mismo vehículo+fecha, lanza excepción.
- `deleteDailyAssignments(date)` — Soft-delete masivo de todas las asignaciones de una fecha (usado por el builder al "sobreescribir" un día).

**Flujo del Builder:**
El frontend construye el parte diario completo, y al guardar ejecuta:
1. `DELETE /daily?date=2025-01-15` — borra asignaciones previas del día.
2. `POST /batch` — crea las nuevas asignaciones.

### 2.4 Repository: `CrewAssignmentRepository`

Métodos clave:

- `findAllWithFilters(...)` — JPQL con 12 filtros null-safe.
- `findByDateAndDeletedFalseOrdered(date)` — Asignaciones del día ordenadas para resumen diario.
- `findDriverByVehicleAndDate(vehicleId, date)` — Busca conductor existente (uniqueness check).
- `countByDateRange(startDate, endDate)` — Agregado para calendario.
- `softDeleteByDate(date)` — `@Modifying UPDATE SET deleted = true WHERE date = :date`.

---

## 3. Frontend — Estructura

### 3.1 Ubicación

```
domains/vehicle-fleet/crew-assignment/
├── crew-builder/
│   ├── crew-builder.ts
│   ├── crew-builder.html
│   └── crew-builder.scss
├── crew-daily-detail/
│   ├── crew-daily-detail.ts
│   ├── crew-daily-detail.html
│   └── crew-daily-detail.scss
├── crew-employee-list/
│   ├── crew-employee-list.ts
│   ├── crew-employee-list.html
│   └── crew-employee-list.scss
├── crew-vehicle-list/
│   ├── crew-vehicle-list.ts
│   ├── crew-vehicle-list.html
│   └── crew-vehicle-list.scss
├── crew-history/
│   ├── crew-history.ts
│   ├── crew-history.html
│   └── crew-history.scss
├── crew-assignment-page/
│   ├── crew-assignment-page.ts
│   ├── crew-assignment-page.html
│   └── crew-assignment-page.scss
└── services/
    ├── crew-assignment.service.ts
    └── crew-assignment-form.service.ts
```

### 3.2 Componente: `CrewBuilder`

**Archivo:** `crew-builder.ts`

Interface de construcción del parte diario con drag-and-drop (Angular CDK):

- **Panel izquierdo:** Lista de empleados disponibles (`CrewEmployeeList`), filtrable.
- **Panel derecho:** Lista de tarjetas de vehículos (`CrewVehicleList`), cada una con sus miembros asignados.
- **Flujo:** Arrastrar un empleado desde la lista hacia una tarjeta de vehículo para asignarlo.
- **Filtro por zona de proyecto:** Permite seleccionar una `ProjectArea` que se aplica a todas las asignaciones.
- **Señales:** `date` (fecha seleccionada), `assignments` (estado local del builder), `areaFilter`.
- **Guardar:** Ejecuta `deleteDaily(date)` + `createBatch(assignments)`. Muestra warnings en un diálogo si los hay.

### 3.3 Componente: `CrewDailyDetail`

**Archivo:** `crew-daily-detail.ts`

Vista de solo lectura del resumen diario. Muestra tarjetas de vehículo con sus tripulantes, zona de proyecto, sub-tarea y km. Accesible desde el calendario al hacer clic en un día.

### 3.4 Componente: `CrewHistory`

Tabla convencional con filtros, paginación y sort. Muestra el historial de todas las asignaciones.

### 3.5 Ruta

**Archivo:** `domains/vehicle-fleet/vehicle-fleet.routes.ts`

```typescript
{ path: 'parte-diario', component: CrewAssignmentPage, canActivate: [permissionGuard('CREW_ASSIGNMENT_READ')] }
```

Ruta completa: `/flota/parte-diario`

**Navegación:** Entrada en el menú lateral bajo "Flota" con label "Parte Diario" y permiso `CREW_ASSIGNMENT_READ`.

---

## 4. Permisos

| Constante | Uso |
|---|---|
| `CREW_ASSIGNMENT_READ` | Ver parte diario, resumen, calendario, historial |
| `CREW_ASSIGNMENT_WRITE` | Crear/editar asignaciones, toggle conductor |
| `CREW_ASSIGNMENT_DELETE` | Eliminar asignaciones individuales o por día |

---

## 5. Migraciones

| Migración | Descripción |
|---|---|
| `V42__create_crew_assignments.sql` | Tabla `crew_assignments`, índices, unique constraint, permisos |
| `V55__add_km_to_crew_assignments_and_vehicles.sql` | Agrega columna `km` (ver Feature 10) |
| `V56__create_project_area_tasks.sql` | Agrega FK a `project_area_tasks` (ver Feature 11) |

---
