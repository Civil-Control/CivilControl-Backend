# Feature 2 — Vinculación de Comprobantes de Compra a Registros del Sistema

## Contexto

El sistema gestiona comprobantes de compra (`TransactionalDocument`) de forma independiente a los gastos que los originan. Esta feature agrega una FK nullable en cuatro entidades (`FuelLoad`, `Repair`, `SalaryPayment`, `Stock`) para vincular opcionalmente un comprobante a cada registro. La vinculación es **1:1 por registro** (un comprobante por carga, reparación, pago o ítem de stock) y completamente opcional.

La vinculación funciona en **dos sentidos**:

- **Desde el registro hacia el comprobante:** al crear o editar una carga de combustible, reparación, pago de haberes o compra de stock, el usuario puede buscar y seleccionar el comprobante que lo respalda mediante un componente reutilizable `DocumentPickerComponent`.
- **Desde el comprobante hacia el registro:** al visualizar un comprobante, el usuario puede ver todos los registros ya vinculados a él y crear nuevos registros directamente desde ahí, con el comprobante pre-seleccionado.

---

## Modelo de datos — cambios

### Entidades modificadas

Agregar campo nullable a cada una:

| Entidad | Tabla | Campo nuevo | Tipo |
|---|---|---|---|
| `FuelLoad` | `fuel_loads` | `transactionalDocument` | `@ManyToOne` FK → `TransactionalDocument` (nullable) |
| `Repair` | `repairs` | `transactionalDocument` | `@ManyToOne` FK → `TransactionalDocument` (nullable) |
| `SalaryPayment` | `salary_payments` | `transactionalDocument` | `@ManyToOne` FK → `TransactionalDocument` (nullable) |
| `Stock` | `stock` | `transactionalDocument` | `@ManyToOne` FK → `TransactionalDocument` (nullable) |

Anotación en cada entidad:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "transactional_document_id")
private TransactionalDocument transactionalDocument;
```

### Nuevo DTO compartido: `TransactionalDocumentSummaryDTO`

Para incluir en los response DTOs sin cargar el documento completo:
```java
public record TransactionalDocumentSummaryDTO(
    Long id,
    String documentType,
    String branchCode,
    String documentNumber,
    String supplierName,
    BigDecimal total,
    LocalDate date
) {}
```

---

## DTOs — cambios

### `FuelLoadDTO` (request)
Agregar campo:
```java
@Positive Long transactionalDocumentId  // nullable, sin @NotNull
```

### `FuelLoadResponseDTO` (response)
Agregar campo:
```java
TransactionalDocumentSummaryDTO transactionalDocument  // null si no hay vínculo
```

### `RepairDTO` (request)
Agregar campo:
```java
@Positive Long transactionalDocumentId  // nullable
```

### `RepairResponseDTO` (response)
Agregar campo:
```java
TransactionalDocumentSummaryDTO transactionalDocument  // null si no hay vínculo
```
> **Nota:** `RepairResponseDTO` ya fue extendido en Feature 5 con `repairOrder`. Se agrega un campo más.

### `SalaryPaymentDTO` (request)
Agregar campo:
```java
@Positive Long transactionalDocumentId  // nullable
```

### `SalaryPaymentResponseDTO` (response)
Agregar campo:
```java
TransactionalDocumentSummaryDTO transactionalDocument  // null si no hay vínculo
```

### `StockDTO` (request)
Agregar campo:
```java
@Positive Long transactionalDocumentId  // nullable
```

### `StockResponseDTO` (response)
Agregar campo:
```java
TransactionalDocumentSummaryDTO transactionalDocument  // null si no hay vínculo
```

---

## Mappers — cambios

En cada uno de los cuatro mappers (`FuelLoadMapper`, `RepairMapper`, `SalaryPaymentMapper`, `StockMapper`):

### En `toEntity(DTO → Entity)` / `partialUpdate()`

Agregar helper `@Named("documentIdToEntity")` (o reutilizar el mismo patrón que `vehicleIdToEntity` ya presente en `RepairMapper`):
```java
@Named("documentIdToEntity")
default TransactionalDocument documentIdToEntity(Long id) {
    if (id == null) return null;
    TransactionalDocument doc = new TransactionalDocument();
    doc.setId(id);
    return doc;
}
```

Agregar mapping en `toEntity`:
```java
@Mapping(target = "transactionalDocument", source = "transactionalDocumentId", qualifiedByName = "documentIdToEntity")
```

### En `toResponseDto(Entity → ResponseDTO)`

Agregar mapping:
```java
@Mapping(target = "transactionalDocument.supplierName", source = "transactionalDocument.supplier.legalName")
TransactionalDocumentSummaryDTO toSummaryDto(TransactionalDocument doc);
```

---

## Servicios — cambios

No se crean servicios nuevos. Solo se actualiza la lógica de validación en los cuatro servicios existentes para resolver el `TransactionalDocument` por ID si viene en el DTO:

```java
// Patrón a aplicar en cada ServiceImpl (FuelLoadService, RepairService, SalaryPaymentService, StockService)
if (dto.transactionalDocumentId() != null) {
    TransactionalDocument doc = transactionalDocumentRepository
        .findByIdAndTenantId(dto.transactionalDocumentId(), tenantId)
        .orElseThrow(() -> new EntityNotFoundException("Comprobante no encontrado"));
    entity.setTransactionalDocument(doc);
} else {
    entity.setTransactionalDocument(null);
}
```

---

## Repositories — cambios

Agregar método de búsqueda por ID + tenantId a `TransactionalDocumentRepository`:
```java
Optional<TransactionalDocument> findByIdAndTenantId(Long id, Long tenantId);
```
> Verificar si ya existe. Si el repositorio extiende `JpaRepository` con `TenantEntity`, puede ya estar disponible vía Spring Data naming convention.

---

## Migración de base de datos — `V21`

```sql
-- V21__link_transactional_document_to_records.sql

