# Feature 8 — Certificaciones de Obra y Seguimiento de Facturación por Contrato

## Contexto

En empresas constructoras argentinas, la relación financiera con un comitente se estructura a través de un **contrato u orden de trabajo** que fija el monto total. A medida que avanza la obra se emiten **certificados de avance**, cada uno representa trabajo ejecutado en un período y puede vincularse a una factura de venta.

El objetivo central de este módulo es dar visibilidad inmediata sobre tres preguntas clave:
- ¿Cuánto se certificó del total contratado?
- ¿Cuánto de lo certificado ya fue facturado?
- ¿Cuánto de lo facturado ya fue cobrado?

> **Dependencia con Feature 7:** `WorkContract.client` referencia a `Supplier` con `isClient = true`. `Certification.salesDocument` referencia a `SalesDocument` de F7.

---

## Modelo de datos — nuevas entidades

### Nueva entidad: `WorkContract` (Contrato / Pedido de Obra)

```java
@Entity
@Table(name = "work_contracts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "contract_number"})
})
public class WorkContract extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", nullable = false, length = 50)
    private String contractNumber;          // Número de contrato/pedido, único por tenant

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Supplier client;               // FK → suppliers WHERE isClient = true (F3)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @Column(nullable = false, length = 1000)
    private String description;            // Objeto del contrato

    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;        // Fecha de firma o inicio

    @Column(name = "end_date")
    private LocalDate endDate;             // Fecha de vencimiento (opcional)

    @Column(name = "contracted_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal contractedAmount;   // Monto total contratado

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Currency currency;             // ARS | USD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private WorkContractStatus status = WorkContractStatus.ACTIVO;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
```

#### Nuevo enum: `WorkContractStatus`

```java
public enum WorkContractStatus {
    ACTIVO,
    SUSPENDIDO,
    FINALIZADO
}
```

#### Nuevo enum: `Currency`

```java
public enum Currency {
    ARS,
    USD
}
```

---

### Nueva entidad: `Certification` (Certificado de Avance)

Diseñada intencionalmente sin restricciones en las transiciones de estado. El operador puede asignar cualquier estado en cualquier momento.

```java
@Entity
@Table(name = "certifications")
public class Certification extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certification_number", nullable = false)
    private Integer certificationNumber;    // Auto-asignado: MAX + 1 dentro del contrato

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "work_contract_id", nullable = false)
    private WorkContract contract;

    @Column(name = "certification_date", nullable = false)
    private LocalDate certificationDate;

    @Column(name = "certified_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal certifiedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_document_id")
    private SalesDocument salesDocument;    // FK opcional — comprobante de venta vinculado

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private CertificationStatus status = CertificationStatus.PRESENTADO;
    // Sin restricciones de transición — cualquier cambio es válido

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
```

#### Nuevo enum: `CertificationStatus`

```java
public enum CertificationStatus {
    PRESENTADO,   // emitido, pendiente
    APROBADO,     // aprobado por el cliente
    FACTURADO,    // comprobante de venta emitido
    COBRADO       // dinero ingresado
}
```

> **Sin validaciones de transición:** a diferencia de otros módulos, el servicio no impone flujo obligatorio entre estados. El operador puede pasar de `PRESENTADO` a `COBRADO` directamente, o volver a `APROBADO` desde `FACTURADO`. Esto responde a la realidad del trabajo en obra, donde la secuencia administrativa no siempre sigue el orden formal.

---

## DTOs

### `WorkContractDTO` (request)

```java
public record WorkContractDTO(
    @NotBlank @Size(max = 50) String contractNumber,
    @NotNull @Positive Long clientId,
    Long projectAreaId,
    @NotBlank @Size(max = 1000) String description,
    @NotNull LocalDate contractDate,
    LocalDate endDate,                  // nullable — fecha de vencimiento opcional
    @NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal contractedAmount,
    @NotNull Currency currency,
    @NotNull WorkContractStatus status,
    @Size(max = 500) String comment
) {}
```

### `WorkContractResponseDTO` (response)

