# Feature 3 — Facturación de Ventas y Gestión de Clientes

## Contexto

Las empresas constructoras argentinas facturan sus servicios a comitentes, municipios, organismos y empresas privadas. Esta funcionalidad es completamente independiente del módulo de compras existente: el flujo es inverso (empresa → cliente), el destinatario es un cliente (no un proveedor), y el tipo de comprobante depende de la condición impositiva del receptor frente al IVA. Los tipos de comprobante aplicables son: Factura A (para Responsables Inscriptos), Factura B (para Consumidores Finales y Exentos) y Factura C (para Monotributistas).

El módulo se organiza dentro de una nueva sección **"Clientes"**, con una vista de lista de clientes y una vista de detalle por cliente que incluye el historial de facturación.

---

## Modelo de datos — nuevas entidades

### Nueva entidad: `Client` (Cliente)

Análogo a `Supplier` pero para el lado de ventas.

```java
@Entity
@Table(name = "clients", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "cuit"})
})
public class Client extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String cuit;                      // CUIT del cliente (único por tenant)

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;              // razón social

    @Column(name = "trade_name", length = 200)
    private String tradeName;                 // nombre comercial (opcional)

    @Enumerated(EnumType.STRING)
    @Column(name = "iva_condition", nullable = false, length = 30)
    private IvaCondition ivaCondition;        // condición IVA (ver enum abajo)

    @Embedded
    private Address address;                  // reutiliza Address embeddable existente

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private ContactInfo contactInfo;          // reutiliza ContactInfo entity existente

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
```

#### Nuevo enum: `IvaCondition`

```java
public enum IvaCondition {
    RESPONSABLE_INSCRIPTO,  // → Factura A
    MONOTRIBUTISTA,         // → Factura C
    EXENTO,                 // → Factura B
    CONSUMIDOR_FINAL        // → Factura B
}
```

---

### Nueva entidad: `SalesDocument` (Comprobante de Venta)

Análogo a `TransactionalDocument` pero para ventas.

```java
@Entity
@Table(name = "sales_documents")
public class SalesDocument extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private SalesDocumentType documentType;

    @Column(name = "branch_code", nullable = false, length = 5)
    private String branchCode;               // punto de venta (5 dígitos, AFIP)

    @Column(name = "document_number", nullable = false, length = 8)
    private String documentNumber;           // número de comprobante (8 dígitos, AFIP)

    @Column(nullable = false)
    private LocalDate date;                  // fecha de emisión

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "purchase_order_reference", length = 100)
    private String purchaseOrderReference;   // número de pedido del cliente (opcional)

    @Column(name = "net_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal netTotal;

    @Column(name = "iva_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal ivaTotal;

    @Column(name = "iva_exempt_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal ivaExemptTotal;

    @Column(name = "other_taxes", nullable = false, precision = 19, scale = 2)
    private BigDecimal otherTaxes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @OneToMany(mappedBy = "salesDocument", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SalesItemDetail> items = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean paid = false;            // cobrado/pendiente de cobro

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
```

#### Nuevo enum: `SalesDocumentType`

```java
public enum SalesDocumentType {
    FACTURA_A, FACTURA_B, FACTURA_C,
    NOTA_DEBITO_A, NOTA_DEBITO_B, NOTA_DEBITO_C,
    NOTA_CREDITO_A, NOTA_CREDITO_B, NOTA_CREDITO_C
}
```

---

### Nueva entidad: `SalesItemDetail` (Ítems del comprobante de venta)

Análogo a `ItemDetail` pero vinculado a `SalesDocument`. Reutiliza la entidad `Item` existente.

```java
@Entity
@Table(name = "sales_item_details")
public class SalesItemDetail extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;                       // reutiliza Item existente

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_document_id", nullable = false)
    private SalesDocument salesDocument;

    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "iva_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPercentage;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @PrePersist @PreUpdate
    private void calculateTotalAmount() {
        if (this.totalAmount == null) {
            BigDecimal subtotal = unitAmount.multiply(BigDecimal.valueOf(quantity));
            BigDecimal ivaFactor = BigDecimal.ONE.add(
                ivaPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            this.totalAmount = subtotal.multiply(ivaFactor).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
```

---

## DTOs

### `ClientDTO` (request)

