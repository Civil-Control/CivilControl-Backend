# Feature 5 — Panel de Trabajo Interactivo y Semáforo de Órdenes de Reparación

## Contexto

El módulo de órdenes de reparación (Feature 1) gestiona el flujo PENDIENTE → EN_PROCESO → COMPLETADA. Sin embargo, el personal de taller necesitaba una vista operativa en tiempo real que mostrara de un vistazo el estado de todos los trabajos activos, sin tener que navegar por la tabla convencional.

Esta feature agrega dos capacidades:

1. **Panel de Trabajo (Workshop Board)** — Vista tipo Kanban de dos columnas (Pendientes | En Proceso) que muestra tarjetas con los datos clave de cada orden. Se auto-refresca cada 30 segundos y muestra un reloj en vivo. Es una pantalla pensada para un monitor en el taller.
2. **Semáforo en tabla** — Indicadores de color (rojo / amarillo / verde) aplicados a cada fila de la tabla de órdenes según su estado. Soporta un modo de intensidad configurable, persistido en `localStorage`.

---

## 1. Backend — Endpoints Reutilizados

El panel de trabajo no requiere endpoints nuevos. Reutiliza los existentes del módulo de órdenes de reparación:

| Método | Ruta | Uso en el panel |
|---|---|---|
| `GET` | `/api/v1/repair-orders` | Carga todas las órdenes (filtradas por estado PENDIENTE/EN_PROCESO) |
| `PATCH` | `/api/v1/repair-orders/{id}/status` | Botón "Iniciar" → cambia PENDIENTE a EN_PROCESO |
| `POST` | `/api/v1/repair-orders/{id}/complete` | Botón "Finalizar" → abre formulario de completar orden |

### 1.1 Enum: `RepairOrderStatus`

```java
public enum RepairOrderStatus {
    PENDIENTE,
    EN_PROCESO,
    COMPLETADA
}
```

Ubicación: `PSG.backEnd.model.enums.vehicle.RepairOrderStatus`

### 1.2 Regla de transición de estados

El servicio `RepairOrderService.changeStatus()` sólo permite la transición `PENDIENTE → EN_PROCESO`. La transición a `COMPLETADA` se realiza exclusivamente a través de `completeRepairOrder()`, que además crea la entidad `Repair` vinculada.

---

## 2. Frontend — Panel de Trabajo (Workshop Board)

### 2.1 Ubicación

```
domains/inventory/repair-order/workshop-board/
├── workshop-board.ts
├── workshop-board.html
└── workshop-board.scss
```

### 2.2 Componente: `WorkshopBoard`

**Archivo:** `workshop-board.ts`

Componente standalone que presenta un tablero Kanban de dos columnas:

| Columna | Contenido |
|---|---|
| **Pendientes** | Órdenes con `status = PENDIENTE` |
| **En Proceso** | Órdenes con `status = EN_PROCESO` |

**Características:**
- **Auto-refresh:** Cada 30 segundos recarga las órdenes activas automáticamente.
- **Reloj en vivo:** Muestra la hora actual del sistema, actualizada cada segundo.
- **Tarjetas de orden:** Cada tarjeta muestra: patente del vehículo, lista de ítems reportados, descripción, fecha de reporte y nombre del reportante.
- **Acciones por tarjeta:**
  - En columna Pendientes: botón **"Iniciar"** → cambia estado a EN_PROCESO vía `changeStatus()`.
  - En columna En Proceso: botón **"Finalizar"** → abre el formulario de completar orden (`RepairOrderCompleteForm`).

### 2.3 Ruta

**Archivo:** `domains/inventory/inventory.routes.ts`

```typescript
{ path: 'panel-taller', component: WorkshopBoard }
```

Ruta completa: `/taller/panel-taller`

**Pantalla sin header:** La ruta está incluida en el array `HEADERLESS_ROUTES` de `app.ts`, por lo que se renderiza a pantalla completa sin la barra de navegación superior — ideal para monitores de taller.

---

## 3. Frontend — Semáforo en Tabla de Órdenes

### 3.1 Ubicación

```
domains/inventory/repair-order/repair-order-table/
├── repair-order-table.ts
├── repair-order-table.html
└── repair-order-table.scss
```

### 3.2 Componente: `RepairOrderTable`

**Archivo:** `repair-order-table.ts`

Extiende la tabla estándar con dos funciones de coloreo por fila:

#### `statusRowClassFn(row)`

Asigna clases CSS según el estado de la orden:

| Estado | Clase CSS |
|---|---|
| `PENDIENTE` | `row-pendiente` |
| `EN_PROCESO` | `row-en-proceso` |
| `COMPLETADA` | `row-completada` |

Posee variantes **intensas** (ej. `row-pendiente-intense`) activadas según el modo seleccionado.

#### `statusRowAccentFn(row)`

Retorna un color hexadecimal para el borde lateral de la fila:

| Estado | Color |
|---|---|
| `PENDIENTE` | `#ef4444` (rojo) |
| `EN_PROCESO` | `#eab308` (amarillo) |
| `COMPLETADA` | `#22c55e` (verde) |

#### Modo de intensidad

El componente expone un signal `intenseMode` que alterna entre modo suave y modo intenso. La preferencia se persiste en `localStorage` con la clave `ro_color_mode`.

### 3.3 Mapeo de colores en el modelo

**Archivo:** `shared/models/repair-order.model.ts`

```typescript
export const RepairOrderStatusColors: Record<RepairOrderStatus, string> = {
  [RepairOrderStatus.PENDIENTE]: 'red',
  [RepairOrderStatus.EN_PROCESO]: 'yellow',
  [RepairOrderStatus.COMPLETADA]: 'green',
};

export const RepairOrderStatusLabels: Record<RepairOrderStatus, string> = {
  [RepairOrderStatus.PENDIENTE]: 'Pendiente',
  [RepairOrderStatus.EN_PROCESO]: 'En Proceso',
  [RepairOrderStatus.COMPLETADA]: 'Completada',
};
```

### 3.4 Botón de acceso al panel

La tabla incluye un botón en la cabecera: **"Panel de Trabajos"** que navega a `/taller/panel-taller`, configurado a través de `boardHeaderButtons`.

---

## 4. Permisos

Reutiliza los permisos existentes del módulo de órdenes de reparación:

| Constante | Uso |
|---|---|
| `REPAIR_ORDER_CREATE` | Crear órdenes, ver "Mis órdenes" |
| `REPAIR_ORDER_READ` | Ver tabla completa con semáforo |
| `REPAIR_ORDER_WRITE` | Cambiar estado, completar orden, acceder al panel |

---

## 5. Migraciones

No requiere migraciones propias. Reutiliza las tablas y permisos creados en Feature 1 (Órdenes de Reparación):

- `V24__add_repair_orders.sql`
- `V25__repair_orders_compensation.sql`
- `V26__assign_repair_order_permissions_to_system_roles.sql`
- `V27__fix_repair_order_permissions.sql`
- `V28__fix_repair_order_permission_translations.sql`
- `V38__add_repair_order_items.sql`

---
