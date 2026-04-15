# Feature 11 — Sub-tareas dentro de Sectores y Áreas de Proyecto

## Contexto

Todos los registros del sistema se asocian a un sector o área de proyecto (`ProjectArea`), pero no existía la posibilidad de especificar a qué **tarea concreta** dentro de ese sector corresponde un registro. Esta feature agrega una entidad `ProjectAreaTask` que funciona como un segundo nivel de clasificación, completamente opcional y transversal a todos los módulos que trabajan con sectores.

> **Principio:** La sub-tarea es siempre opcional. Si un sector no tiene sub-tareas configuradas, la interfaz no muestra el campo adicional. Si el sector tiene sub-tareas, el usuario puede seleccionar una o dejar "Sin especificar".

---

## 1. Modelo de Datos

### 1.1 Entidad: `ProjectAreaTask`

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `id` | `Long` (PK, auto) | No | Identificador único |
| `tenant_id` | `Long` | No | Heredado de `TenantEntity` |
| `projectArea` | `ProjectArea` (FK) | No | Sector al que pertenece la sub-tarea |
| `name` | `String(150)` | No | Nombre descriptivo de la sub-tarea |
| `description` | `String(500)` | Sí | Descripción detallada (opcional) |
| `deleted` | `Boolean` | No | Soft delete. Default: `false` |

**Restricciones:**
- Unique constraint compuesto: `(tenant_id, project_area_id, name)` — No puede haber dos sub-tareas con el mismo nombre dentro del mismo sector y tenant.

```java
@Entity
@Table(name = "project_area_tasks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "project_area_id", "name"})
})
public class ProjectAreaTask extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id", nullable = false)
    private ProjectArea projectArea;

    @Column(nullable = false, columnDefinition = "VARCHAR(150)")
    private String name;

    @Column(columnDefinition = "VARCHAR(500)")
    private String description;

    @Column(nullable = false) @Builder.Default
    private Boolean deleted = false;
}
```

### 1.2 Tabla SQL: `project_area_tasks`

```sql
CREATE TABLE project_area_tasks (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    project_area_id  BIGINT       NOT NULL,
    name             VARCHAR(150) NOT NULL,
    description      VARCHAR(500) NULL,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_task_project_area FOREIGN KEY (project_area_id) REFERENCES project_areas(id),
    CONSTRAINT uk_task_per_area     UNIQUE (tenant_id, project_area_id, name)
);
```

### 1.3 FK en entidades transversales

La siguiente columna fue agregada a **10 tablas**:

```sql
ALTER TABLE <tabla> ADD COLUMN project_area_task_id BIGINT NULL
    REFERENCES project_area_tasks(id);
```

| Entidad | Tabla | Campo JPA |
|---|---|---|
| `Vehicle` | `vehicles` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `CrewAssignment` | `crew_assignments` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `Building` | `buildings` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `WorkContract` | `work_contracts` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `FuelLoad` | `fuel_loads` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `TransactionalDocument` | `transactional_documents` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `ServicePayment` | `service_payments` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `SalaryPayment` | `salary_payments` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `Employee` | `employees` | `@ManyToOne ProjectAreaTask projectAreaTask` |
| `SalesDocument` | `sales_documents` | `@ManyToOne ProjectAreaTask projectAreaTask` |

---

## 2. DTOs

### 2.1 `ProjectAreaTaskDTO` (record, CREATE/UPDATE)

```java
public record ProjectAreaTaskDTO(
    @NotNull Long projectAreaId,
    @NotNull @Size(max = 150) String name,
    @Size(max = 500) String description
) {}
```

### 2.2 `ProjectAreaTaskResponseDTO` (record)

```java
public record ProjectAreaTaskResponseDTO(
    Long id,
    String name,
    String description,
    Long projectAreaId,
    String projectAreaName,
    Boolean deleted
) {}
```

### 2.3 DTOs transversales modificados

Se agregaron dos campos a los DTOs de CREATE y RESPONSE de cada módulo:

| DTO (Create) | Campo agregado |
|---|---|
| `VehicleDTO` | `Long projectAreaTaskId` |
| `CrewAssignmentDTO` | `Long projectAreaTaskId` |
| `BuildingDTO` | `Long projectAreaTaskId` |
| `WorkContractDTO` | `Long projectAreaTaskId` |
| `FuelLoadDTO` | `Long projectAreaTaskId` |
| `TransactionalDocumentDTO` | `Long projectAreaTaskId` |
| `ServicePaymentDTO` | `Long projectAreaTaskId` |
| `SalaryPaymentDTO` | `Long projectAreaTaskId` |
| `EmployeeDTO` | `Long projectAreaTaskId` |
| `SalesDocumentDTO` | `Long projectAreaTaskId` |

