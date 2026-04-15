# Feature 5 — Órdenes de Reparación (Flujo de Trabajo en Taller)

## Contexto

Actualmente el sistema registra reparaciones de vehículos (`Repair`) con información técnica y de costo, pero no contempla un flujo que diferencie una rotura reportada de una reparación ya ejecutada.

El área de taller necesita gestionar un tablero de trabajos pendientes para planificar mano de obra y materiales. Existen dos perfiles de usuario con responsabilidades distintas:

- **Operario de campo:** detecta y reporta la falla.
- **Personal de taller:** ejecuta la reparación y la registra en el sistema.

---

## Flujo de trabajo

```
Operario de campo                   Personal de taller
        │                                   │
        ▼                                   │
Crea Orden de Reparación            Ve listado de órdenes
  · Vehículo                        (PENDIENTE / EN_PROCESO)
  · Fecha                                   │
  · Descripción de la falla          ┌──────┴───────┐
                                  [Iniciar]     [Finalizar]
                                     │               │
                                EN_PROCESO     Abre formulario
                                               completo de reparación.
                                               Al guardar → crea Repair
                                               vinculada a la orden.
                                               Orden → COMPLETADA.
```

> **Permisos del operario de campo:**
> - Solo ve sus propias órdenes (`GET /mine`).
> - Puede editar y eliminar sus órdenes solo si están en estado `PENDIENTE`.
> - No tiene acceso a reparaciones finalizadas ni puede cambiar estados.

---

## Modelo de datos

### Nueva entidad: `RepairOrder`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK autoincremental |
| `vehicle` | FK → `Vehicle` | Vehículo afectado |
| `date` | `LocalDate` | Fecha en que se reportó la falla |
| `description` | `TEXT` | Descripción libre de qué falló |
| `reportedBy` | `VARCHAR(100)` | Nombre del operario que reporta (opcional) |
| `status` | `RepairOrderStatus` | Estado actual de la orden |
| `createdByUser` | FK → `User` | Usuario que creó la orden (control de ownership) |
| `deleted` | `Boolean` | Soft-delete |
| `tenantId` | `Long` | Multi-tenancy |

**Estado por defecto al crear:** `PENDIENTE`

### Nuevo enum: `RepairOrderStatus`

```
PENDIENTE  →  EN_PROCESO  →  COMPLETADA
```

### Modificación a la entidad `Repair` existente

Agregar campo nullable:

| Campo nuevo | Tipo | Descripción |
|---|---|---|
| `repairOrder` | FK → `RepairOrder` (nullable) | Presente solo si la reparación se originó desde una orden |

---

## DTOs

### `RepairOrderRequestDTO`
```java
public record RepairOrderRequestDTO(
    @NotNull LocalDate date,
    @NotNull @Positive Long vehicleId,
    @NotBlank @Size(max = 1000) String description,
    @Size(max = 100) String reportedBy
) {}
```

### `RepairOrderResponseDTO`
```java
public record RepairOrderResponseDTO(
    Long id,
    LocalDate date,
    Long vehicleId,
    String vehicleLicensePlate,
    String description,
    String reportedBy,
    RepairOrderStatus status,
    String createdByUserName   // firstName + lastName del creador
) {}
```

### `RepairOrderStatusDTO` *(para PATCH de estado)*
```java
public record RepairOrderStatusDTO(
    @NotNull RepairOrderStatus status  // Solo acepta EN_PROCESO
) {}
```

### `RepairOrderCompleteDTO` *(para POST de completar)*
```java
public record RepairOrderCompleteDTO(
    @DecimalMin("0.01") BigDecimal cost,
    @Size(max = 1000) String description,
    @Size(max = 100) String employee,
    @Positive Long supplierId,
    @NotEmpty List<String> repairTypes
) {}
```

### Modificación a `RepairResponseDTO` existente

Agregar campo al record existente:
```java
RepairOrderResponseDTO repairOrder  // null si no vino de una orden
```

---

## API — `RepairOrderController` (`/api/v1/repair-orders`)