```java
public record WorkContractResponseDTO(
    Long id,
    String contractNumber,
    Long clientId,
    String clientBusinessName,
    String clientCuit,
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    String description,
    LocalDate contractDate,
    LocalDate endDate,
    BigDecimal contractedAmount,
    Currency currency,
    WorkContractStatus status,
    String comment,
    Boolean deleted
) {}
```

### `WorkContractStatsDTO` (métricas de seguimiento)

Este DTO es el núcleo del módulo. Todas las métricas se calculan con una query con `SUM(CASE WHEN...)` en lugar de múltiples consultas.

```java
public record WorkContractStatsDTO(
    // ------ Certificación ------
    BigDecimal contractedAmount,        // monto total del contrato
    BigDecimal totalCertified,          // suma de certifiedAmount (todas las certificaciones)
    BigDecimal pendingToCertify,        // contractedAmount − totalCertified

    // ------ Facturación ------
    BigDecimal totalInvoiced,           // suma de certifiedAmount donde salesDocument != null
    BigDecimal pendingToInvoice,        // totalCertified − totalInvoiced

    // ------ Cobro ------
    BigDecimal totalCollected,          // suma de certifiedAmount donde status = COBRADO
    BigDecimal pendingToCollect,        // totalInvoiced − totalCollected

    // ------ Porcentajes ------
    BigDecimal certificationProgress,   // (totalCertified / contractedAmount) × 100
    BigDecimal invoicingProgress,       // (totalInvoiced / totalCertified) × 100 — puede ser 0 si sin certif.
    BigDecimal collectionProgress       // (totalCollected / totalInvoiced) × 100 — puede ser 0 si sin facturas
) {}
```

### `WorkContractFilterDTO`

```java
public record WorkContractFilterDTO(
    Long clientId,
    String clientBusinessName,
    String contractNumber,
    Long projectAreaId,
    WorkContractStatus status,
    Currency currency,
    LocalDate contractDateFrom,
    LocalDate contractDateTo,
    BigDecimal minContractedAmount,
    BigDecimal maxContractedAmount,
    String search     // busca en contractNumber + description + clientBusinessName
) {}
```

### `CertificationDTO` (request)

```java
public record CertificationDTO(
    @NotNull @Positive Long workContractId,
    @NotNull LocalDate certificationDate,
    @NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal certifiedAmount,
    Long salesDocumentId,               // nullable — se puede vincular o cambiar en cualquier momento
    @NotNull CertificationStatus status,
    @Size(max = 500) String comment
) {}
```

### `CertificationResponseDTO` (response)

```java
public record CertificationResponseDTO(
    Long id,
    Integer certificationNumber,
    Long workContractId,
    String contractNumber,
    LocalDate certificationDate,
    BigDecimal certifiedAmount,
    Long salesDocumentId,
    String salesDocumentLabel,          // "FACTURA_A 0001-00000123 — $2.500.000"
    CertificationStatus status,
    String comment,
    Boolean deleted
) {}
```

### `CertificationFilterDTO`

```java
public record CertificationFilterDTO(
    Long workContractId,
    Long clientId,
    CertificationStatus status,
    LocalDate dateFrom,
    LocalDate dateTo,
    Boolean hasInvoice,                 // true = con comprobante, false = sin comprobante
    Long salesDocumentId
) {}
```

---

## Servicios

### `IWorkContractService` / `WorkContractService`

```java
public interface IWorkContractService {
    WorkContractResponseDTO createWorkContract(WorkContractDTO dto);
    WorkContractResponseDTO getWorkContractById(Long id);
    WorkContractResponseDTO updateWorkContract(Long id, WorkContractDTO dto);
    void deleteWorkContract(Long id);
    Page<WorkContractResponseDTO> getAllWorkContracts(WorkContractFilterDTO filter, Pageable pageable);
    WorkContractStatsDTO getStats(Long workContractId);
}
```

**Lógica de `getStats()`:** delega el cálculo al repositorio mediante una query con `SUM(CASE WHEN...)`. Los porcentajes se calculan en Java con protección contra división por cero.

### `ICertificationService` / `CertificationService`