| DTO (Response) | Campos agregados |
|---|---|
| `VehicleResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `CrewAssignmentResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `BuildingResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `WorkContractResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `FuelLoadResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `TransactionalDocumentResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `ServicePaymentResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `SalesDocumentResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |
| `EmployeeResponseDTO` | `Long projectAreaTaskId`, `String projectAreaTaskName` |

### 2.4 DTOs de Reportes

| DTO | Campo agregado | Uso |
|---|---|---|
| `SalaryReportPaymentDTO` | `String projectAreaTaskName` | Reporte de salarios — Capa 3 |
| `ReportItemDTO` | `String projectAreaTaskName` | Reportes de facturación, servicios, combustible, reparaciones — Capa 3 |

---

## 3. Backend — Capas

### 3.1 Mapper: `ProjectAreaTaskMapper`

```java
@Mapper(componentModel = "spring")
public interface ProjectAreaTaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ProjectAreaTask toEntity(ProjectAreaTaskDTO dto);

    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    ProjectAreaTaskResponseDTO toResponseDto(ProjectAreaTask entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ProjectAreaTask partialUpdate(ProjectAreaTaskDTO dto, @MappingTarget ProjectAreaTask entity);
}
```

### 3.2 Mappers transversales — Resolución de FK

En cada mapper que trabaja con entidades que tienen `projectAreaTask`, se agrega un mapping helper:

```java
// Ejemplo en VehicleMapper:
@Mapping(source = "projectAreaTaskId", target = "projectAreaTask", qualifiedByName = "resolveTask")
Vehicle toEntity(VehicleDTO dto);

@Named("resolveTask")
default ProjectAreaTask resolveTask(Long id) {
    if (id == null) return null;
    ProjectAreaTask t = new ProjectAreaTask();
    t.setId(id);
    return t;
}
```

En el response DTO:

```java
@Mapping(source = "projectAreaTask.id", target = "projectAreaTaskId")
@Mapping(source = "projectAreaTask.name", target = "projectAreaTaskName")
VehicleResponseDTO toResponseDto(Vehicle entity);
```

### 3.3 Repository: `ProjectAreaTaskRepository`

```java
@Repository
public interface ProjectAreaTaskRepository extends JpaRepository<ProjectAreaTask, Long> {

    Optional<ProjectAreaTask> findByIdAndDeletedFalse(Long id);

    List<ProjectAreaTask> findByProjectAreaIdAndDeletedFalseOrderByNameAsc(Long projectAreaId);

    boolean existsByProjectAreaIdAndNameAndDeletedFalse(Long projectAreaId, String name);

    boolean existsByIdAndDeletedFalse(Long id);

    Optional<ProjectAreaTask> findByProjectAreaIdAndNameAndDeletedTrue(
        Long projectAreaId, String name);
}
```

### 3.4 Service: `ProjectAreaTaskService`

Implementa `IProjectAreaTaskService`.

**Métodos:**

| Método | Descripción |
|---|---|
| `createTask(dto)` | Verifica unicidad de nombre. Si existe una tarea soft-deleted con el mismo nombre, la reactiva. Si no, crea nueva. |
| `updateTask(id, dto)` | Actualización parcial. Si se cambia el nombre, verifica unicidad. Si hay conflicto con una soft-deleted, elimina la soft-deleted primero. |
| `deleteTask(id)` | Soft delete (`deleted = true`). |
| `getTaskById(id)` | Lookup por ID (solo activas). |
| `getTasksByProjectArea(projectAreaId)` | Lista ordenada por nombre ASC (solo activas). |
| `getEntityById(id)` | Retorna entidad JPA (para resolución de FK en otros servicios). |

**Lógica de reactivación:**

```
Si existe task con mismo (projectArea, name) y deleted = true:
  → Reactivar: deleted = false, actualizar campos
  → Retornar la reactivada

Si existe task con mismo (projectArea, name) y deleted = false:
  → DuplicateException

Si no existe:
  → Crear nueva
```

### 3.5 Servicios transversales modificados

Cada servicio que gestiona una entidad con FK a `ProjectAreaTask` resuelve el `projectAreaTaskId` recibido en el DTO:

| Servicio | Cambio |
|---|---|
| `CrewAssignmentService` | Resuelve `projectAreaTaskId` en `create` y `update` |
| `BuildingService` | Resuelve `projectAreaTaskId` en `update` |
| `ReportService` | Popula `projectAreaTaskName` en `ReportItemDTO` y `SalaryReportPaymentDTO` |

---

## 4. Controller: `ProjectAreaTaskController`