```java
public record ClientDTO(
    @NotBlank @Size(max = 100) String cuit,
    @NotBlank @Size(max = 200) String businessName,
    @Size(max = 200) String tradeName,
    @NotNull IvaCondition ivaCondition,
    @Valid Address address,
    @Valid ContactInfoDTO contactInfo,
    @NotNull Boolean active
) {}
```

### `ClientResponseDTO` (response)

```java
public record ClientResponseDTO(
    Long id,
    String cuit,
    String businessName,
    String tradeName,
    IvaCondition ivaCondition,
    Address address,
    ContactInfoResponseDTO contactInfo,
    Boolean active,
    Boolean deleted
) {}
```

### `ClientSummaryDTO` (para mostrar en sub-listados)

```java
public record ClientSummaryDTO(
    Long id,
    String cuit,
    String businessName,
    String tradeName,
    IvaCondition ivaCondition
) {}
```

### `SalesDocumentDTO` (request)

```java
public record SalesDocumentDTO(
    @NotNull SalesDocumentType documentType,
    @NotBlank @Pattern(regexp = "\\d{5}") String branchCode,
    @NotBlank @Pattern(regexp = "\\d{8}") String documentNumber,
    @NotNull @PastOrPresent LocalDate date,
    @NotNull @Positive Long clientId,
    @Size(max = 100) String purchaseOrderReference,
    @NotNull @DecimalMin("0.00") BigDecimal netTotal,
    @NotNull @DecimalMin("0.00") BigDecimal ivaTotal,
    @NotNull @DecimalMin("0.00") BigDecimal ivaExemptTotal,
    @NotNull @DecimalMin("0.00") BigDecimal otherTaxes,
    @NotNull @DecimalMin("0.00") BigDecimal total,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3, fraction=2) BigDecimal discountPercentage,
    Long projectAreaId,
    @NotNull @Valid @NotEmpty List<SalesItemDetailDTO> items,
    @Size(max = 500) String comment,
    boolean deleted
) {}
```

### `SalesDocumentResponseDTO` (response)

```java
public record SalesDocumentResponseDTO(
    Long id,
    SalesDocumentType documentType,
    String branchCode,
    String documentNumber,
    LocalDate date,
    ClientSummaryDTO client,
    String purchaseOrderReference,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    BigDecimal ivaExemptTotal,
    BigDecimal otherTaxes,
    BigDecimal total,
    BigDecimal discountPercentage,
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    List<SalesItemDetailResponseDTO> items,
    Boolean paid,
    String comment,
    Boolean deleted
) {}
```

### `SalesItemDetailDTO` (request)

```java
public record SalesItemDetailDTO(
    Long id,
    @NotNull @Positive Long itemId,
    @NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal unitAmount,
    @NotNull @Min(1) Integer quantity,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3, fraction=2) BigDecimal ivaPercentage,
    @DecimalMin("0.01") @Digits(integer=19, fraction=2) BigDecimal totalAmount
) {}
```

### `SalesItemDetailResponseDTO` (response)

```java
public record SalesItemDetailResponseDTO(
    Long id,
    BigDecimal unitAmount,
    Integer quantity,
    BigDecimal ivaPercentage,
    BigDecimal totalAmount,
    Long itemId,
    String itemName
) {}
```

### `ClientStatsDTO` (para totales en el detalle del cliente)

```java
public record ClientStatsDTO(
    BigDecimal totalInvoiced,      // suma de total de todos los SalesDocuments del cliente
    BigDecimal totalCollected,     // suma de total donde paid = true
    BigDecimal totalPending        // totalInvoiced - totalCollected
) {}
```

### `SalesDocumentFilterDTO` (para GET con filtros)

```java
public record SalesDocumentFilterDTO(
    Long clientId,
    String clientCuit,
    String clientBusinessName,
    SalesDocumentType documentType,
    String documentNumber,
    LocalDate dateFrom,
    LocalDate dateTo,
    BigDecimal minTotal,
    BigDecimal maxTotal,
    Boolean paid,
    Long projectAreaId,
    String purchaseOrderReference,
    String search
) {}
```

### `ClientFilterDTO`

```java
public record ClientFilterDTO(
    String cuit,
    String businessName,
    IvaCondition ivaCondition,
    Boolean active,
    String search
) {}
```

---

## Servicios

### `IClientService` / `ClientService`

