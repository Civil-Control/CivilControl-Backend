# Feature 19 — Sistema de Órdenes de Compra

## Contexto

El sistema actualmente no contempla un mecanismo formal para registrar y gestionar solicitudes de materiales, herramientas, ropa de trabajo, insumos y otros elementos necesarios para las operaciones. Los operarios y encargados deben gestionar estas necesidades fuera del sistema, sin trazabilidad ni flujo de aprobación.

Esta feature implementa un módulo de **Órdenes de Compra** que permite:

- **Operarios/encargados:** registrar una necesidad de compra detallando los ítems requeridos.
- **Supervisores/compradores:** revisar, aprobar y marcar como compradas las órdenes.
- **Contabilidad/administración:** vincular la orden comprada a un comprobante fiscal real (`TransactionalDocument`) una vez realizada la compra.

---

## Flujo de trabajo

```
Operario/Encargado              Supervisor/Comprador
        │                               │
        ▼                               │
Crea Orden de Compra           Ve listado de órdenes
  · Fecha                      (PENDIENTE / EN_REVISION / APROBADA)
  · Categoría                          │
  · Prioridad                   ┌──────┼──────────┐
  · Descripción del motivo  [En Revisión]  [Aprobar]  [Marcar Comprada]
  · Lista de ítems solicitados   │         │            │
  · Pedido por (texto)       EN_REVISION APROBADA    COMPRADA
  · Monto estimado (opc.)                           + vincular factura
                                                    (TransactionalDocument)
```

### Estados y transiciones válidas

```
PENDIENTE → EN_REVISION → APROBADA → COMPRADA
              ↓
           PENDIENTE     (devolver para corrección)
              ↑
           EN_REVISION   (regresar desde APROBADA)
```

> **Nota de diseño:** Las transiciones de retroceso (EN_REVISION → PENDIENTE, APROBADA → EN_REVISION) permiten al supervisor devolver una orden para corrección sin eliminarla.

---

## Modelo de datos

### Nueva entidad: `PurchaseOrder`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK autoincremental |
| `date` | `LocalDate` | Fecha de la solicitud |
| `category` | `PurchaseOrderCategory` | Categoría del pedido (enum) |
| `description` | `TEXT` | Descripción del motivo o necesidad |
| `items` | `List<String>` @ElementCollection | Lista de ítems solicitados (texto libre) |
| `requestedBy` | `VARCHAR(100)` nullable | Nombre del solicitante (puede diferir del usuario autenticado) |
| `estimatedAmount` | `DECIMAL(12,2)` nullable | Monto estimado (opcional) |
| `priority` | `PurchaseOrderPriority` | Prioridad: BAJA, MEDIA, ALTA |
| `status` | `PurchaseOrderStatus` | Estado actual de la orden |
| `transactionalDocument` | FK → `TransactionalDocument` nullable | Factura vinculada una vez comprada |
| `createdByUser` | FK → `User` | Usuario que creó la orden (control de ownership) |
| `deleted` | `Boolean` | Soft-delete |
| `tenantId` | `Long` | Multi-tenancy (heredado de TenantEntity) |

**Estado por defecto al crear:** `PENDIENTE`

**Ubicación del archivo:** `model/entity/PurchaseOrder.java` (dominio raíz, no en `/vehicle/` ya que es transversal a la empresa)

### Nuevo enum: `PurchaseOrderStatus`

```java
// model/enums/PurchaseOrderStatus.java
public enum PurchaseOrderStatus {
    PENDIENTE,
    EN_REVISION,
    APROBADA,
    COMPRADA
}
```

### Nuevo enum: `PurchaseOrderCategory`

```java
// model/enums/PurchaseOrderCategory.java
public enum PurchaseOrderCategory {
    MATERIALES,
    ROPA_EPP,
    HERRAMIENTAS,
    INSUMOS,
    OTRO
}
```

### Nuevo enum: `PurchaseOrderPriority`

```java
// model/enums/PurchaseOrderPriority.java
public enum PurchaseOrderPriority {
    BAJA,
    MEDIA,
    ALTA
}
```

### `TransactionalDocument` — sin modificaciones a la entidad

La FK `transactional_document_id` vive en la tabla `purchase_orders` (lado de `PurchaseOrder`). Para consultar desde el comprobante qué órdenes están vinculadas, se usa un filtro `transactionalDocumentId` en el endpoint GET de purchase orders. No se modifica la entidad `TransactionalDocument`.