ALTER TABLE fuel_loads
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);

ALTER TABLE repairs
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);

ALTER TABLE salary_payments
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);

ALTER TABLE stock
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);
```

---

## API — endpoints afectados

### Endpoints existentes modificados

Los endpoints `POST` y `PATCH` de los cuatro recursos aceptan el nuevo campo opcional `transactionalDocumentId` en el body. Los endpoints `GET` devuelven el nuevo campo `transactionalDocument` (summary) en la respuesta.

| Recurso | Endpoint afectado |
|---|---|
| FuelLoad | `POST /api/v1/fuel-loads`, `PATCH /api/v1/fuel-loads/{id}`, `GET /api/v1/fuel-loads`, `GET /api/v1/fuel-loads/{id}` |
| Repair | `POST /api/v1/repairs`, `PATCH /api/v1/repairs/{id}`, `GET /api/v1/repairs`, `GET /api/v1/repairs/{id}` |
| SalaryPayment | `POST /api/v1/salary-payments`, `PATCH /api/v1/salary-payments/{id}`, `GET /api/v1/salary-payments`, `GET /api/v1/salary-payments/{id}` |
| Stock | `POST /api/v1/stock`, `PATCH /api/v1/stock/{id}`, `GET /api/v1/stock`, `GET /api/v1/stock/{id}` |

### Nuevo parámetro de filtro en endpoints GET

Para poder consultar desde el comprobante qué registros están vinculados a él, se agrega `transactionalDocumentId` como filtro opcional en los cuatro endpoints de listado:

- `GET /api/v1/fuel-loads?transactionalDocumentId={id}`
- `GET /api/v1/repairs?transactionalDocumentId={id}`
- `GET /api/v1/salary-payments?transactionalDocumentId={id}`
- `GET /api/v1/stock?transactionalDocumentId={id}`

Esto requiere agregar el parámetro a cada `FilterDTO` correspondiente y a la `@Query` JPQL del repository.

---

## Frontend

### Nuevo componente compartido: `DocumentPickerComponent`

**Ubicación:** `src/app/shared/components/document-picker/`

**Archivos:**
- `document-picker.ts`
- `document-picker.html`
- `document-picker.scss`

**Comportamiento:**
- Campo de texto para buscar comprobantes (por número, proveedor, CUIT).
- Busca con debounce de 300ms llamando a `TransactionalDocumentService.getAll()`.
- Muestra dropdown con hasta 20 resultados.
- Cada opción del dropdown muestra: tipo + número de comprobante (`FACTURA A 00001-00012345`), nombre del proveedor, total, fecha.
- Soporte de teclado: Arrow Up/Down, Enter para seleccionar, Escape para cerrar.
- Si ya hay un documento seleccionado, muestra el resumen y un botón `×` para desvincular.

**Interfaz del componente:**
```typescript
@Component({ selector: 'app-document-picker', standalone: true })
export class DocumentPickerComponent {
  // Inputs
  @Input() selectedDocumentId: number | null = null;
  @Input() selectedDocumentSummary: TransactionalDocumentSummary | null = null;
  @Input() label: string = 'Comprobante vinculado';
  @Input() placeholder: string = 'Buscar por número o proveedor...';
  @Input() disabled: boolean = false;