```java
public interface IClientService {
    ClientResponseDTO createClient(ClientDTO dto);
    ClientResponseDTO getClientById(Long id);
    ClientResponseDTO updateClient(Long id, ClientDTO dto);
    void deleteClient(Long id);
    Page<ClientResponseDTO> getAllClients(ClientFilterDTO filter, Pageable pageable);
    ClientStatsDTO getClientStats(Long clientId);  // totales facturado/cobrado/pendiente
}
```

### `ISalesDocumentService` / `SalesDocumentService`

```java
public interface ISalesDocumentService {
    SalesDocumentResponseDTO createSalesDocument(SalesDocumentDTO dto);
    SalesDocumentResponseDTO getSalesDocumentById(Long id);
    SalesDocumentResponseDTO updateSalesDocument(Long id, SalesDocumentDTO dto);
    void deleteSalesDocument(Long id);
    Page<SalesDocumentResponseDTO> getAllSalesDocuments(SalesDocumentFilterDTO filter, Pageable pageable);
    SalesDocumentResponseDTO markAsPaid(Long id, boolean paid);
}
```

**Lógica en `SalesDocumentService.createSalesDocument()`:** análoga a `TransactionalDocumentService.createTransactionalDocument()` — gestiona ítems con `orphanRemoval`, calcula totales, valida que el `client` exista y no esté eliminado.

---

## Repositories

```java
// ClientRepository
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByCuitAndTenantId(String cuit, Long tenantId);
    Page<Client> findAllWithFilters(...);  // @Query con filtros nullables
}

// SalesDocumentRepository
@Repository
public interface SalesDocumentRepository extends JpaRepository<SalesDocument, Long> {
    Page<SalesDocument> findAllWithFilters(...);  // @Query con todos los filtros
    
    @Query("SELECT new PSG.backEnd.model.dto.sales.ClientStatsDTO(" +
           "  COALESCE(SUM(sd.total), 0), " +
           "  COALESCE(SUM(CASE WHEN sd.paid = true THEN sd.total ELSE 0 END), 0), " +
           "  COALESCE(SUM(CASE WHEN sd.paid = false THEN sd.total ELSE 0 END), 0)) " +
           "FROM SalesDocument sd " +
           "WHERE sd.client.id = :clientId AND sd.deleted = false AND sd.tenantId = :tenantId")
    ClientStatsDTO getStatsByClientId(Long clientId, Long tenantId);
}
```

---

## API — nuevos controllers

### `ClientController` — `/api/v1/clients`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/` | Crea un nuevo cliente |
| `GET` | `/` | Lista clientes con filtros (cuit, razón social, IVA condition, activo) |
| `GET` | `/{id}` | Obtiene un cliente por ID |
| `GET` | `/{id}/stats` | Devuelve totales: facturado, cobrado, pendiente |
| `PATCH` | `/{id}` | Actualiza un cliente |
| `DELETE` | `/{id}` | Soft-delete |

### `SalesDocumentController` — `/api/v1/sales-documents`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/` | Crea un comprobante de venta |
| `GET` | `/` | Lista con filtros (clientId, tipo, fechas, monto, paid, etc.) |
| `GET` | `/{id}` | Obtiene un comprobante por ID |
| `PATCH` | `/{id}` | Actualiza un comprobante |
| `DELETE` | `/{id}` | Soft-delete |
| `PATCH` | `/{id}/paid` | Marca como cobrado/pendiente. Body: `{ paid: boolean }` |

---

## Migración de base de datos — `V22`