---

## Implementación Java — Entity

```java
// model/entity/PurchaseOrder.java
@Entity
@Table(name = "purchase_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class PurchaseOrder extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseOrderCategory category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @ElementCollection
    @CollectionTable(name = "purchase_order_items", joinColumns = @JoinColumn(name = "purchase_order_id"))
    @Column(name = "item", nullable = false)
    @Builder.Default
    private List<String> items = new ArrayList<>();

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "estimated_amount", precision = 12, scale = 2)
    private BigDecimal estimatedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PurchaseOrderPriority priority = PurchaseOrderPriority.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactional_document_id")
    private TransactionalDocument transactionalDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
```

---

## DTOs

### `PurchaseOrderRequestDTO`

```java
// model/dto/purchaseOrder/PurchaseOrderRequestDTO.java
public record PurchaseOrderRequestDTO(
    @NotNull(message = "{purchaseOrder.date.required}")
    @PastOrPresent
    LocalDate date,

    @NotNull(message = "{purchaseOrder.category.required}")
    PurchaseOrderCategory category,

    @NotBlank(message = "{purchaseOrder.description.required}")
    @Size(max = 1000)
    String description,

    @NotEmpty(message = "{purchaseOrder.items.required}")
    List<@NotBlank @Size(max = 200) String> items,

    @Size(max = 100)
    String requestedBy,

    @Positive
    @DecimalMax("9999999999.99")
    BigDecimal estimatedAmount,

    @NotNull(message = "{purchaseOrder.priority.required}")
    PurchaseOrderPriority priority
) {}
```

### `PurchaseOrderResponseDTO`

```java
// model/dto/purchaseOrder/PurchaseOrderResponseDTO.java
public record PurchaseOrderResponseDTO(
    Long id,
    LocalDate date,
    PurchaseOrderCategory category,
    String description,
    List<String> items,
    String requestedBy,
    BigDecimal estimatedAmount,
    PurchaseOrderPriority priority,
    PurchaseOrderStatus status,
    TransactionalDocumentSummaryDTO transactionalDocument,
    String createdByUserName
) {}
```

> `TransactionalDocumentSummaryDTO` ya existe desde Feature 2. Reutilizar sin crear uno nuevo.

### `PurchaseOrderStatusDTO`

```java
// model/dto/purchaseOrder/PurchaseOrderStatusDTO.java
public record PurchaseOrderStatusDTO(
    @NotNull PurchaseOrderStatus status
) {}
```

### `PurchaseOrderLinkDocumentDTO`

```java
// model/dto/purchaseOrder/PurchaseOrderLinkDocumentDTO.java
public record PurchaseOrderLinkDocumentDTO(
    @NotNull @Positive Long transactionalDocumentId
) {}
```

### `PurchaseOrderFilterDTO`

```java
// model/dto/purchaseOrder/PurchaseOrderFilterDTO.java
public record PurchaseOrderFilterDTO(
    LocalDate dateFrom,
    LocalDate dateTo,
    PurchaseOrderStatus status,
    PurchaseOrderCategory category,
    PurchaseOrderPriority priority,
    Long transactionalDocumentId,
    String search
) {}
```

---

## API — `PurchaseOrderController` (`/api/v1/purchase-orders`)

| Método | Ruta | Permiso requerido | Descripción |
|---|---|---|---|
| `POST` | `/` | `PURCHASE_ORDER_CREATE` | Crea nueva orden. Estado inicial: PENDIENTE. Vincula al usuario autenticado. |
| `GET` | `/mine` | `PURCHASE_ORDER_CREATE` | Lista solo las órdenes del usuario autenticado. Excluye COMPRADA. |
| `GET` | `/` | `PURCHASE_ORDER_READ` | Lista todas las órdenes del tenant con filtros y paginación. |
| `GET` | `/{id}` | `PURCHASE_ORDER_CREATE` | Obtiene una orden. Valida ownership salvo que tenga `PURCHASE_ORDER_READ`. |
| `PATCH` | `/{id}` | `PURCHASE_ORDER_CREATE` | Edita campos. Solo si estado es PENDIENTE. Valida ownership. |
| `DELETE` | `/{id}` | `PURCHASE_ORDER_CREATE` | Soft-delete. Solo si estado es PENDIENTE. Valida ownership. |
| `PATCH` | `/{id}/status` | `PURCHASE_ORDER_WRITE` | Avanza o retrocede el estado según transiciones válidas. |
| `PATCH` | `/{id}/link-document` | `PURCHASE_ORDER_WRITE` | Vincula un `TransactionalDocument`. Solo si estado es COMPRADA. |