```java
public interface ICertificationService {
    CertificationResponseDTO createCertification(CertificationDTO dto);
    CertificationResponseDTO getCertificationById(Long id);
    CertificationResponseDTO updateCertification(Long id, CertificationDTO dto);
    void deleteCertification(Long id);
    Page<CertificationResponseDTO> getAllCertifications(CertificationFilterDTO filter, Pageable pageable);
}
```

**Reglas en `createCertification()`:**
- `certificationNumber` se asigna automáticamente: `MAX(certificationNumber) + 1` dentro del mismo contrato. Si no existen certificaciones previas, empieza en 1.
- Si `salesDocumentId` está presente, validar que el `SalesDocument.client` coincida con el `WorkContract.client`.
- **Sin validaciones de transición de estado.**

---

## Repositories

```java
@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    @Query("SELECT COALESCE(MAX(c.certificationNumber), 0) FROM Certification c " +
           "WHERE c.contract.id = :contractId AND c.tenantId = :tenantId AND c.deleted = false")
    Integer findMaxCertificationNumber(Long contractId, Long tenantId);

    @Query("""
        SELECT new PSG.backEnd.model.dto.certification.CertificationStatsProjection(
            COALESCE(SUM(c.certifiedAmount), 0),
            COALESCE(SUM(CASE WHEN c.salesDocument IS NOT NULL THEN c.certifiedAmount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN c.status = 'COBRADO' THEN c.certifiedAmount ELSE 0 END), 0)
        )
        FROM Certification c
        WHERE c.contract.id = :contractId AND c.deleted = false AND c.tenantId = :tenantId
        """)
    CertificationStatsProjection getStatsByContractId(Long contractId, Long tenantId);
}
```

---

## API — nuevos controllers

### `WorkContractController` — `/api/v1/work-contracts`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/` | Crea un nuevo contrato |
| `GET` | `/` | Lista con filtros |
| `GET` | `/{id}` | Obtiene un contrato por ID |
| `GET` | `/{id}/stats` | Devuelve las métricas de seguimiento financiero |
| `PATCH` | `/{id}` | Actualiza el contrato |
| `DELETE` | `/{id}` | Soft-delete |

### `CertificationController` — `/api/v1/certifications`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/` | Crea una certificación |
| `GET` | `/` | Lista con filtros (workContractId, status, fechas, hasInvoice) |
| `GET` | `/{id}` | Obtiene una certificación por ID |
| `PATCH` | `/{id}` | Actualiza cualquier campo, incluyendo status y salesDocumentId |
| `DELETE` | `/{id}` | Soft-delete |

> No se expone un endpoint específico para cambio de estado ni para vincular comprobante: ambas acciones se resuelven con `PATCH /{id}` estándar.

---

## Migración de base de datos — `V23`