```sql
-- V22__add_sales_invoicing.sql

CREATE TABLE clients (
    id              BIGSERIAL PRIMARY KEY,
    cuit            VARCHAR(100) NOT NULL,
    business_name   VARCHAR(200) NOT NULL,
    trade_name      VARCHAR(200),
    iva_condition   VARCHAR(30) NOT NULL,
    street          VARCHAR(200),
    number          INTEGER,
    city            VARCHAR(100),
    state           VARCHAR(100),
    country         VARCHAR(100),
    zip_code        VARCHAR(20),
    contact_info_id BIGINT REFERENCES contact_info(id),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id),
    UNIQUE (tenant_id, cuit)
);

CREATE TABLE sales_documents (
    id                         BIGSERIAL PRIMARY KEY,
    document_type              VARCHAR(20) NOT NULL,
    branch_code                VARCHAR(5) NOT NULL,
    document_number            VARCHAR(8) NOT NULL,
    date                       DATE NOT NULL,
    client_id                  BIGINT NOT NULL REFERENCES clients(id),
    purchase_order_reference   VARCHAR(100),
    net_total                  NUMERIC(19,2) NOT NULL,
    iva_total                  NUMERIC(19,2) NOT NULL,
    iva_exempt_total           NUMERIC(19,2) NOT NULL,
    other_taxes                NUMERIC(19,2) NOT NULL,
    total                      NUMERIC(19,2) NOT NULL,
    discount_percentage        NUMERIC(5,2) NOT NULL DEFAULT 0,
    project_area_id            BIGINT REFERENCES project_areas(id),
    paid                       BOOLEAN NOT NULL DEFAULT FALSE,
    comment                    VARCHAR(500),
    deleted                    BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id                  BIGINT NOT NULL REFERENCES tenants(id)
);

CREATE TABLE sales_item_details (
    id                  BIGSERIAL PRIMARY KEY,
    item_id             BIGINT NOT NULL REFERENCES items(id),
    sales_document_id   BIGINT NOT NULL REFERENCES sales_documents(id),
    unit_amount         NUMERIC(19,2) NOT NULL,
    quantity            INTEGER NOT NULL,
    iva_percentage      NUMERIC(5,2) NOT NULL,
    total_amount        NUMERIC(19,2) NOT NULL,
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id)
);

INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description) VALUES
    ('CLIENT_READ',           'Clients', 'clients', 'Read clients',           'Ver Clientes',             'Permite ver el listado de clientes'),
    ('CLIENT_WRITE',          'Clients', 'clients', 'Create/update clients',  'Crear/Editar Clientes',    'Permite crear y modificar clientes'),
    ('CLIENT_DELETE',         'Clients', 'clients', 'Delete clients',         'Eliminar Clientes',        'Permite eliminar clientes'),
    ('SALES_DOCUMENT_READ',   'Clients', 'clients', 'Read sales documents',   'Ver Comprobantes de Venta','Permite ver comprobantes de venta'),
    ('SALES_DOCUMENT_WRITE',  'Clients', 'clients', 'Create/update sales docs','Crear Comp. de Venta',   'Permite crear y modificar comprobantes de venta'),
    ('SALES_DOCUMENT_DELETE', 'Clients', 'clients', 'Delete sales documents', 'Eliminar Comp. de Venta', 'Permite eliminar comprobantes de venta');
```

---

## Frontend

### Estructura de carpetas nueva: `domains/clients/`

```
domains/clients/
├── clients-page/                           — shell: SubHeader + RouterOutlet
│   ├── clients-page.ts
│   └── clients-page.html
├── client/
│   ├── services/
│   │   ├── client.service.ts               — HTTP CRUD para clientes
│   │   └── client-form.service.ts          — coordinación tabla/form/detalle
│   ├── client-table/                       — listado de clientes
│   │   ├── client-table.ts
│   │   └── client-table.html
│   ├── client-form/                        — crear/editar cliente
│   │   ├── client-form.ts
│   │   └── client-form.html
│   └── client-page/                        — contenedor tabla + form popup
│       ├── client-page.ts
│       └── client-page.html
├── sales-document/
│   ├── services/
│   │   ├── sales-document.service.ts       — HTTP CRUD para ventas
│   │   └── sales-document-form.service.ts  — coordinación form/detail
│   ├── sales-document-form/                — formulario completo de venta (espeja transactional-document-form)
│   │   ├── sales-document-form.ts
│   │   └── sales-document-form.html
│   ├── sales-document-detail/              — popup de detalle (solo lectura)
│   │   ├── sales-document-detail.ts
│   │   └── sales-document-detail.html
│   └── sales-document-table/              — sub-tabla dentro de client-detail-page
│       ├── sales-document-table.ts
│       └── sales-document-table.html
├── client-detail-page/                     — página completa con info del cliente + sub-listado
│   ├── client-detail-page.ts
│   └── client-detail-page.html
└── clients.routes.ts
```

### Modelos frontend nuevos

