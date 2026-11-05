# Feature 10 — Registro de Kilómetros en Parte Diario y Vehículos

## Contexto

No existía un mecanismo para registrar el kilometraje de los vehículos día a día. La lectura de odómetro solo se podía conocer revisando el vehículo presencialmente, y no quedaba constancia histórica en el sistema.

Esta feature agrega un campo `km` al módulo de **parte diario / cuadrillas** (`CrewAssignment`) y al **vehículo** (`Vehicle`). El flujo es:

1. Al confeccionar el parte diario, el operador ingresa la lectura de odómetro de cada vehículo junto con la asignación de cuadrilla.
2. Al guardar, el sistema **sincroniza automáticamente** el valor al campo `km` de la ficha del vehículo.
3. Al consultar un parte diario de cualquier fecha pasada, se muestra el kilometraje registrado ese día.

> **Principio:** El km es un dato de la asignación (capturado en el momento operativo) y un dato derivado del vehículo (siempre refleja la última lectura informada).

---

## 1. Modelo de Datos

### 1.1 Entidad: `Vehicle` — Campo agregado

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `km` | `Integer` | Sí | Kilometraje actual del vehículo. Sincronizado automáticamente desde la última asignación de cuadrilla. |

**Mapper:** El campo `km` está **ignorado** en `VehicleMapper.toEntity()` y `partialUpdate()` — es de **sólo lectura**, se actualiza exclusivamente a través de la sincronización desde `CrewAssignment`.

```java
@Mapping(target = "km", ignore = true)
Vehicle toEntity(VehicleDTO dto);

@Mapping(target = "km", ignore = true)
Vehicle partialUpdate(VehicleDTO dto, @MappingTarget Vehicle entity);
```

### 1.2 Entidad: `CrewAssignment` — Campo agregado

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `km` | `Integer` | Sí | Lectura de odómetro del vehículo al momento de la asignación. |

El campo se persiste por cada asignación individual. Si un vehículo tiene 3 tripulantes asignados un día, los 3 registros comparten el mismo valor de km (se toma del primero al consultar el resumen diario).

---

## 2. DTOs

### 2.1 `CrewAssignmentDTO` (record, CREATE/UPDATE)

```java
public record CrewAssignmentDTO(
    @NotNull Long vehicleId,
    @NotNull Long employeeId,
    @NotNull LocalDate date,
    Long projectAreaId,
    Long projectAreaTaskId,
    Boolean isDriver,
    Integer km          // ← opcional
) {}
```

### 2.2 `CrewAssignmentResponseDTO` (record)

```java
public record CrewAssignmentResponseDTO(
    Long id,
    Long vehicleId,
    String vehicleLicensePlate,
    String vehicleBrand,
    String vehicleModel,
    Long employeeId,
    String employeeName,
    String employeeLastName,
    LocalDate date,
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    Long projectAreaTaskId,
    String projectAreaTaskName,
    Boolean isDriver,
    Integer km              // ← incluido en respuesta
) {}
```

### 2.3 `VehicleCrewDTO` (record, Resumen diario por vehículo)

```java
public record VehicleCrewDTO(
    Long vehicleId,
    String licensePlate,
    String brand,
    String model,
    Integer km,             // ← km del primer assignment del vehículo ese día
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    Long projectAreaTaskId,
    String projectAreaTaskName,
    List<ProjectAreaTask> projectAreaTasks,
    List<CrewMemberDTO> members
) {}
```

### 2.4 `VehicleResponseDTO` — Campo agregado

| Campo | Tipo | Descripción |
|---|---|---|
| `km` | `Integer` | "Current mileage of the vehicle (km). Synced from the latest crew assignment." |

> **Nota:** `VehicleDTO` (para CREATE/UPDATE) **no incluye** `km` — es de sólo lectura en la ficha del vehículo.

---

## 3. Backend — Lógica de Sincronización

### 3.1 Servicio: `CrewAssignmentService`

El método clave que sincroniza el km del vehículo:

```java
private void syncVehicleKm(Vehicle vehicle, Integer km) {
    if (km != null) {
        vehicle.setKm(km);
        vehicleRepository.save(vehicle);
    }
}
```

**Puntos de invocación:**

| Método | Cuándo sincroniza |
|---|---|
| `createCrewAssignment(dto)` | Después de persistir una asignación individual |
| `createBatchCrewAssignments(batchDTO)` | Después de persistir cada asignación del lote |

> **Comportamiento:** Si el campo `km` del DTO es `null`, no se modifica el vehículo. Si tiene valor, se sobreescribe el `km` del vehículo con el nuevo valor. El último lote guardado para un vehículo en un día define el km actual.

### 3.2 Resumen Diario (`getDailyCrewSummary`)

Al obtener el resumen diario para una fecha, el servicio agrupa las asignaciones por vehículo y toma el `km` del **primer** registro de cada grupo:

```java
VehicleCrewDTO(
    vehicleId,
    licensePlate, brand, model,
    first.getKm(),    // ← km de la primera asignación del vehículo ese día
    ...
)
```

---

## 4. Controller: `CrewAssignmentController`