```sql
-- V23__add_work_contracts_and_certifications.sql

CREATE TABLE work_contracts (
    id                  BIGSERIAL PRIMARY KEY,
    contract_number     VARCHAR(50)     NOT NULL,
    client_id           BIGINT          NOT NULL REFERENCES suppliers(id),
    project_area_id     BIGINT          REFERENCES project_areas(id),
    description         VARCHAR(1000)   NOT NULL,
    contract_date       DATE            NOT NULL,
    end_date            DATE,                          -- opcional
    contracted_amount   NUMERIC(19,2)   NOT NULL,
    currency            VARCHAR(5)      NOT NULL,
    status              VARCHAR(15)     NOT NULL DEFAULT 'ACTIVO',
    comment             VARCHAR(500),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    tenant_id           BIGINT          NOT NULL REFERENCES tenants(id),
    UNIQUE (tenant_id, contract_number)
);

CREATE TABLE certifications (
    id                      BIGSERIAL PRIMARY KEY,
    certification_number    INTEGER         NOT NULL,
    work_contract_id        BIGINT          NOT NULL REFERENCES work_contracts(id),
    certification_date      DATE            NOT NULL,
    certified_amount        NUMERIC(19,2)   NOT NULL,
    sales_document_id       BIGINT          REFERENCES sales_documents(id),   -- nullable
    status                  VARCHAR(15)     NOT NULL DEFAULT 'PRESENTADO',
    comment                 VARCHAR(500),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    tenant_id               BIGINT          NOT NULL REFERENCES tenants(id)
);

CREATE INDEX idx_certifications_work_contract ON certifications(work_contract_id);
CREATE INDEX idx_certifications_sales_document ON certifications(sales_document_id);
CREATE INDEX idx_work_contracts_client ON work_contracts(client_id);
CREATE INDEX idx_work_contracts_project_area ON work_contracts(project_area_id);

INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description) VALUES
    ('WORK_CONTRACT_READ',   'WorkContracts', 'contracts', 'Read work contracts',     'Ver Contratos',              'Permite ver el listado de contratos'),
    ('WORK_CONTRACT_WRITE',  'WorkContracts', 'contracts', 'Create/update contracts', 'Crear/Editar Contratos',     'Permite crear y modificar contratos'),
    ('WORK_CONTRACT_DELETE', 'WorkContracts', 'contracts', 'Delete work contracts',   'Eliminar Contratos',         'Permite eliminar contratos'),
    ('CERTIFICATION_READ',   'WorkContracts', 'contracts', 'Read certifications',     'Ver Certificaciones',        'Permite ver las certificaciones de avance'),
    ('CERTIFICATION_WRITE',  'WorkContracts', 'contracts', 'Create/update certs',     'Crear/Editar Certificaciones','Permite crear y modificar certificaciones'),
    ('CERTIFICATION_DELETE', 'WorkContracts', 'contracts', 'Delete certifications',   'Eliminar Certificaciones',   'Permite eliminar certificaciones');
```

---

## Frontend

### Estructura de carpetas nueva: `domains/contracts/`

```
domains/contracts/
├── contracts-page/
│   ├── contracts-page.ts
│   └── contracts-page.html
├── work-contract/
│   ├── services/
│   │   ├── work-contract.service.ts
│   │   └── work-contract-form.service.ts
│   ├── work-contract-table/
│   │   ├── work-contract-table.ts
│   │   └── work-contract-table.html
│   └── work-contract-form/
│       ├── work-contract-form.ts
│       └── work-contract-form.html
├── certification/
│   ├── services/
│   │   ├── certification.service.ts
│   │   └── certification-form.service.ts
│   ├── certification-table/
│   │   ├── certification-table.ts
│   │   └── certification-table.html
│   └── certification-form/
│       ├── certification-form.ts
│       └── certification-form.html
├── work-contract-page/
│   ├── work-contract-page.ts
│   └── work-contract-page.html
├── work-contract-detail-page/
│   ├── work-contract-detail-page.ts
│   └── work-contract-detail-page.html
└── contracts.routes.ts
```

### Modelos frontend nuevos

```typescript
// shared/models/work-contract.model.ts

export interface WorkContract {
  id?: number;
  contractNumber: string;
  clientId: number;
  clientBusinessName?: string;
  clientCuit?: string;
  projectAreaId?: number;
  projectAreaName?: string;
  projectAreaColor?: string;
  description: string;
  contractDate: string;
  endDate?: string | null;           // fecha de vencimiento — opcional
  contractedAmount: number;
  currency: Currency;
  status: WorkContractStatus;
  comment?: string;
  deleted?: boolean;
}

export enum WorkContractStatus {
  ACTIVO     = 'ACTIVO',
  SUSPENDIDO = 'SUSPENDIDO',
  FINALIZADO = 'FINALIZADO'
}

export enum Currency {
  ARS = 'ARS',
  USD = 'USD'
}

export interface WorkContractStats {
  contractedAmount:      number;
  // Certificación
  totalCertified:        number;
  pendingToCertify:      number;
  certificationProgress: number;  // %
  // Facturación
  totalInvoiced:         number;
  pendingToInvoice:      number;
  invoicingProgress:     number;  // %
  // Cobro
  totalCollected:        number;
  pendingToCollect:      number;
  collectionProgress:    number;  // %
}

export interface WorkContractFilters {
  clientId?: number | null;
  clientBusinessName?: string;
  contractNumber?: string;
  projectAreaId?: number | null;
  status?: WorkContractStatus | null;
  currency?: Currency | null;
  contractDateFrom?: string;
  contractDateTo?: string;
  minContractedAmount?: number | null;
  maxContractedAmount?: number | null;
  search?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}
```