| Método | Ruta | Permiso requerido | Descripción |
|---|---|---|---|
| `POST` | `/` | `REPAIR_ORDER_CREATE` | Crea una nueva orden. Estado inicial: `PENDIENTE`. Vincula al usuario autenticado. |
| `GET` | `/mine` | `REPAIR_ORDER_CREATE` | Lista solo las órdenes del usuario autenticado (excluye `COMPLETADA`). |
| `GET` | `/` | `REPAIR_ORDER_READ` | Lista todas las órdenes del tenant con filtros. |
| `GET` | `/{id}` | `REPAIR_ORDER_CREATE` | Devuelve una orden. Valida ownership salvo que el usuario tenga `REPAIR_ORDER_READ`. |
| `PATCH` | `/{id}` | `REPAIR_ORDER_CREATE` | Edita descripción/fecha. Solo si `PENDIENTE`. Valida ownership. |
| `DELETE` | `/{id}` | `REPAIR_ORDER_CREATE` | Soft-delete. Solo si `PENDIENTE`. Valida ownership. |
| `PATCH` | `/{id}/status` | `REPAIR_ORDER_WRITE` | Transición `PENDIENTE → EN_PROCESO`. |
| `POST` | `/{id}/complete` | `REPAIR_ORDER_WRITE` | Crea la `Repair` vinculada y cierra la orden en `COMPLETADA`. |

### Lógica del endpoint `POST /{id}/complete`

```
1. Validar que la orden exista y esté en estado EN_PROCESO.
2. Crear entidad Repair:
     - date        ← orden.date
     - vehicle     ← orden.vehicle
     - description ← dto.description ?? orden.description
     - cost        ← dto.cost
     - employee    ← dto.employee
     - supplier    ← resolver por dto.supplierId (si presente)
     - repairTypes ← mapear strings → RepairType
     - repairOrder ← referencia a la orden original
3. Persistir Repair.
4. Actualizar orden: status → COMPLETADA.
5. Retornar RepairResponseDTO de la Repair creada.
```

---

## Permisos nuevos

| Nombre | Descripción |
|---|---|
| `REPAIR_ORDER_CREATE` | Registrar nuevas órdenes y gestionar las propias (ver, editar, eliminar si `PENDIENTE`) |
| `REPAIR_ORDER_READ` | Ver todas las órdenes del tenant (personal de taller) |
| `REPAIR_ORDER_WRITE` | Cambiar estado y completar órdenes (personal de taller) |

---

## Migración de base de datos — `V20`

```sql
-- V20__add_repair_orders.sql

CREATE TABLE repair_orders (
    id                  BIGSERIAL PRIMARY KEY,
    vehicle_id          BIGINT NOT NULL REFERENCES vehicles(id),
    date                DATE NOT NULL,
    description         TEXT NOT NULL,
    reported_by         VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_by_user_id  BIGINT REFERENCES users(id),
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id)
);

ALTER TABLE repairs
    ADD COLUMN repair_order_id BIGINT REFERENCES repair_orders(id);

INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description) VALUES
    ('REPAIR_ORDER_CREATE', 'Repair Orders', 'vehicles',
     'Create and manage own repair orders',
     'Crear Órdenes de Reparación',
     'Permite registrar nuevas órdenes y gestionar las propias'),
    ('REPAIR_ORDER_READ', 'Repair Orders', 'vehicles',
     'Read all repair orders',
     'Ver todas las Órdenes de Reparación',
     'Permite ver todas las órdenes del tenant'),
    ('REPAIR_ORDER_WRITE', 'Repair Orders', 'vehicles',
     'Manage repair order status and complete orders',
     'Gestionar Órdenes de Reparación',
     'Permite cambiar estado y completar órdenes');
```

---

## Frontend

### Estructura de carpetas nueva

```
domains/inventory/
├── repair/                          (existente — sin cambios estructurales)
└── repair-order/                    (nuevo)
    ├── services/
    │   ├── repair-order.service.ts         — HTTP client
    │   └── repair-order-form.service.ts    — coordinación entre componentes
    ├── repair-order-page/                  — contenedor principal
    ├── repair-order-table/                 — vista personal de taller
    ├── repair-order-my-list/               — vista operario de campo
    ├── repair-order-form/                  — formulario simple (nueva orden)
    └── repair-order-complete-form/         — formulario completo al finalizar
```

### Modelo frontend

```typescript
// shared/models/repair-order.model.ts

export interface RepairOrder {
  id?: number;
  date: string;
  vehicleId: number;
  vehicleLicensePlate?: string;
  description: string;
  reportedBy?: string;
  status: RepairOrderStatus;
  createdByUserName?: string;
}

export enum RepairOrderStatus {
  PENDIENTE  = 'PENDIENTE',
  EN_PROCESO = 'EN_PROCESO',
  COMPLETADA = 'COMPLETADA'
}
```

### `repair-order-page` — Lógica de permisos

La página detecta el perfil del usuario y renderiza la vista correspondiente:

```typescript
readonly showWorkshopView = computed(() => this.authService.hasPermission('REPAIR_ORDER_READ'));
readonly showMyOrdersView = computed(() => this.authService.hasPermission('REPAIR_ORDER_CREATE'));
```