Ruta base: `/api/v1/crew-assignments`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/` | `CREW_ASSIGNMENT_WRITE` | Crear asignación (body incluye `km`) |
| `POST` | `/batch` | `CREW_ASSIGNMENT_WRITE` | Crear lote de asignaciones → sincroniza km por cada vehículo |
| `GET` | `/daily?date=` | `CREW_ASSIGNMENT_READ` | Resumen diario → retorna `VehicleCrewDTO.km` |
| `PATCH` | `/{id}` | `CREW_ASSIGNMENT_WRITE` | Actualizar asignación (puede actualizar `km`) |
| `DELETE` | `/{id}` | `CREW_ASSIGNMENT_DELETE` | Eliminar asignación |

> **Nota:** No existe un endpoint dedicado para el km. Se transmite como parte del flujo normal de asignación de cuadrillas.

---

## 5. Migraciones de Base de Datos

### V54 — Mileage en Reparaciones (relacionado)

```sql
-- V54__refactor_repairs_add_items_and_mileage.sql
ALTER TABLE repairs ADD COLUMN mileage INTEGER;
```

> Campo `mileage` en `Repair` es el km del vehículo al momento de la reparación. Pre-existente al flujo de parte diario, pero conceptualmente relacionado.

### V55 — Km en Asignaciones y Vehículos

```sql
-- V55__add_km_to_crew_assignments_and_vehicles.sql
ALTER TABLE crew_assignments ADD COLUMN km INTEGER;
ALTER TABLE vehicles ADD COLUMN km INTEGER;
ALTER TABLE crew_assignments DROP CONSTRAINT IF EXISTS uk_crew_employee_date;
```

---

## 6. Permisos

Reutiliza los permisos existentes del módulo de cuadrillas:

| Constante | Uso |
|---|---|
| `CREW_ASSIGNMENT_READ` | Ver resumen diario con km |
| `CREW_ASSIGNMENT_WRITE` | Crear/editar asignaciones con km |
| `CREW_ASSIGNMENT_DELETE` | Eliminar asignaciones |

---

## 7. Frontend — Estructura

### 7.1 Modelo: `crew-assignment.model.ts`

```typescript
export interface CrewAssignment {
    // ... campos existentes ...
    km?: number | null;
}

export interface CrewAssignmentRequest {
    // ... campos existentes ...
    km?: number;
}

export interface VehicleCrew {
    // ... campos existentes ...
    km?: number | null;
}

export interface VehicleAssignmentDraft {
    // ... campos existentes ...
    km?: number | null;  // estado local del builder
}
```

### 7.2 Modelo: `vehicle.model.ts`

```typescript
export interface Vehicle {
    // ... campos existentes ...
    km?: number | null;  // "Current vehicle mileage (km). Synced from the latest crew assignment."
}
```

> `VehicleRequest` **no incluye** `km` — sólo lectura en la ficha del vehículo.

### 7.3 Componentes

**`crew-vehicle-list` — Input de km por vehículo:**

```html
<!-- crew-vehicle-list.html -->
<input type="number"
       [value]="vehicle.km"
       (change)="onKmChange($event, vehicle.vehicleId)"
       placeholder="Km"
       class="crew-km-input" />
```

- `@Output() kmChanged` emite `{ vehicleId: number, km: number | null }`
- `onKmChange()` parsea el input y emite el evento

**`crew-builder` — Orquestador del parte diario:**

```typescript
// crew-builder.ts
updateKm(vehicleId: number, km: number | null) {
    // Actualiza el draft signal del vehículo correspondiente
}

// En save():
km: v.km ?? undefined  // incluido en cada CrewAssignmentRequest del batch
```

- Al cargar un resumen diario existente, mapea `v.km` desde la respuesta del API
- `(kmChanged)` del template wired a `updateKm()`

**`crew-daily-detail` — Vista de consulta (solo lectura):**

```html
<!-- crew-daily-detail.html -->
@if (vehicle.km != null) {
    <span class="vehicle-km-badge">{{ vehicle.km | number:'1.0-0' }} km</span>
}
```

**`vehicle-detail` — Ficha del vehículo:**

```html
<!-- vehicle-detail.html -->
<span class="doc-form__display">
    {{ v.km != null ? (v.km | number:'1.0-0') + ' km' : '—' }}
</span>
```

### 7.4 Integración con Reparaciones

El módulo de reparaciones tiene su propio campo `mileage` (pre-existente):

| Componente | Campo | Descripción |
|---|---|---|
| `repair-form` | `mileage` input | Km del vehículo al momento de la reparación |
| `repair-detail` | `mileage` display | Muestra el km registrado |
| `repair-table` | `minMileage` / `maxMileage` | Filtros de rango en la tabla |
| `repair-order-complete-form` | `mileage` input | Km al completar una orden de reparación |

### 7.5 Ruta

El registro de km forma parte de la ruta existente del parte diario:

```typescript
// vehicle-fleet.routes.ts
{
    path: 'parte-diario',
    loadComponent: async () =>
        (await import('./crew-assignment/crew-assignment-page/crew-assignment-page')).CrewAssignmentPage,
    canMatch: [hasPermissionMatch('CREW_ASSIGNMENT_READ')],
}
```

Ruta completa: `/flota/parte-diario`

---

## 8. Diagrama de Flujo

```
Operador en Parte Diario              Sistema
         │                                │
         ▼                                │
Asigna cuadrilla a vehículo              │
  · Empleados                             │
  · Fecha                                 │
  · Km del vehículo (opcional)            │
         │                                │
         ▼                                │
    [Guardar]  ──POST /batch──►   CrewAssignmentService
                                          │
                                   ┌──────┴──────┐
                                   │             │
                              Persiste       syncVehicleKm()
                           asignaciones          │
                                            vehicle.km = km
                                            vehicleRepo.save()
                                                 │
                                                 ▼
                                     Vehículo actualizado
                                     con último km informado
```