### Paginación y sorting

Mismo patrón que `RepairOrderController`:
- Parámetros: `page` (default 0), `size` (default 10), `sortBy` (default `date`), `sortDir` (default `desc`)
- Campos de sort permitidos: `date`, `status`, `priority`, `category`, `requestedBy`

### Lógica del endpoint `PATCH /{id}/status`

```
Transiciones válidas:
  PENDIENTE   → EN_REVISION
  EN_REVISION → APROBADA
  EN_REVISION → PENDIENTE    (devolver)
  APROBADA    → COMPRADA
  APROBADA    → EN_REVISION  (regresar)
  COMPRADA    → (ninguna — estado final)

Si la transición solicitada no está en el set válido → lanzar IllegalStateException (HTTP 400).
```

### Lógica del endpoint `PATCH /{id}/link-document`

```
1. Cargar la PurchaseOrder. Lanzar EntityNotFoundException si no existe o deleted.
2. Validar que status == COMPRADA. Si no → HTTP 400.
3. Resolver TransactionalDocument por dto.transactionalDocumentId y mismo tenantId.
   Si no existe → lanzar EntityNotFoundException.
4. order.setTransactionalDocument(doc).
5. Persistir.
6. Retornar PurchaseOrderResponseDTO actualizado.
```

---

## Permisos nuevos

| Nombre | Descripción |
|---|---|
| `PURCHASE_ORDER_CREATE` | Registrar nuevas órdenes y gestionar las propias (ver, editar, eliminar si PENDIENTE) |
| `PURCHASE_ORDER_READ` | Ver todas las órdenes del tenant con filtros completos |
| `PURCHASE_ORDER_WRITE` | Cambiar estado de órdenes y vincular comprobantes fiscales |

### Cambio en `AppPermissions.java`

Agregar las tres constantes al final de la clase:
```java
public static final String PURCHASE_ORDER_CREATE = "PURCHASE_ORDER_CREATE";
public static final String PURCHASE_ORDER_READ   = "PURCHASE_ORDER_READ";
public static final String PURCHASE_ORDER_WRITE  = "PURCHASE_ORDER_WRITE";
```

---

## Mapper

```java
// model/mapper/PurchaseOrderMapper.java
@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    PurchaseOrder toEntity(PurchaseOrderRequestDTO dto);

    @Mapping(
        target = "createdByUserName",
        expression = "java(entity.getCreatedByUser() != null ? entity.getCreatedByUser().getFirstName() + ' ' + entity.getCreatedByUser().getLastName() : null)"
    )
    @Mapping(target = "transactionalDocument", source = "transactionalDocument", qualifiedByName = "toSummaryDTO")
    PurchaseOrderResponseDTO toResponseDto(PurchaseOrder entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    void partialUpdate(@MappingTarget PurchaseOrder entity, PurchaseOrderRequestDTO dto);

    @Named("toSummaryDTO")
    default TransactionalDocumentSummaryDTO toSummaryDTO(TransactionalDocument doc) {
        if (doc == null) return null;
        return new TransactionalDocumentSummaryDTO(
            doc.getId(),
            doc.getDocumentType() != null ? doc.getDocumentType().name() : null,
            doc.getBranchCode(),
            doc.getDocumentNumber(),
            doc.getSupplier() != null ? doc.getSupplier().getLegalName() : null,
            doc.getTotal(),
            doc.getDate()
        );
    }
}
```

---

## Repository