| Situación | Vista renderizada |
|---|---|
| Usuario con `REPAIR_ORDER_READ` | `repair-order-table` (vista taller completa) |
| Usuario solo con `REPAIR_ORDER_CREATE` | `repair-order-my-list` (solo propias) |

### `repair-order-table` — Vista personal de taller

- **Columnas:** Patente, Descripción, Fecha, Reportado por, Estado
- **Filtros:** Estado (select), Patente (text), Fecha desde / hasta
- **Filtro por defecto:** `status = PENDIENTE, EN_PROCESO`
- **Chips de estado:**
  - `PENDIENTE` → gris
  - `EN_PROCESO` → amarillo
  - `COMPLETADA` → verde
- **Acciones por fila:**
  - Si `PENDIENTE` → botón **"Iniciar"** → `PATCH /{id}/status {status: 'EN_PROCESO'}`
  - Si `EN_PROCESO` → botón **"Finalizar"** → abre `repair-order-complete-form`

### `repair-order-my-list` — Vista operario de campo

- Lista las propias órdenes vía `GET /mine` (excluye `COMPLETADA`)
- **Columnas:** Patente, Descripción, Fecha, Estado (solo lectura)
- **Acciones por fila:** Editar y Eliminar (solo si `PENDIENTE`)
- Botón **"+ Nueva Orden"** → abre `repair-order-form`

### `repair-order-form` — Nueva orden (formulario simple)

| Campo | Tipo | Obligatorio |
|---|---|---|
| Fecha | date | Sí |
| Vehículo | autocomplete | Sí |
| Descripción de la falla | textarea | Sí |
| Reportado por | text | No (puede prellenarse con el nombre del usuario logueado) |

### `repair-order-complete-form` — Finalización (formulario completo)

**Encabezado (solo lectura):** datos de la orden original — vehículo, fecha, descripción reportada.

**Campos editables:**

| Campo | Obligatorio |
|---|---|
| Tipos de reparación (multi-select) | Sí |
| Mecánico (text) | No |
| Proveedor (lookup) | No |
| Costo | No |
| Descripción técnica del trabajo realizado | No |

Al guardar → `POST /repair-orders/{id}/complete` → backend crea la `Repair` y cierra la orden.

### Modificación a `repair-detail` existente

Agregar sección condicional al final de la vista de detalle:

```html
@if (repair().repairOrder) {
  <section>
    <h4>Orden de Reparación Original</h4>
    <p>Fecha de reporte: {{ repair().repairOrder.date }}</p>
    <p>Reportado por:    {{ repair().repairOrder.reportedBy }}</p>
    <p>Descripción:      {{ repair().repairOrder.description }}</p>
  </section>
}
```

### Nueva ruta en `inventory.routes.ts`

```typescript
{
  path: 'ordenes-reparacion',
  loadComponent: () =>
    import('./repair-order/repair-order-page/repair-order-page')
      .then(m => m.RepairOrderPage),
  canActivate: [RoleGuard],
  data: { requiredPermissions: ['REPAIR_ORDER_CREATE', 'REPAIR_ORDER_READ'] }
  // el guard acepta si el usuario tiene AL MENOS UNO de los dos permisos
}
```

---

## Resumen de cambios por capa

| Capa | Archivos nuevos | Archivos modificados |
|---|---|---|
| **DB Migration** | `V20__add_repair_orders.sql` | — |
| **Enum** | `RepairOrderStatus.java` | — |
| **Entity** | `RepairOrder.java` | `Repair.java` (+1 campo `repairOrder`) |
| **DTO** | `RepairOrderRequestDTO`, `RepairOrderResponseDTO`, `RepairOrderStatusDTO`, `RepairOrderCompleteDTO` | `RepairResponseDTO` (+1 campo) |
| **Mapper** | `RepairOrderMapper.java` | `RepairMapper.java` (+1 mapping) |
| **Repository** | `RepairOrderRepository.java` | — |
| **Service** | `IRepairOrderService.java`, `RepairOrderService.java` | — |
| **Controller** | `RepairOrderController.java` | — |
| **Frontend model** | `repair-order.model.ts` | `mechanic.model.ts` (+campo `repairOrder` en `Repair`) |
| **Frontend service** | `repair-order.service.ts`, `repair-order-form.service.ts` | — |
| **Frontend components** | `repair-order-page`, `repair-order-table`, `repair-order-my-list`, `repair-order-form`, `repair-order-complete-form` | `repair-detail` (+sección origen), `inventory.routes.ts` (+ruta) |