  // Outputs
  @Output() documentSelected = new EventEmitter<TransactionalDocumentSummary | null>();

  // Internal state
  query = '';
  dropdownOpen = false;
  results: TransactionalDocumentSummary[] = [];
  activeIndex = -1;
  loading = false;
}
```

**Modelo frontend compartido:**
```typescript
// src/app/shared/models/transactional-document-summary.model.ts
export interface TransactionalDocumentSummary {
  id: number;
  documentType: string;
  branchCode: string;
  documentNumber: string;
  supplierName: string;
  total: number;
  date: string;
}
```

**Lógica de búsqueda:**
```typescript
// ngOnInit: subscribe a query con debounce
private searchSubject = new Subject<string>();

ngOnInit() {
  this.searchSubject.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    switchMap(q => {
      if (!q || q.length < 2) return of([]);
      this.loading = true;
      return this.documentService.getAll({ size: '20', page: '0' }, { search: q });
    })
  ).subscribe({
    next: (res: any) => {
      this.results = res?.content ?? [];
      this.dropdownOpen = this.results.length > 0;
      this.loading = false;
    }
  });
}

onQueryChange(value: string) {
  this.query = value;
  if (!value) { this.clearSelection(); return; }
  this.searchSubject.next(value);
}
```

**Template HTML (estructura):**
```html
<div class="doc-picker">
  <label>{{ label }}</label>

  <!-- Si ya hay documento seleccionado -->
  @if (selectedDocumentSummary) {
    <div class="doc-picker__selected">
      <span class="doc-picker__tag">
        {{ selectedDocumentSummary.documentType }} {{ selectedDocumentSummary.branchCode }}-{{ selectedDocumentSummary.documentNumber }}
        — {{ selectedDocumentSummary.supplierName }}
        — ${{ selectedDocumentSummary.total | number:'1.2-2' }}
      </span>
      <button type="button" class="doc-picker__clear" (click)="clearSelection()">×</button>
    </div>
  }

  <!-- Campo de búsqueda (solo si no hay selección) -->
  @if (!selectedDocumentSummary) {
    <input
      type="text"
      [placeholder]="placeholder"
      [(ngModel)]="query"
      (input)="onQueryChange(query)"
      (keydown)="onKeydown($event)"
      autocomplete="off" />

    @if (loading) { <span class="doc-picker__spinner">Buscando...</span> }

    @if (dropdownOpen) {
      <ul class="doc-picker__dropdown">
        @for (doc of results; track doc.id; let i = $index) {
          <li
            [id]="'dp-opt-' + i"
            [class.active]="i === activeIndex"
            (click)="selectDocument(doc)">
            <span class="doc-picker__num">{{ doc.documentType }} {{ doc.branchCode }}-{{ doc.documentNumber }}</span>
            <span class="doc-picker__supplier">{{ doc.supplierName }}</span>
            <span class="doc-picker__meta">{{ doc.date }} · ${{ doc.total | number:'1.2-2' }}</span>
          </li>
        }
      </ul>
    }
  }