```java
// repository/PurchaseOrderRepository.java
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
        SELECT po FROM PurchaseOrder po
        LEFT JOIN FETCH po.createdByUser
        WHERE po.deleted = false
          AND (:#{#f.status}   IS NULL OR po.status   = :#{#f.status})
          AND (:#{#f.category} IS NULL OR po.category = :#{#f.category})
          AND (:#{#f.priority} IS NULL OR po.priority = :#{#f.priority})
          AND (:#{#f.dateFrom} IS NULL OR po.date >= :#{#f.dateFrom})
          AND (:#{#f.dateTo}   IS NULL OR po.date <= :#{#f.dateTo})
          AND (:#{#f.transactionalDocumentId} IS NULL
               OR po.transactionalDocument.id = :#{#f.transactionalDocumentId})
          AND (:#{#f.search} IS NULL
               OR LOWER(po.description)  LIKE LOWER(CONCAT('%', :#{#f.search}, '%'))
               OR LOWER(po.requestedBy)  LIKE LOWER(CONCAT('%', :#{#f.search}, '%')))
        """)
    Page<PurchaseOrder> findAllWithFilters(
        @Param("f") PurchaseOrderFilterDTO f,
        Pageable pageable
    );

    @Query("""
        SELECT po FROM PurchaseOrder po
        WHERE po.deleted = false
          AND po.createdByUser.id = :userId
          AND po.status != PSG.backEnd.model.enums.PurchaseOrderStatus.COMPRADA
          AND (:#{#f.status}   IS NULL OR po.status   = :#{#f.status})
          AND (:#{#f.category} IS NULL OR po.category = :#{#f.category})
          AND (:#{#f.search} IS NULL
               OR LOWER(po.description) LIKE LOWER(CONCAT('%', :#{#f.search}, '%'))
               OR LOWER(po.requestedBy) LIKE LOWER(CONCAT('%', :#{#f.search}, '%')))
        """)
    Page<PurchaseOrder> findAllByCreatedByUserWithFilters(
        @Param("userId") Long userId,
        @Param("f") PurchaseOrderFilterDTO f,
        Pageable pageable
    );
}
```

---

## Service

### Interface: `IPurchaseOrderService`

```java
// service/port/IPurchaseOrderService.java
public interface IPurchaseOrderService {
    PurchaseOrderResponseDTO createPurchaseOrder(PurchaseOrderRequestDTO dto);
    PurchaseOrderResponseDTO getPurchaseOrderById(Long id);
    Page<PurchaseOrderResponseDTO> getAllPurchaseOrders(PurchaseOrderFilterDTO filterDTO, Pageable pageable);
    Page<PurchaseOrderResponseDTO> getMyPurchaseOrders(PurchaseOrderFilterDTO filterDTO, Pageable pageable);
    PurchaseOrderResponseDTO updatePurchaseOrder(Long id, PurchaseOrderRequestDTO dto);
    void deletePurchaseOrder(Long id);
    PurchaseOrderResponseDTO changeStatus(Long id, PurchaseOrderStatusDTO statusDTO);
    PurchaseOrderResponseDTO linkTransactionalDocument(Long id, PurchaseOrderLinkDocumentDTO dto);
}
```

### Implementation: `PurchaseOrderService`

Seguir el patrón exacto de `RepairOrderService`. Los métodos clave:

**`createPurchaseOrder()`:**
```java
PurchaseOrder order = mapper.toEntity(dto);
order.setStatus(PurchaseOrderStatus.PENDIENTE);
order.setCreatedByUser(getCurrentUser());
return mapper.toResponseDto(repository.save(order));
```

**`updatePurchaseOrder()`:** Validar que `status == PENDIENTE` y que el usuario sea el owner (o tenga READ). Usar `mapper.partialUpdate()`.

**`deletePurchaseOrder()`:** Validar `status == PENDIENTE` y ownership. Setear `deleted = true`.

**`changeStatus()`:**
```java
// Definir transiciones válidas como Map o switch expression
Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> validTransitions = Map.of(
    PurchaseOrderStatus.PENDIENTE,   Set.of(PurchaseOrderStatus.EN_REVISION),
    PurchaseOrderStatus.EN_REVISION, Set.of(PurchaseOrderStatus.APROBADA, PurchaseOrderStatus.PENDIENTE),
    PurchaseOrderStatus.APROBADA,    Set.of(PurchaseOrderStatus.COMPRADA, PurchaseOrderStatus.EN_REVISION),
    PurchaseOrderStatus.COMPRADA,    Set.of()
);
if (!validTransitions.get(order.getStatus()).contains(dto.status())) {
    throw new IllegalStateException("Transición de estado no válida");
}
order.setStatus(dto.status());
return mapper.toResponseDto(repository.save(order));
```