```typescript
// shared/models/client.model.ts
export interface Client {
  id?: number;
  cuit: string;
  businessName: string;
  tradeName?: string;
  ivaCondition: IvaCondition;
  address?: Address;
  contactInfo?: ContactInfo;
  active: boolean;
  deleted?: boolean;
}

export enum IvaCondition {
  RESPONSABLE_INSCRIPTO = 'RESPONSABLE_INSCRIPTO',
  MONOTRIBUTISTA        = 'MONOTRIBUTISTA',
  EXENTO                = 'EXENTO',
  CONSUMIDOR_FINAL      = 'CONSUMIDOR_FINAL'
}

export const IvaConditionLabels: Record<IvaCondition, string> = {
  [IvaCondition.RESPONSABLE_INSCRIPTO]: 'Responsable Inscripto',
  [IvaCondition.MONOTRIBUTISTA]:        'Monotributista',
  [IvaCondition.EXENTO]:                'Exento',
  [IvaCondition.CONSUMIDOR_FINAL]:      'Consumidor Final'
};

export interface ClientStats {
  totalInvoiced:  number;
  totalCollected: number;
  totalPending:   number;
}

export interface ClientFilters {
  cuit?: string;
  businessName?: string;
  ivaCondition?: string;
  active?: boolean | null;
  search?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}
```

```typescript
// shared/models/sales-document.model.ts
export interface SalesDocument {
  id?: number;
  documentType: SalesDocumentType | string;
  branchCode: string;
  documentNumber: string;
  date: string;
  clientId: number;
  clientBusinessName?: string;
  clientCuit?: string;
  purchaseOrderReference?: string;
  netTotal: number;
  ivaTotal: number;
  ivaExemptTotal: number;
  otherTaxes: number;
  total: number;
  discountPercentage: number;
  projectAreaId?: number;
  projectAreaName?: string;
  projectAreaColor?: string;
  items: SalesItemDetail[];
  paid: boolean;
  comment?: string;
  deleted?: boolean;
}

export enum SalesDocumentType {
  FACTURA_A      = 'FACTURA_A',
  FACTURA_B      = 'FACTURA_B',
  FACTURA_C      = 'FACTURA_C',
  NOTA_DEBITO_A  = 'NOTA_DEBITO_A',
  NOTA_DEBITO_B  = 'NOTA_DEBITO_B',
  NOTA_DEBITO_C  = 'NOTA_DEBITO_C',
  NOTA_CREDITO_A = 'NOTA_CREDITO_A',
  NOTA_CREDITO_B = 'NOTA_CREDITO_B',
  NOTA_CREDITO_C = 'NOTA_CREDITO_C'
}

export const SalesDocumentTypeLabels: Record<SalesDocumentType, string> = {
  [SalesDocumentType.FACTURA_A]:      'Factura A',
  [SalesDocumentType.FACTURA_B]:      'Factura B',
  [SalesDocumentType.FACTURA_C]:      'Factura C',
  [SalesDocumentType.NOTA_DEBITO_A]:  'Nota de Débito A',
  [SalesDocumentType.NOTA_DEBITO_B]:  'Nota de Débito B',
  [SalesDocumentType.NOTA_DEBITO_C]:  'Nota de Débito C',
  [SalesDocumentType.NOTA_CREDITO_A]: 'Nota de Crédito A',
  [SalesDocumentType.NOTA_CREDITO_B]: 'Nota de Crédito B',
  [SalesDocumentType.NOTA_CREDITO_C]: 'Nota de Crédito C'
};

export interface SalesItemDetail {
  id?: number;
  itemId: number;
  itemName?: string;
  unitAmount: number;
  quantity: number;
  ivaPercentage: number;
  totalAmount?: number;
}

export interface SalesDocumentFilters {
  clientId?: number | null;
  clientBusinessName?: string;
  documentType?: string;
  documentNumber?: string;
  dateFrom?: string;
  dateTo?: string;
  minTotal?: number | null;
  maxTotal?: number | null;
  paid?: boolean | null;
  projectAreaId?: number | null;
  purchaseOrderReference?: string;
  search?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}
```

### Rutas: `clients.routes.ts`

```typescript
export const CLIENTS_ROUTES: Routes = [
  {
    path: '',
    component: ClientsPage,
    children: [
      {
        path: '',
        loadComponent: () => import('./client/client-page/client-page').then(m => m.ClientPage)
      },
      {
        path: ':id',
        loadComponent: () => import('./client-detail-page/client-detail-page').then(m => m.ClientDetailPage)
      }
    ]
  }
];
```

### Registro en `app.routes.ts`

```typescript
{
  path: 'clientes',
  loadChildren: () => import('./domains/clients/clients.routes').then(m => m.CLIENTS_ROUTES),
  canActivate: [RoleGuard],
  data: { requiredPermissions: ['CLIENT_READ', 'SALES_DOCUMENT_READ'] }
}
```