</div>
```

---

### Modelos frontend — cambios

#### `FuelLoad` interface (`fuel.model.ts`)
Agregar campos:
```typescript
transactionalDocumentId?: number | null;
transactionalDocument?: TransactionalDocumentSummary | null;
```

#### `Repair` interface (`mechanic.model.ts`)
Agregar campos:
```typescript
transactionalDocumentId?: number | null;
transactionalDocument?: TransactionalDocumentSummary | null;
```

#### `SalaryPayment` interface (`salary-payment.model.ts`)
Agregar campos:
```typescript
transactionalDocumentId?: number | null;
transactionalDocument?: TransactionalDocumentSummary | null;
```

#### `Stock` interface (`stock.model.ts`)
Agregar campos:
```typescript
transactionalDocumentId?: number | null;
transactionalDocument?: TransactionalDocumentSummary | null;
```

---

### Formularios — cambios

En cada uno de los cuatro formularios afectados se agrega:

1. **Propiedad de estado:**
```typescript
selectedDocument: TransactionalDocumentSummary | null = null;
```

2. **Handler de selección:**
```typescript
onDocumentSelected(doc: TransactionalDocumentSummary | null) {
  this.selectedDocument = doc;
}
```

3. **En `loadX()` (modo edición):** poblar `selectedDocument` desde los datos del registro:
```typescript
this.selectedDocument = x.transactionalDocument ?? null;
```

4. **En `submit()`:** incluir `transactionalDocumentId` en el body:
```typescript
transactionalDocumentId: this.selectedDocument?.id ?? null
```

5. **En el template:** agregar el componente `<app-document-picker>` como una nueva fila en el formulario:
```html
<div class="pf-row">
  <div class="pf-field pf-field--full">
    <app-document-picker
      [selectedDocumentId]="selectedDocument?.id ?? null"
      [selectedDocumentSummary]="selectedDocument"
      (documentSelected)="onDocumentSelected($event)" />
  </div>