```typescript
// shared/models/certification.model.ts

export interface Certification {
  id?: number;
  certificationNumber?: number;       // asignado por el backend al crear
  workContractId: number;
  contractNumber?: string;
  certificationDate: string;
  certifiedAmount: number;
  salesDocumentId?: number | null;
  salesDocumentLabel?: string;        // "FACTURA_A 0001-00000123 — $2.500.000"
  status: CertificationStatus;
  comment?: string;
  deleted?: boolean;
}

export enum CertificationStatus {
  PRESENTADO = 'PRESENTADO',
  APROBADO   = 'APROBADO',
  FACTURADO  = 'FACTURADO',
  COBRADO    = 'COBRADO'
}

export const CertificationStatusLabels: Record<CertificationStatus, string> = {
  [CertificationStatus.PRESENTADO]: 'Presentado',
  [CertificationStatus.APROBADO]:   'Aprobado',
  [CertificationStatus.FACTURADO]:  'Facturado',
  [CertificationStatus.COBRADO]:    'Cobrado'
};

export const CertificationStatusColors: Record<CertificationStatus, string> = {
  [CertificationStatus.PRESENTADO]: 'gray',
  [CertificationStatus.APROBADO]:   'blue',
  [CertificationStatus.FACTURADO]:  'orange',
  [CertificationStatus.COBRADO]:    'green'
};

export interface CertificationFilters {
  workContractId?: number | null;
  clientId?: number | null;
  status?: CertificationStatus | null;
  dateFrom?: string;
  dateTo?: string;
  hasInvoice?: boolean | null;
}
```

### Rutas: `contracts.routes.ts`

```typescript
export const CONTRACTS_ROUTES: Routes = [
  {
    path: '',
    component: ContractsPage,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./work-contract-page/work-contract-page').then(m => m.WorkContractPage)
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./work-contract-detail-page/work-contract-detail-page')
            .then(m => m.WorkContractDetailPage)
      }
    ]
  }
];
```

### Registro en `app.routes.ts`

```typescript
{
  path: 'contratos',
  loadChildren: () =>
    import('./domains/contracts/contracts.routes').then(m => m.CONTRACTS_ROUTES),
  canActivate: [RoleGuard],
  data: { requiredPermissions: ['WORK_CONTRACT_READ'] }
}
```

---

### Componente: `work-contract-table`

- Extiende `BaseTableDirective<WorkContract>`.
- Columnas: N° Contrato, Cliente, Área, Monto Contratado, Moneda, Fecha, Vencimiento (si existe), Estado.
- El campo **Vencimiento** se muestra en rojo si `endDate < hoy` y el contrato está en estado `ACTIVO`.
- Estado como chip: verde = Activo, naranja = Suspendido, gris = Finalizado.
- Filtros: cliente, área, status, moneda, rango montos, rango fechas, search.
- Clic en fila → navega a `/contratos/:id`.

### Componente: `work-contract-form`

Formulario popup. Campos:

| Campo | Tipo | Obligatorio |
|---|---|---|
| N° de Contrato | text | Sí |
| Cliente | autocomplete (Suppliers con isClient=true) | Sí |
| Área de Proyecto | select | No |
| Descripción | textarea | Sí |
| Fecha del Contrato | date picker | Sí |
| Fecha de Vencimiento | date picker | **No** |
| Monto Contratado | number | Sí |
| Moneda | select (ARS / USD) | Sí |
| Estado | select | Sí |
| Comentario | textarea | No |

---

### Componente: `work-contract-detail-page` — diseño de la vista

Esta es la pantalla más importante del módulo. El panel de seguimiento financiero ocupa la zona central y debe leerse de un vistazo.

#### Layout propuesto