**`linkTransactionalDocument()`:** Ver lógica del endpoint `PATCH /{id}/link-document` descrita arriba.

**`getCurrentUser()`:** mismo helper que `RepairOrderService` — obtener de `SecurityContextHolder`.

**Ownership validation helper:**
```java
private void validateOwnership(PurchaseOrder order) {
    User currentUser = getCurrentUser();
    boolean hasReadPermission = currentUser.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals(AppPermissions.PURCHASE_ORDER_READ));
    if (!hasReadPermission && !order.getCreatedByUser().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException("No tiene permisos para acceder a esta orden");
    }
}
```

---

## Migración de base de datos

> **IMPORTANTE:** Verificar el número de migración máximo en `src/main/resources/db/migration/` y usar el siguiente en secuencia.

```sql
-- V{N}__add_purchase_orders.sql

CREATE TABLE purchase_orders (
    id                          BIGSERIAL PRIMARY KEY,
    date                        DATE NOT NULL,
    category                    VARCHAR(30) NOT NULL,
    description                 TEXT NOT NULL,
    requested_by                VARCHAR(100),
    estimated_amount            DECIMAL(12, 2),
    priority                    VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    transactional_document_id   BIGINT REFERENCES transactional_documents(id),
    created_by_user_id          BIGINT REFERENCES users(id),
    deleted                     BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id                   BIGINT NOT NULL REFERENCES tenants(id)
);

CREATE TABLE purchase_order_items (
    purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    item                VARCHAR(200) NOT NULL
);

INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description) VALUES
    ('PURCHASE_ORDER_CREATE', 'Purchase Orders', 'administration',
     'Create and manage own purchase orders',
     'Crear Órdenes de Compra',
     'Permite registrar nuevas órdenes y gestionar las propias'),
    ('PURCHASE_ORDER_READ', 'Purchase Orders', 'administration',
     'Read all purchase orders of the tenant',
     'Ver todas las Órdenes de Compra',
     'Permite ver todas las órdenes con filtros completos'),
    ('PURCHASE_ORDER_WRITE', 'Purchase Orders', 'administration',
     'Manage purchase order status and link fiscal documents',
     'Gestionar Órdenes de Compra',
     'Permite cambiar estado y vincular comprobantes fiscales');
```

---

## Frontend

### Estructura de carpetas nueva

> **Verificar** en el proyecto si existe `domains/administration/`. Si no existe, usar `domains/inventory/` siguiendo el patrón de las repair orders. Adaptar la ruta del router al archivo de rutas correcto (`administration.routes.ts` o `inventory.routes.ts`).

```
domains/administration/
└── purchase-order/                         (nuevo)
    ├── services/
    │   ├── purchase-order.service.ts       — HTTP client
    │   └── purchase-order-form.service.ts  — coordinación entre componentes
    ├── purchase-order-page/                — contenedor principal (standalone)
    ├── purchase-order-table/               — vista gestión completa (READ)
    ├── purchase-order-my-list/             — vista operario/propias (CREATE)
    └── purchase-order-form/               — formulario único crear/editar
```

### Modelos frontend

```typescript
// shared/models/purchase-order.model.ts

export enum PurchaseOrderStatus {
  PENDIENTE   = 'PENDIENTE',
  EN_REVISION = 'EN_REVISION',
  APROBADA    = 'APROBADA',
  COMPRADA    = 'COMPRADA'
}

export enum PurchaseOrderCategory {
  MATERIALES   = 'MATERIALES',
  ROPA_EPP     = 'ROPA_EPP',
  HERRAMIENTAS = 'HERRAMIENTAS',
  INSUMOS      = 'INSUMOS',
  OTRO         = 'OTRO'
}

export enum PurchaseOrderPriority {
  BAJA  = 'BAJA',
  MEDIA = 'MEDIA',
  ALTA  = 'ALTA'
}

export interface PurchaseOrder {
  id?: number;
  date: string;
  category: PurchaseOrderCategory;
  description: string;
  items: string[];
  requestedBy?: string;
  estimatedAmount?: number | null;
  priority: PurchaseOrderPriority;
  status: PurchaseOrderStatus;
  transactionalDocument?: TransactionalDocumentSummary | null;
  createdByUserName?: string;
}

export interface PurchaseOrderFilters {
  status?: PurchaseOrderStatus | string;
  category?: PurchaseOrderCategory | string;
  priority?: PurchaseOrderPriority | string;
  transactionalDocumentId?: number;
  dateFrom?: string;
  dateTo?: string;
  search?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}
```