</div>
```

#### Detalle de cambios por formulario

| Formulario | Sección donde se agrega |
|---|---|
| `fuel-load-form` | Nueva sección al final del formulario general ("Datos Generales"), antes del listado de ítems. La vinculación aplica a **nivel del batch completo** (una carga = un comprobante). En modo edición individual, se muestra dentro del ítem. **Decisión de diseño:** en modo creación batch, el picker se ubica en la sección "Datos Generales" y aplica a todos los ítems del batch. |
| `repair-form` | Nueva fila al final del formulario, dentro de la sección existente. Visible principalmente cuando hay un `supplier` seleccionado. |
| `salary-paymemt-form` | Por ser multi-ítem, el picker se agrega **dentro de cada ítem** (`mi-item-card`) como última columna, igual que los otros campos por ítem. |
| `stock-form` | Nueva fila al final del formulario, dentro de `pf-section`. |

---

### Vistas de detalle de registros — cambios

En cada detail component (`fuel-load-detail`, `repair-detail`, `salary-payment-detail`, `stock-detail`) agregar sección condicional:

```html
@if (item().transactionalDocument) {
  <div class="detail-section">
    <h4>Comprobante Vinculado</h4>
    <p>
      <strong>Número:</strong>
      {{ item().transactionalDocument.documentType }}
      {{ item().transactionalDocument.branchCode }}-{{ item().transactionalDocument.documentNumber }}
    </p>
    <p><strong>Proveedor:</strong> {{ item().transactionalDocument.supplierName }}</p>
    <p><strong>Fecha:</strong>     {{ item().transactionalDocument.date }}</p>
    <p><strong>Total:</strong>     $ {{ item().transactionalDocument.total | number:'1.2-2' }}</p>
  </div>
}
```

---

## Vinculación desde el lado del comprobante

### Concepto

Además de poder vincular un comprobante desde cada formulario de registro, el usuario también puede abrir un comprobante y ver desde ahí qué registros están respaldados por él — y crear nuevos directamente desde esa vista. Esto es especialmente útil cuando se recibe una factura que cubre varios gastos, o cuando el flujo de trabajo parte del comprobante y no del registro.

### Cambio en `transactional-document-detail`

Se agrega una nueva sección al final de la vista de detalle del comprobante: **"Registros Vinculados"**.

Esta sección contiene cuatro sub-secciones colapsables, una por tipo de registro. Cada sub-sección:
1. Al expandirse, consulta el servicio correspondiente filtrando por `transactionalDocumentId`.
2. Muestra un listado compacto de los registros ya vinculados (si los hay).
3. Ofrece un botón **"+ Agregar [tipo]"** que abre el formulario correspondiente con el comprobante pre-seleccionado y bloqueado (no editable en el picker).

**Estructura de la sección en el template:**

```html
<section class="linked-records">
  <h3>Registros Vinculados</h3>

  <!-- Sub-sección: Cargas de Combustible -->
  <div class="linked-records__group">
    <button (click)="toggleSection('fuel')">
      Cargas de Combustible
      <span class="badge">{{ fuelLoadsCount() }}</span>
    </button>
    @if (sectionOpen('fuel')) {
      @if (loadingFuel()) { <span>Cargando...</span> }
      @for (fl of linkedFuelLoads(); track fl.id) {
        <div class="linked-record-item">
          <span>{{ fl.vehicleLicensePlate }} — {{ fl.date }} — {{ fl.liters }}L</span>
        </div>
      }
      <button class="btn-add" (click)="openFuelLoadForm()">+ Agregar carga de combustible</button>
    }
  </div>

  <!-- Sub-sección: Reparaciones -->
  <div class="linked-records__group">
    <button (click)="toggleSection('repair')">
      Reparaciones
      <span class="badge">{{ repairsCount() }}</span>
    </button>
    @if (sectionOpen('repair')) {
      @for (r of linkedRepairs(); track r.id) {
        <div class="linked-record-item">
          <span>{{ r.vehicleLicensePlate }} — {{ r.date }} — {{ r.repairTypes.join(', ') }}</span>
        </div>
      }
      <button class="btn-add" (click)="openRepairForm()">+ Agregar reparación</button>
    }
  </div>

  <!-- Sub-sección: Pagos de Haberes -->
  <div class="linked-records__group">
    <button (click)="toggleSection('salary')">
      Pagos de Haberes
      <span class="badge">{{ salaryPaymentsCount() }}</span>
    </button>
    @if (sectionOpen('salary')) {
      @for (sp of linkedSalaryPayments(); track sp.id) {
        <div class="linked-record-item">
          <span>{{ sp.employeeLastName }}, {{ sp.employeeName }} — {{ sp.paymentDate }} — ${{ sp.amount | number:'1.2-2' }}</span>
        </div>
      }
      <button class="btn-add" (click)="openSalaryPaymentForm()">+ Agregar pago de haberes</button>
    }
  </div>

  <!-- Sub-sección: Compras de Stock -->
  <div class="linked-records__group">
    <button (click)="toggleSection('stock')">
      Compras de Stock
      <span class="badge">{{ stockCount() }}</span>
    </button>
    @if (sectionOpen('stock')) {
      @for (s of linkedStock(); track s.id) {
        <div class="linked-record-item">
          <span>{{ s.name }} — {{ s.quantity }} uds — {{ s.stockCategory }}</span>
        </div>
      }
      <button class="btn-add" (click)="openStockForm()">+ Agregar compra de stock</button>
    }
  </div>
</section>
```

### Lógica en `transactional-document-detail.ts`

```typescript
linkedFuelLoads      = signal<FuelLoad[]>([]);
linkedRepairs        = signal<Repair[]>([]);
linkedSalaryPayments = signal<SalaryPayment[]>([]);
linkedStock          = signal<Stock[]>([]);
openSections         = signal<Set<string>>(new Set());
loadingFuel          = signal(false);
// ... (idem para los otros tipos)

fuelLoadsCount    = computed(() => this.linkedFuelLoads().length);
// ...

toggleSection(key: string) {
  const sections = new Set(this.openSections());
  if (sections.has(key)) { sections.delete(key); }
  else { sections.add(key); this.loadLinkedRecords(key); }
  this.openSections.set(sections);
}

loadLinkedRecords(key: string) {
  const docId = this.document().id;
  switch (key) {
    case 'fuel':
      this.loadingFuel.set(true);
      this.fuelLoadService.getAll({ size: '100', page: '0' }, { transactionalDocumentId: docId })
        .subscribe({ next: (res: any) => {
          this.linkedFuelLoads.set(res?.content ?? []);
          this.loadingFuel.set(false);
        }});
      break;
    case 'repair': /* análogo */ break;
    case 'salary': /* análogo */ break;
    case 'stock':  /* análogo */ break;
  }
}