---

### Componente: `clients-page`

Shell con `SubHeader` y `RouterOutlet`. Tabs:

```typescript
readonly subHeaderTabs = computed<Tab[]>(() => [
  { id: 'clientes', label: 'Clientes', route: '/clientes', allowedPermissions: ['CLIENT_READ'] }
]);
```

> No necesita múltiples tabs por ahora — la sub-navegación es mediante el router (lista → detalle).

### Componente: `client-page`

Contenedor que muestra `<app-client-table>` y el popup `<app-client-form>` cuando corresponde. Sigue el mismo patrón que `transactional-document-page`.

### Componente: `client-table`

- Extiende `BaseTableDirective<Client>`.
- Columnas: Razón Social, CUIT, Condición IVA, Estado (activo/inactivo).
- Filtros: búsqueda por razón social o CUIT, condición IVA (select), activo (select).
- Acción principal por fila: **"Ver detalle"** → navega a `/clientes/:id`.
- Botones secundarios: editar (abre form popup), eliminar (modal de confirmación).
- Botón `"+ Nuevo Cliente"` → abre `client-form`.

### Componente: `client-form`

Formulario popup. Sigue el patrón de `PopupFormShell`.

Campos:
| Campo | Tipo | Obligatorio |
|---|---|---|
| CUIT | text (11 dígitos) | Sí |
| Razón Social | text | Sí |
| Nombre Comercial | text | No |
| Condición IVA | select | Sí |
| Calle | text | No |
| Número | number | No |
| Localidad | text | No |
| Provincia | text | No |
| País | text | No |
| Código Postal | text | No |
| Email(s) | text (multi) | No |
| Teléfono(s) | text (multi) | No |
| Activo | checkbox/toggle | Sí |

### Componente: `client-detail-page` (página completa, no popup)

Esta es la vista central del módulo. Se renderiza al hacer clic en un cliente desde el listado.

**Sección superior — Datos del cliente:**
- Muestra todos los campos del cliente en modo lectura.
- Botón "Editar cliente" → abre `client-form` en modo edición.

**Sección de totales (cards/chips):**
- Total Facturado: suma de `total` de todos los comprobantes de venta del cliente.
- Total Cobrado: suma de `total` donde `paid = true`.
- Saldo Pendiente: diferencia (cobrado en color verde, pendiente en color naranja/rojo).
- Se llama a `GET /api/v1/clients/{id}/stats`.

**Sección inferior — Comprobantes de Venta:**
- Sub-tabla `<app-sales-document-table>` filtrada por el `clientId` del cliente actual.
- Columnas: Tipo, Número, Fecha, Referencia/Pedido, Total, Estado (Cobrado / Pendiente).
- Filtros: tipo de documento, fechas, paid, búsqueda.
- Acción: ver detalle (abre `sales-document-detail`), editar, eliminar.
- Botón `"+ Nuevo Comprobante de Venta"` → abre `sales-document-form` con `clientId` pre-cargado.
- Acción rápida en tabla: botón "Marcar cobrado / pendiente" → `PATCH /{id}/paid`.

**Template estructura:**

```html
<div class="client-detail-page">
  <div class="client-detail-page__back">
    <button (click)="goBack()">← Volver a clientes</button>
  </div>

  <!-- Datos del cliente -->
  <section class="client-info">
    <h2>{{ client().businessName }}</h2>
    <p>CUIT: {{ client().cuit }} &nbsp;|&nbsp; {{ ivaConditionLabel(client().ivaCondition) }}</p>
    <!-- resto de datos -->
    <button *appHasPermission="'CLIENT_WRITE'" (click)="editClient()">Editar cliente</button>
  </section>

  <!-- Totales -->
  <section class="client-stats">
    <div class="stat-card">
      <span>Total Facturado</span>
      <strong>$ {{ stats().totalInvoiced | number:'1.2-2' }}</strong>
    </div>
    <div class="stat-card stat-card--collected">
      <span>Total Cobrado</span>
      <strong>$ {{ stats().totalCollected | number:'1.2-2' }}</strong>
    </div>
    <div class="stat-card stat-card--pending">
      <span>Saldo Pendiente</span>
      <strong>$ {{ stats().totalPending | number:'1.2-2' }}</strong>
    </div>
  </section>

  <!-- Sub-tabla de comprobantes de venta -->
  <section class="client-sales">
    <h3>Comprobantes de Venta</h3>
    <app-sales-document-table [clientId]="clientId()" (onNewClick)="openSalesForm()" />
    @if (showSalesForm()) {
      <app-sales-document-form [preselectedClientId]="clientId()" (onFormClosed)="closeSalesForm()" />
    }
    @if (showSalesDetail()) {
      <app-sales-document-detail (onDetailsClosed)="closeSalesDetail()" />
    }
  </section>
</div>
```