### `purchase-order.service.ts`

Espejo exacto del patrón de `RepairOrderService`:

```typescript
@Injectable({ providedIn: 'root' })
export class PurchaseOrderService {
  private baseUrl = `${environment.apiUrl}/purchase-orders`;

  constructor(private http: HttpClient, private toastr: ToastrService) {}

  getAll(pagination: PaginationParams, filters: PurchaseOrderFilters): Observable<Page<PurchaseOrder>>
  getMine(pagination: PaginationParams, filters: PurchaseOrderFilters): Observable<Page<PurchaseOrder>>
  getById(id: number): Observable<PurchaseOrder>
  create(dto: Partial<PurchaseOrder>): Observable<PurchaseOrder>
  update(id: number, dto: Partial<PurchaseOrder>): Observable<PurchaseOrder>
  delete(id: number): Observable<void>
  changeStatus(id: number, status: PurchaseOrderStatus): Observable<PurchaseOrder>
  linkDocument(id: number, transactionalDocumentId: number): Observable<PurchaseOrder>
}
```

Cada método que muta datos emite toastr de éxito/error en `tap` / `catchError`.

### `purchase-order-form.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class PurchaseOrderFormService {
  private _edit$ = new Subject<PurchaseOrder>();
  private _close$ = new Subject<void>();

  edit$  = this._edit$.asObservable();
  close$ = this._close$.asObservable();

  openEdit(order: PurchaseOrder) { this._edit$.next(order); }
  closeForm() { this._close$.next(); }
}
```

### `purchase-order-page` — Lógica de permisos

```typescript
readonly showManagementView = computed(() =>
  this.authService.hasPermission('PURCHASE_ORDER_READ')
);
readonly showMyOrdersView = computed(() =>
  this.authService.hasPermission('PURCHASE_ORDER_CREATE') &&
  !this.authService.hasPermission('PURCHASE_ORDER_READ')
);
readonly canChangeStatus = computed(() =>
  this.authService.hasPermission('PURCHASE_ORDER_WRITE')
);
```

| Situación | Vista renderizada |
|---|---|
| Usuario con `PURCHASE_ORDER_READ` | `purchase-order-table` (gestión completa) |
| Usuario solo con `PURCHASE_ORDER_CREATE` | `purchase-order-my-list` (solo propias) |

### `purchase-order-table` — Vista supervisor/comprador

- **Columnas:** Fecha, Categoría, Prioridad, Descripción, Pedido por, Estado, Factura Vinculada
- **Filtros:** Estado (select), Categoría (select), Prioridad (select), Fecha desde/hasta, Búsqueda libre
- **Filtro por defecto al cargar:** `status = PENDIENTE,EN_REVISION,APROBADA` (excluir COMPRADA del listado inicial)
- **Chips de estado:**
  - `PENDIENTE` → gris
  - `EN_REVISION` → amarillo
  - `APROBADA` → azul
  - `COMPRADA` → verde
- **Chips de prioridad:**
  - `ALTA` → rojo
  - `MEDIA` → naranja
  - `BAJA` → verde claro
- **Acciones por fila (visibles solo con `PURCHASE_ORDER_WRITE`):**
  - Si `PENDIENTE` → botón **"Poner en Revisión"** → `PATCH /{id}/status {status: 'EN_REVISION'}`
  - Si `EN_REVISION` → botón **"Aprobar"** y botón **"Devolver"** → transiciones correspondientes
  - Si `APROBADA` → botón **"Marcar Comprada"** → `PATCH /{id}/status {status: 'COMPRADA'}`
  - Si `COMPRADA` y sin `transactionalDocument` → botón **"Vincular Factura"** → abre modal con `DocumentPickerComponent`

### Vinculación de factura (modal inline en la tabla)

Al hacer clic en **"Vincular Factura"**:
1. Abrir un modal/panel compacto (signal `showLinkModal`, `selectedOrderId`).
2. Renderizar `DocumentPickerComponent` (ya existe de Feature 2 — importar, no recrear).
3. Al emitir `documentSelected` → llamar `purchaseOrderService.linkDocument(orderId, doc.id)`.
4. Al completar → cerrar modal, refrescar fila con los datos actualizados.
5. La celda "Factura Vinculada" muestra `DOC_TYPE BRANCH-NUMBER` con un link al detalle del comprobante.

### `purchase-order-my-list` — Vista operario

- Lista las propias órdenes vía `GET /mine` (excluye COMPRADA del backend)
- **Columnas:** Fecha, Categoría, Ítems (cantidad), Prioridad, Estado
- **Acciones por fila:** Editar (solo si PENDIENTE), Eliminar (solo si PENDIENTE)
- Botón **"+ Nueva Orden"** fijo en la cabecera → abre `purchase-order-form`
- Sin acciones de cambio de estado (ese flujo es exclusivo del supervisor)

### `purchase-order-form` — Formulario único (crear y editar)

| Campo | Tipo UI | Obligatorio | Notas |
|---|---|---|---|
| Fecha | date input | Sí | Default: hoy |
| Categoría | select (enum labels) | Sí | |
| Prioridad | select (enum labels) | Sí | Default: MEDIA |
| Descripción del motivo | textarea | Sí | max 1000 chars |
| Ítems solicitados | tag-input list | Sí (≥1) | Ver patrón abajo |
| Pedido por | text input | No | Pre-llenar con nombre del usuario logueado |
| Monto estimado | number input | No | Prefijo $ |

**Patrón de tag-input para ítems** (mismo espíritu que `@ElementCollection` en RepairOrder):
```typescript
newItem = '';
items: string[] = [];