openFuelLoadForm()       { this.fuelLoadFormService.openWithDocument(this.document()); }
openRepairForm()         { this.repairFormService.openWithDocument(this.document()); }
openSalaryPaymentForm()  { this.salaryPaymentFormService.openWithDocument(this.document()); }
openStockForm()          { this.stockFormService.openWithDocument(this.document()); }
```

### Cambios en los form services

Cada form service (`FuelLoadFormService`, `RepairFormService`, `SalaryPaymentFormService`, `StockFormService`) agrega:

```typescript
private _openWithDocument$ = new Subject<TransactionalDocumentSummary>();
openWithDocument$ = this._openWithDocument$.asObservable();

openWithDocument(doc: TransactionalDocumentSummary) {
  this._openWithDocument$.next(doc);
}
```

Cada form component suscribe a `openWithDocument$` en `ngOnInit`, pre-carga `selectedDocument` y deshabilita el picker:

```typescript
this.formService.openWithDocument$.subscribe(doc => {
  this.selectedDocument = doc;
  this.documentPickerDisabled = true;
});
```

El `DocumentPickerComponent` con `disabled = true` muestra el documento seleccionado sin el botón `×`.

### Flujo de apertura cross-dominio

```
transactional-document-detail
  └─ openFuelLoadForm()
       └─ fuelLoadFormService.openWithDocument(doc)
            └─ [en fuel-load-page] suscribe a openWithDocument$
                 └─ muestra popup fuel-load-form con doc pre-cargado y locked
                      └─ al guardar → linkedFuelLoads recarga vía loadLinkedRecords('fuel')
```

> El detalle del comprobante no renderiza directamente los formularios de otros dominios. Usa los form services como canal inter-dominio, manteniendo la separación de responsabilidades del patrón DDD existente.

---

## Resumen de cambios por capa

| Capa | Archivos nuevos | Archivos modificados |
|---|---|---|
| **DB Migration** | `V21__link_transactional_document_to_records.sql` | — |
| **Entity** | — | `FuelLoad.java`, `Repair.java`, `SalaryPayment.java`, `Stock.java` (+1 campo cada uno) |
| **DTO** | `TransactionalDocumentSummaryDTO.java` | `FuelLoadDTO`, `FuelLoadResponseDTO`, `FuelLoadFilterDTO`, `RepairDTO`, `RepairResponseDTO`, `RepairFilterDTO`, `SalaryPaymentDTO`, `SalaryPaymentResponseDTO`, `SalaryPaymentFilterDTO`, `StockDTO`, `StockResponseDTO`, `StockFilterDTO` (+1-2 campos cada uno) |
| **Mapper** | — | `FuelLoadMapper`, `RepairMapper`, `SalaryPaymentMapper`, `StockMapper` (+helper + mapping) |
| **Repository** | — | `TransactionalDocumentRepository` (+1 método si no existe), `FuelLoadRepository`, `RepairRepository`, `SalaryPaymentRepository`, `StockRepository` (+cláusula filtro en `@Query`) |
| **Service** | — | `FuelLoadService`, `RepairService`, `SalaryPaymentService`, `StockService` (+resolución de FK) |
| **Controller** | — | `FuelLoadController`, `RepairController`, `SalaryPaymentController`, `StockController` (+parámetro `transactionalDocumentId` en GET) |
| **Frontend model** | `transactional-document-summary.model.ts` | `fuel.model.ts`, `mechanic.model.ts`, `salary-payment.model.ts`, `stock.model.ts` (+2 campos cada uno) |
| **Frontend form services** | — | `FuelLoadFormService`, `RepairFormService`, `SalaryPaymentFormService`, `StockFormService` (+`openWithDocument$`) |
| **Frontend forms** | — | `fuel-load-form`, `repair-form`, `salary-paymemt-form`, `stock-form` (+picker + `documentPickerDisabled` + suscripción) |
| **Frontend details (registros)** | — | `fuel-load-detail`, `repair-detail`, `salary-payment-detail`, `stock-detail` (+sección comprobante vinculado) |
| **Frontend detail (comprobante)** | — | `transactional-document-detail` (+sección "Registros Vinculados", queries lazy, apertura cross-dominio) |
| **Frontend component** | `document-picker/` (ts + html + scss) | — |