### Componente: `sales-document-form`

Espeja casi completamente `transactional-document-form`. Diferencias clave:

| Campo | En compras (`transactional-document-form`) | En ventas (`sales-document-form`) |
|---|---|---|
| Proveedor | Lookup de `Supplier` | Lookup de `Client` (o pre-cargado si viene de `client-detail-page`) |
| Tipo de documento | `DocumentType` (BILL_A, BILL_B...) | `SalesDocumentType` (FACTURA_A, FACTURA_B...) |
| Campo extra | — | `purchaseOrderReference` (texto libre, "N° de Pedido del Cliente") |
| Paid | Pagado (¿se pagó al proveedor?) | Cobrado (¿se cobró al cliente?) |
| Supplier ID bloqueado | No | `clientId` bloqueado si viene de `client-detail-page` |

El formulario tiene las mismas secciones:
1. Encabezado: cliente, tipo de documento, punto de venta, número, fecha, área, referencia de pedido.
2. Ítems: tabla igual a la de `transactional-document-form` (item lookup, cantidad, precio unitario, IVA, total).
3. Totales: neto, IVA, IVA exento, otros impuestos, descuento, total general.
4. Opciones: comentario, marcar como cobrado.

**Input:** `@Input() preselectedClientId: number | null = null` — si viene pre-cargado, el selector de cliente se muestra disabled.

### Componente: `sales-document-table`

- Extiende `BaseTableDirective<SalesDocument>`.
- `@Input() clientId: number | null = null` — si se pasa, filtra por cliente (uso en `client-detail-page`).
- Columnas: Tipo, N° Comprobante, Fecha, N° Pedido Cliente, Total, Estado (Cobrado/Pendiente).
- Filtros: tipo, fechas, monto, paid.
- Estado como chip: verde = Cobrado, naranja = Pendiente.
- Acción rápida: toggle "Cobrado/Pendiente" → `PATCH /sales-documents/{id}/paid`.

### Componente: `sales-document-detail`

Popup de solo lectura. Misma estructura que `transactional-document-detail`, mostrando:
- Datos del cliente (nombre, CUIT, condición IVA).
- Encabezado del comprobante (tipo, número, fecha, área, referencia de pedido).
- Tabla de ítems.
- Totales.
- Estado de cobro.

---

## Resumen de cambios por capa

| Capa | Archivos nuevos | Archivos modificados |
|---|---|---|
| **DB Migration** | `V22__add_sales_invoicing.sql` | — |
| **Enum** | `IvaCondition.java`, `SalesDocumentType.java` | — |
| **Entity** | `Client.java`, `SalesDocument.java`, `SalesItemDetail.java` | — |
| **DTO** | `ClientDTO`, `ClientResponseDTO`, `ClientSummaryDTO`, `ClientStatsDTO`, `ClientFilterDTO`, `SalesDocumentDTO`, `SalesDocumentResponseDTO`, `SalesDocumentFilterDTO`, `SalesItemDetailDTO`, `SalesItemDetailResponseDTO` | — |
| **Mapper** | `ClientMapper.java`, `SalesDocumentMapper.java` | — |
| **Repository** | `ClientRepository.java`, `SalesDocumentRepository.java` | — |
| **Service** | `IClientService`, `ClientService`, `ISalesDocumentService`, `SalesDocumentService` | — |
| **Controller** | `ClientController.java`, `SalesDocumentController.java` | — |
| **Frontend model** | `client.model.ts`, `sales-document.model.ts` | — |
| **Frontend service** | `client.service.ts`, `client-form.service.ts`, `sales-document.service.ts`, `sales-document-form.service.ts` | — |
| **Frontend components** | `clients-page`, `client-page`, `client-table`, `client-form`, `client-detail-page`, `sales-document-form`, `sales-document-table`, `sales-document-detail` | `app.routes.ts` (+ruta `/clientes`) |