addItem() {
  const trimmed = this.newItem.trim();
  if (trimmed) { this.items.push(trimmed); this.newItem = ''; }
}

removeItem(index: number) {
  this.items.splice(index, 1);
}
```

En el template: input + botón "Agregar" + lista de chips con botón `×` por ítem.

**En modo edición:** `loadOrder(order: PurchaseOrder)` pre-carga todos los campos. Campos solo disponibles si `status == PENDIENTE` (de lo contrario mostrar en modo lectura).

**Submit payload:**
```typescript
{
  date: this.form.value.date,
  category: this.form.value.category,
  description: this.form.value.description,
  items: this.items,
  requestedBy: this.form.value.requestedBy || null,
  estimatedAmount: this.form.value.estimatedAmount || null,
  priority: this.form.value.priority
}
```

---

## Vinculación desde `transactional-document-detail`

Se agrega una nueva sub-sección **"Órdenes de Compra"** dentro de la sección **"Registros Vinculados"** ya existente (implementada en Feature 2):

### Cambio en `transactional-document-detail.ts`

Agregar signals y lógica (mismo patrón que `linkedFuelLoads`, etc.):

```typescript
linkedPurchaseOrders  = signal<PurchaseOrder[]>([]);
loadingPurchaseOrders = signal(false);
purchaseOrdersCount   = computed(() => this.linkedPurchaseOrders().length);
```

En el método `loadLinkedRecords(key: string)`, agregar:
```typescript
case 'purchaseOrders':
  this.loadingPurchaseOrders.set(true);
  this.purchaseOrderService.getAll(
    { size: '100', page: '0' },
    { transactionalDocumentId: this.document().id }
  ).subscribe({
    next: (res: any) => {
      this.linkedPurchaseOrders.set(res?.content ?? []);
      this.loadingPurchaseOrders.set(false);
    }
  });
  break;
```

### Cambio en `transactional-document-detail.html`

Agregar la sub-sección dentro del bloque `.linked-records`:

```html
<div class="linked-records__group">
  <button (click)="toggleSection('purchaseOrders')">
    Órdenes de Compra
    <span class="badge">{{ purchaseOrdersCount() }}</span>
  </button>
  @if (sectionOpen('purchaseOrders')) {
    @if (loadingPurchaseOrders()) { <span>Cargando...</span> }
    @for (po of linkedPurchaseOrders(); track po.id) {
      <div class="linked-record-item">
        <span>{{ po.date }} — {{ po.category }} — {{ po.description | slice:0:60 }}</span>
        <span class="status-chip">{{ po.status }}</span>
      </div>
    }
  }