Ruta base: `/api/v1/project-area-tasks`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/` | `PROJECT_AREA_WRITE` | Crear sub-tarea |
| `GET` | `/{id}` | `PROJECT_AREA_READ` | Obtener por ID |
| `GET` | `/by-project-area/{projectAreaId}` | `PROJECT_AREA_READ` | Listar sub-tareas de un sector |
| `PATCH` | `/{id}` | `PROJECT_AREA_WRITE` | Actualizar sub-tarea |
| `DELETE` | `/{id}` | `PROJECT_AREA_DELETE` | Soft-delete |

---

## 5. Migraciones de Base de Datos

### V56 — Crear tabla y FKs transversales

```sql
-- V56__create_project_area_tasks.sql

CREATE TABLE project_area_tasks (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    project_area_id  BIGINT       NOT NULL REFERENCES project_areas(id),
    name             VARCHAR(150) NOT NULL,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_task_tenant_area_name UNIQUE (tenant_id, project_area_id, name)
);

-- FK en cada tabla transversal:
ALTER TABLE buildings               ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE work_contracts          ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE fuel_loads              ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE sales_documents         ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE service_payments        ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE transactional_documents ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE salary_payments         ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE crew_assignments        ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE vehicles                ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
ALTER TABLE employees               ADD COLUMN project_area_task_id BIGINT REFERENCES project_area_tasks(id);
```

### V57 — Agregar campo `description`

```sql
-- V57__add_description_to_project_area_tasks.sql
ALTER TABLE project_area_tasks ADD COLUMN IF NOT EXISTS description VARCHAR(500) NULL;
```

---

## 6. Permisos

Reutiliza los permisos existentes del módulo de áreas de proyecto:

| Constante | Uso |
|---|---|
| `PROJECT_AREA_READ` | Listar sub-tareas de un sector |
| `PROJECT_AREA_WRITE` | Crear y editar sub-tareas |
| `PROJECT_AREA_DELETE` | Soft-delete de sub-tareas |

---

## 7. Frontend — Estructura

### 7.1 Modelo: `project-area.model.ts`

```typescript
export interface ProjectAreaTask {
    id: number;
    name: string;
    description?: string | null;
    projectAreaId: number;
    projectAreaName?: string;
    deleted?: boolean;
}
```

### 7.2 Servicio: `project-area.service.ts`

Métodos agregados al servicio existente de `ProjectArea`:

```typescript
getTasksByProjectArea(projectAreaId: number): Observable<ProjectAreaTask[]>
// GET /api/v1/project-area-tasks/by-project-area/{projectAreaId}

createTask(body: { projectAreaId: number; name: string; description?: string | null }): Observable<ProjectAreaTask>
// POST /api/v1/project-area-tasks

updateTask(id: number, body: { name: string; description?: string | null }): Observable<ProjectAreaTask>
// PATCH /api/v1/project-area-tasks/{id}

deleteTask(id: number): Observable<void>
// DELETE /api/v1/project-area-tasks/{id}
```

### 7.3 Modelos transversales modificados

Se agregó `projectAreaTaskId` y `projectAreaTaskName` a los siguientes modelos:

| Modelo | Campos agregados |
|---|---|
| `CrewAssignment` / `CrewAssignmentRequest` | `projectAreaTaskId`, `projectAreaTaskName` |
| `VehicleAssignmentDraft` | `projectAreaTaskId`, `projectAreaTaskName` |
| `VehicleCrew` | `projectAreaTaskId`, `projectAreaTaskName`, `projectAreaTasks: ProjectAreaTask[]` |
| `Vehicle` / `VehicleRequest` | `projectAreaTaskId`, `projectAreaTaskName` |
| `Building` / `BuildingRequest` | `projectAreaTaskId`, `projectAreaTaskName` |
| `TransactionalDocument` | `projectAreaTaskId`, `projectAreaTaskName` |
| `FuelLoad` | `projectAreaTaskId`, `projectAreaTaskName` |
| `SalesDocument` | `projectAreaTaskId`, `projectAreaTaskName` |
| `ServicePayment` | `projectAreaTaskId`, `projectAreaTaskName` |
| `WorkContract` | `projectAreaTaskId`, `projectAreaTaskName` |
| `Employee` | `projectAreaTaskId`, `projectAreaTaskName` |
| `AttendanceRecord` | `projectAreaTaskId`, `projectAreaTaskName` (heredado del employee) |

### 7.4 Componentes — Formularios con selector de sub-tarea

En todos los formularios que permiten seleccionar un sector, se agrega un `<select>` condicional para la sub-tarea:

```html
<!-- Patrón común en todos los formularios -->
@if (tasksForSelectedArea().length > 0) {
    <label>Sub-tarea</label>
    <select [ngModel]="projectAreaTaskId" (ngModelChange)="onTaskChange($event)">
        <option [value]="null">Sin especificar</option>
        @for (task of tasksForSelectedArea(); track task.id) {
            <option [value]="task.id">{{ task.name }}</option>
        }
    </select>
}
```

**Formularios modificados:**

| Componente | Ubicación |
|---|---|
| `vehicle-form` | `domains/vehicle-fleet/vehicle/vehicle-form/` |
| `work-contract-form` | `domains/contracts/work-contract/work-contract-form/` |
| `fuel-load-form` | `domains/vehicle-fleet/fuel-load/fuel-load-form/` (por ítem) |
| `service-payment-form` | `domains/services/service-payment/service-payment-form/` |
| `crew-vehicle-list` | `domains/vehicle-fleet/crew-assignment/crew-vehicle-list/` |

### 7.5 Componentes — Vistas de detalle

En todas las vistas de detalle, la sub-tarea se muestra como complemento del sector:

```html
<!-- Patrón: "Sector — Sub-tarea" -->
{{ projectAreaName }}
@if (projectAreaTaskName) {
    — {{ projectAreaTaskName }}
}
```

**Vistas modificadas:**

| Componente | Ubicación |
|---|---|
| `vehicle-detail` | `domains/vehicle-fleet/vehicle/vehicle-detail/` |
| `crew-daily-detail` | `domains/vehicle-fleet/crew-assignment/crew-daily-detail/` |
| `fuel-load-detail` | `domains/vehicle-fleet/fuel-load/fuel-load-detail/` |
| `work-contract-detail-page` | `domains/contracts/work-contract-detail-page/` |
| `transactional-document-detail` | `domains/finance/transacional-document/transactional-document-detail/` |
| `supplier-detail` | `domains/company/supplier/supplier-detail/` |
| `service-payment-detail` | `domains/services/service-payment/service-payment-detail/` |

### 7.6 Componentes — Parte Diario (Crew Builder)

```typescript
// crew-builder.ts
projectAreaTasksMap = signal<Map<number, ProjectAreaTask[]>>(new Map());