```
┌────────────────────────────────────────────────────────────────┐
│  ← Volver   |  Contrato N° 2025-003  |  [Editar]  [ACTIVO ▼]  │
│  Cliente: Municipalidad de Luján  |  Área: Obra Vial Norte     │
│  Fecha: 15/03/2025  |  Vencimiento: 31/12/2025                 │
│  Descripción: Pavimentación calle Belgrano tramo 3             │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  MONTO CONTRATADO                                              │
│  ARS $  10.000.000,00                                          │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  CERTIFICADO      7.500.000   ████████████░░░  75%       │  │
│  │  Sin certificar   2.500.000                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  FACTURADO        5.000.000   ██████████░░░░░  67%       │  │
│  │  Sin facturar     2.500.000   (de lo certificado)        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  COBRADO          3.000.000   ████████░░░░░░░  60%       │  │
│  │  Por cobrar       2.000.000   (de lo facturado)          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
├────────────────────────────────────────────────────────────────┤
│  Certificaciones de Avance                  [+ Nueva Certif.]  │
│  ┌──┬──────────┬──────────────┬─────────────┬────────────────┐ │
│  │N°│  Fecha   │    Monto     │ Comprobante │    Estado      │ │
│  ├──┼──────────┼──────────────┼─────────────┼────────────────┤ │
│  │ 1│15/04/2025│ $2.500.000   │ FA 0001-001 │ ● Cobrado      │ │
│  │ 2│15/05/2025│ $2.500.000   │ FA 0001-002 │ ● Facturado    │ │
│  │ 3│15/06/2025│ $2.500.000   │ —           │ ○ Aprobado     │ │
│  └──┴──────────┴──────────────┴─────────────┴────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

#### Template (estructura simplificada)

```html
<div class="contract-detail-page">

  <!-- Header -->
  <header class="contract-header">
    <button (click)="goBack()">← Volver a contratos</button>
    <h2>Contrato N° {{ contract().contractNumber }}</h2>
    <div class="contract-header__actions">
      <button *appHasPermission="'WORK_CONTRACT_WRITE'" (click)="editContract()">Editar</button>
      <app-status-chip [status]="contract().status" />
    </div>
    <p>{{ contract().clientBusinessName }} | {{ contract().projectAreaName }}</p>
    <p>Fecha: {{ contract().contractDate | date:'dd/MM/yyyy' }}
      @if (contract().endDate) {
        | Vencimiento:
        <span [class.overdue]="isOverdue()">{{ contract().endDate | date:'dd/MM/yyyy' }}</span>
      }
    </p>
  </header>

  <!-- Panel financiero central -->
  <section class="contract-finance-panel">

    <div class="contract-finance-panel__contracted">
      <span class="label">Monto Contratado</span>
      <strong>{{ contract().currency }} $ {{ stats().contractedAmount | number:'1.2-2' }}</strong>
    </div>

    <!-- Bloque certificación -->
    <div class="finance-block">
      <div class="finance-block__header">
        <span class="finance-block__label">Certificado</span>
        <strong>$ {{ stats().totalCertified | number:'1.2-2' }}</strong>
        <span class="finance-block__pct">{{ stats().certificationProgress | number:'1.0-1' }}%</span>
      </div>
      <div class="progress-bar">
        <div class="progress-bar__fill progress-bar__fill--certified"
             [style.width]="stats().certificationProgress + '%'"></div>
      </div>
      <span class="finance-block__pending">
        Sin certificar: $ {{ stats().pendingToCertify | number:'1.2-2' }}
      </span>
    </div>

    <!-- Bloque facturación -->
    <div class="finance-block">
      <div class="finance-block__header">
        <span class="finance-block__label">Facturado</span>
        <strong>$ {{ stats().totalInvoiced | number:'1.2-2' }}</strong>
        <span class="finance-block__pct">{{ stats().invoicingProgress | number:'1.0-1' }}%</span>
      </div>
      <div class="progress-bar">
        <div class="progress-bar__fill progress-bar__fill--invoiced"
             [style.width]="stats().invoicingProgress + '%'"></div>
      </div>
      <span class="finance-block__pending">
        Sin facturar: $ {{ stats().pendingToInvoice | number:'1.2-2' }}
      </span>
    </div>

    <!-- Bloque cobro -->
    <div class="finance-block">
      <div class="finance-block__header">
        <span class="finance-block__label">Cobrado</span>
        <strong>$ {{ stats().totalCollected | number:'1.2-2' }}</strong>
        <span class="finance-block__pct">{{ stats().collectionProgress | number:'1.0-1' }}%</span>
      </div>
      <div class="progress-bar">
        <div class="progress-bar__fill progress-bar__fill--collected"
             [style.width]="stats().collectionProgress + '%'"></div>
      </div>
      <span class="finance-block__pending">
        Por cobrar: $ {{ stats().pendingToCollect | number:'1.2-2' }}
      </span>
    </div>

  </section>

  <!-- Tabla de certificaciones -->
  <section class="contract-certifications">
    <h3>Certificaciones de Avance</h3>
    <app-certification-table
      [workContractId]="contractId()"
      [clientId]="contract().clientId"
      (onNewClick)="openCertificationForm()" />
    @if (showCertificationForm()) {
      <app-certification-form
        [workContractId]="contractId()"
        [clientId]="contract().clientId"
        (onFormClosed)="closeCertificationForm()" />
    }
  </section>