</div>
```

> No se agrega un botón "Crear orden desde el comprobante" porque el flujo lógico va en el otro sentido: la orden se crea primero, y la factura se vincula después.

---

## Nueva ruta en el router

```typescript
// En administration.routes.ts (o inventory.routes.ts si no existe administration)
{
  path: 'ordenes-compra',
  loadComponent: () =>
    import('./purchase-order/purchase-order-page/purchase-order-page')
      .then(m => m.PurchaseOrderPage),
  canActivate: [RoleGuard],
  data: { requiredPermissions: ['PURCHASE_ORDER_CREATE', 'PURCHASE_ORDER_READ'] }
  // RoleGuard acepta si el usuario tiene AL MENOS UNO de los dos permisos
}
```

---

## Resumen de cambios por capa

| Capa | Archivos nuevos | Archivos modificados |
|---|---|---|
| **DB Migration** | `V{N}__add_purchase_orders.sql` | — |
| **Enum** | `PurchaseOrderStatus.java`, `PurchaseOrderCategory.java`, `PurchaseOrderPriority.java` | — |
| **Entity** | `PurchaseOrder.java` | — |
| **DTO** | `PurchaseOrderRequestDTO.java`, `PurchaseOrderResponseDTO.java`, `PurchaseOrderStatusDTO.java`, `PurchaseOrderLinkDocumentDTO.java`, `PurchaseOrderFilterDTO.java` | — |
| **Mapper** | `PurchaseOrderMapper.java` | — |
| **Repository** | `PurchaseOrderRepository.java` | — |
| **Service** | `IPurchaseOrderService.java`, `PurchaseOrderService.java` | — |
| **Controller** | `PurchaseOrderController.java` | — |
| **Constants** | — | `AppPermissions.java` (+3 constantes) |
| **Frontend model** | `purchase-order.model.ts` | — |
| **Frontend service** | `purchase-order.service.ts`, `purchase-order-form.service.ts` | — |
| **Frontend components** | `purchase-order-page`, `purchase-order-table`, `purchase-order-my-list`, `purchase-order-form` | `transactional-document-detail` (+sub-sección + signal + loadLinkedRecords case) |
| **Frontend router** | — | `administration.routes.ts` o `inventory.routes.ts` (+ruta `ordenes-compra`) |

---

## Notas críticas de implementación

1. **Reutilizar `DocumentPickerComponent`:** ya existe desde Feature 2 en `shared/components/document-picker/`. No recrear. Importarlo directamente en `purchase-order-table` para el modal de vinculación de facturas.

2. **Reutilizar `TransactionalDocumentSummaryDTO` (backend):** ya existe desde Feature 2. Importar en `PurchaseOrderResponseDTO` sin duplicar.

3. **Número de migración Flyway:** antes de crear el archivo SQL, ejecutar `ls src/main/resources/db/migration/` y tomar el número máximo + 1.

4. **`TransactionalDocumentRepository.findByIdAndTenantId()`:** verificar si ya existe antes de agregarlo. Puede estar disponible via Spring Data naming convention.

5. **`getMyPurchaseOrders()` no devuelve COMPRADA:** el filtro `po.status != COMPRADA` está en la query del repositorio, no en el servicio. Mantener consistencia con el patrón de `RepairOrderRepository.findAllByCreatedByUserWithFilters()`.

6. **Directorio de enums:** los tres enums nuevos van en `model/enums/` (raíz, no en `/vehicle/`) dado que las órdenes de compra son transversales a la empresa.

7. **Ubicación de la entidad:** `model/entity/PurchaseOrder.java` en el paquete raíz de entidades, al mismo nivel que `TransactionalDocument.java` y `Supplier.java`, no dentro del subpaquete `/vehicle/`.

8. **El endpoint `link-document` es separado de `status`:** decisión deliberada para mantener la máquina de estados limpia. El supervisor primero marca COMPRADA (transición de estado), y luego opcionalmente vincula la factura (operación distinta). Esto permite que queden órdenes COMPRADA sin factura temporalmente, lo cual es válido en el mundo real.

9. **Sin `RepairOrderCompleteForm` equivalente:** la feature de purchase orders no necesita un formulario de "completar" complejo como el de repair orders. La vinculación de la factura se hace directamente desde la tabla con el `DocumentPickerComponent`, sin un flujo de múltiples pasos.