// Al inicializar, carga las sub-tareas por cada área referenciada:
loadTasks(projectAreaId: number) {
    this.projectAreaService.getTasksByProjectArea(projectAreaId).subscribe(tasks => {
        this.projectAreaTasksMap.update(map => {
            map.set(projectAreaId, tasks);
            return new Map(map);
        });
    });
}

// En el template:
updateTask(vehicleId: number, taskId: number | null) {
    // Actualiza el draft signal del vehículo correspondiente
}
```

El `crew-vehicle-list` muestra el dropdown de sub-tarea **solo si** el área del vehículo tiene sub-tareas cargadas:

```html
<!-- crew-vehicle-list.html -->
@if (vehicle.projectAreaTasks?.length > 0) {
    <select (change)="onTaskChange($event, vehicle.vehicleId)">
        <option [value]="null">Sin especificar</option>
        @for (task of vehicle.projectAreaTasks; track task.id) {
            <option [value]="task.id" [selected]="task.id === vehicle.projectAreaTaskId">
                {{ task.name }}
            </option>
        }
    </select>
}
```

### 7.7 Componentes — Reportes

| Reporte | Cambio |
|---|---|
| `report-table` | Muestra `projectAreaTaskName` junto al nombre del área en la tabla de detalle |
| `salary-report` | Columna "Sub-tarea" dedicada en el reporte de pago de sueldos |

```html
<!-- report-table.html — Capa 3 -->
<td>{{ item.projectAreaName }}
    @if (item.projectAreaTaskName) { — {{ item.projectAreaTaskName }} }
</td>
```

```html
<!-- salary-report.html — Columna dedicada -->
<th>Sub-tarea</th>
...
<td>{{ payment.projectAreaTaskName ?? '—' }}</td>
```

### 7.8 Rutas

No se agrega una ruta nueva. La gestión de sub-tareas se realiza a través del servicio `ProjectAreaService` desde los formularios y builders que referencian áreas de proyecto.

---

## 8. Alcance Transversal — Resumen

| Módulo | Formulario | Detalle | Reporte |
|---|---|---|---|
| Vehículos | ✅ | ✅ | ✅ (general) |
| Contratos de Obra | ✅ | ✅ | — |
| Cargas de Combustible | ✅ (por ítem) | ✅ | ✅ (combustible) |
| Pagos de Servicio | ✅ | ✅ | ✅ (servicios) |
| Comprobantes de Venta | — | — | — |
| Comprobantes de Compra | — | ✅ | ✅ (facturación) |
| Empleados | — | ✅ | — |
| Sueldos | — | ✅ | ✅ (salarios) |
| Cuadrillas / Parte Diario | ✅ | ✅ | — |
| Proveedores | — | ✅ | — |
| Reportes | — | — | General y sueldos |