</div>
```

### Componente: `certification-table`

- `@Input() workContractId: number` — requerido.
- `@Input() clientId: number` — para pre-filtrar el picker de SalesDocument.
- Columnas: N°, Fecha, Monto Certificado, Comprobante vinculado (o "—"), Estado.
- Estado como chip con colores. **El estado se puede cambiar directamente desde la tabla** con un select inline o dropdown (sin modal).
- El comprobante vinculado se puede asignar desde la tabla con un botón "Vincular" que abre el picker, o desde el formulario.
- Acciones por fila: editar (popup form), eliminar.
- No hay restricciones de qué acciones se pueden realizar según el estado actual.

### Componente: `certification-form`

Formulario popup.

| Campo | Tipo | Obligatorio |
|---|---|---|
| Fecha | date picker | Sí |
| Monto Certificado | number | Sí |
| Estado | select libre (cualquier valor) | Sí |
| Comprobante de Venta | picker filtrado por clientId | No |
| Comentario | textarea | No |

**Picker de SalesDocument:**
- Llama a `GET /api/v1/sales-documents?clientId={clientId}` con debounce.
- Muestra: tipo + número + fecha + total.
- Totalmente opcional — se puede dejar vacío en cualquier estado.

---

## Resumen de cambios por capa

| Capa | Archivos nuevos | Archivos modificados |
|---|---|---|
| **DB Migration** | `V23__add_work_contracts_and_certifications.sql` | — |
| **Enum** | `WorkContractStatus.java`, `Currency.java`, `CertificationStatus.java` | — |
| **Entity** | `WorkContract.java`, `Certification.java` | — |
| **DTO** | `WorkContractDTO`, `WorkContractResponseDTO`, `WorkContractFilterDTO`, `WorkContractStatsDTO`, `CertificationDTO`, `CertificationResponseDTO`, `CertificationFilterDTO`, `CertificationStatsProjection` | — |
| **Mapper** | `WorkContractMapper.java`, `CertificationMapper.java` | — |
| **Repository** | `WorkContractRepository.java`, `CertificationRepository.java` | — |
| **Service** | `IWorkContractService`, `WorkContractService`, `ICertificationService`, `CertificationService` | — |
| **Controller** | `WorkContractController.java`, `CertificationController.java` | — |
| **Frontend model** | `work-contract.model.ts`, `certification.model.ts` | — |
| **Frontend service** | `work-contract.service.ts`, `work-contract-form.service.ts`, `certification.service.ts`, `certification-form.service.ts` | — |
| **Frontend components** | `contracts-page`, `work-contract-page`, `work-contract-table`, `work-contract-form`, `work-contract-detail-page`, `certification-table`, `certification-form` | `app.routes.ts` (+ruta `/contratos`) |
