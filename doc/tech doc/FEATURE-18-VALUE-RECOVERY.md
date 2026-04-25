# Feature 18 — Recupero de Valores (Funcionalidad Oculta)

## Resumen

Funcionalidad **oculta** que permite a empresas con esquemas de recupero (cesiones, reintegros, acuerdos con casa matriz, devoluciones de programas estatales, etc.) **acreditar automáticamente a una caja** un porcentaje del neto + el IVA total de las facturas A registradas contra un proveedor previamente configurado.

### Activación oculta
La feature **NO aparece nunca en menús ni pantallas de configuración global**. Se activa de forma implícita cuando el usuario crea un `ProjectArea` cuyo `name` (case-insensitive, trimeado) sea exactamente **`"Recupero"`**. Una vez creada, la entidad obtiene un flag persistente `isRecoverySector = true`. A partir de ese momento:

- En el detalle de **ese** sector aparece una sección extra: "Configuración de Recupero" con la tabla de proveedores asociados.
- Cualquier `TransactionalDocument` que el usuario asocie a ese sector (y que sea **factura tipo A**) y cuyo proveedor esté configurado, dispara automáticamente un movimiento positivo en la `CashBox` configurada.
- El sector puede después renombrarse libremente — el flag persiste, así que la funcionalidad sigue activa. Esto evita la fragilidad de depender del nombre exacto a perpetuidad.
- **Solo un sector por tenant puede tener `isRecoverySector = true`**. Si ya existe uno y el usuario crea otro llamado "Recupero", se ignora la magia (segundo sector queda como sector normal con ese nombre).

### Fórmula del recupero (regla central)

$$\text{MontoRecuperado} = (\text{netoTotalFactura} \times \text{porcentajeProveedor}) + \text{IVATotalFactura}$$

- El **neto** de la factura ya incluye items + entidades linkeadas (Repair, FuelLoad, SalaryPayment, StockPurchase) — calculado por `DocumentTotalRecalculator` (ver `REPORTS-DEDUPLICATION-LOGIC.md`).
- El **IVA** se recupera al 100 % siempre (lógica típica de crédito fiscal recuperable).
- El porcentaje y la caja se **congelan al momento de generar el recupero** (snapshot). Cambios futuros en la config no afectan recuperos históricos.

> **Restricción dura:** Solo facturas **tipo A** pueden asociarse al sector de recupero. El frontend bloquea la selección del sector si el TD no es A; el backend valida lo mismo (ver §5.3).

---

## 1. Modelo de Datos

### 1.1 Modificación a `ProjectArea`

```java
@Entity
@Table(name = "project_areas")
public class ProjectArea extends TenantEntity {
    // ... campos existentes

    @Column(name = "is_recovery_sector", nullable = false)
    @Builder.Default
    private Boolean isRecoverySector = false;            // NUEVO — flag persistente
}
```

**Migración Liquibase:**
```sql
ALTER TABLE project_areas ADD COLUMN is_recovery_sector BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: marcar el primero por tenant que tenga nombre exacto "Recupero" (case-insensitive)
UPDATE project_areas pa
SET is_recovery_sector = TRUE
WHERE pa.id IN (
    SELECT MIN(pa2.id)
    FROM project_areas pa2
    WHERE LOWER(TRIM(pa2.name)) = 'recupero'
      AND pa2.deleted = FALSE
    GROUP BY pa2.tenant_id
);

-- Constraint: solo uno por tenant con flag activo
CREATE UNIQUE INDEX uk_project_areas_recovery
    ON project_areas(tenant_id)
    WHERE is_recovery_sector = TRUE;   -- partial index (PostgreSQL); en otros engines usar trigger
```

### 1.2 Entidad nueva: `RecoverySupplierConfig`

**Ubicación:** `model/entity/recovery/RecoverySupplierConfig.java`

```java
@Entity
@Table(name = "recovery_supplier_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "project_area_id", "supplier_id"})
})
public class RecoverySupplierConfig extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id", nullable = false)
    private ProjectArea projectArea;                    // siempre el sector con isRecoverySector=true

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "recovery_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal recoveryPercentage;              // 0.00 a 100.00

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_box_id", nullable = false)
    private CashBox cashBox;                            // caja destino del recupero (F15)

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;                      // permite desactivar sin borrar histórico
}
```

### 1.3 Entidad nueva: `RecoveryEvent`

Registro inmutable de cada operación de recupero generada. Permite trazabilidad y reverso preciso.

**Ubicación:** `model/entity/recovery/RecoveryEvent.java`

```java
@Entity
@Table(name = "recovery_events")
public class RecoveryEvent extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private RecoveryEventType eventType;                // GENERATED | REVERSED | ADJUSTED_CREDIT_NOTE

    // ── Origen del evento ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactional_document_id", nullable = false)
    private TransactionalDocument transactionalDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_document_id")
    private TransactionalDocument creditNoteDocument;   // solo si eventType == ADJUSTED_CREDIT_NOTE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverses_event_id")
    private RecoveryEvent reversesEvent;                // FK al evento que se está reversando (si aplica)

    // ── Snapshot de configuración ──
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_config_id", nullable = false)
    private RecoverySupplierConfig supplierConfig;      // la config usada (referencia auditable)

    @Column(name = "snapshot_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal snapshotPercentage;              // copia inmutable del % al momento

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_cash_box_id", nullable = false)
    private CashBox snapshotCashBox;                    // copia inmutable de la caja al momento

    // ── Datos calculados ──
    @Column(name = "document_net", nullable = false, precision = 19, scale = 2)
    private BigDecimal documentNet;                     // neto al momento del cálculo

    @Column(name = "document_iva", nullable = false, precision = 19, scale = 2)
    private BigDecimal documentIva;

    @Column(name = "recovered_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal recoveredAmount;                 // (net*%) + iva (positivo si GENERATED, negativo si REVERSED/ADJUSTED)

    // ── Trazabilidad ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_box_movement_id", nullable = false)
    private CashBoxMovement cashBoxMovement;            // FK al movimiento que generó/revirtió en la caja

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "triggered_by_user_id", nullable = false)
    private Long triggeredByUserId;
}
```

### 1.4 Enum: `RecoveryEventType`

```java
public enum RecoveryEventType {
    GENERATED,              // recupero positivo por una factura A registrada
    REVERSED,               // reverso completo por edición/eliminación de la factura
    ADJUSTED_CREDIT_NOTE    // reverso parcial por una nota de crédito
}
```

### 1.5 Extensión a `CashBoxMovementType` (de F15)

```java
public enum CashBoxMovementType {
    INCREMENTO_MANUAL,
    DECREMENTO_MANUAL,
    AJUSTE,
    PAGO_EMITIDO,
    TRANSFERENCIA_INTERNA,
    RECUPERO_AUTOMATICO,    // NUEVO — generado por RecoveryService
    RECUPERO_REVERSO        // NUEVO — reverso por edición/eliminación/nota de crédito
}
```

Estos tipos no son creables desde el endpoint manual de movimientos — solo se generan desde `RecoveryService`.

---

## 2. Validaciones del modelo

### 2.1 Sobre `RecoverySupplierConfig`
- El `projectArea` referenciado debe tener `isRecoverySector = true` (validado en service).
- El `cashBox` debe existir, estar activo y no eliminado.
- `recoveryPercentage` ∈ `[0, 100]` (decimal con 2 lugares).
- Un proveedor no puede repetirse para el mismo sector (`UniqueConstraint`).

### 2.2 Sobre la asociación `TransactionalDocument` ↔ sector "Recupero"
Cuando el usuario asigna `projectAreaId` a un `TransactionalDocument`, el service valida:
- Si el `ProjectArea` destino tiene `isRecoverySector = true`:
  - El TD **debe ser tipo factura A** (`document.documentType == FACTURA && document.fiscalLetter == 'A'`).
  - El TD **debe tener proveedor** (`document.supplier != null`).
  - El proveedor **debe estar configurado en `RecoverySupplierConfig`** activo para ese sector — sino se rechaza con mensaje claro.
  - Validaciones equivalentes al **mover** el TD a este sector después (update).

Si el TD ya estaba en el sector Recupero y se cambia a otro sector → se debe ejecutar reverso (ver §4.2).

---

## 3. DTOs

### 3.1 `RecoverySupplierConfigDTO` (request)
```java
public record RecoverySupplierConfigDTO(
    @NotNull Long supplierId,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal recoveryPercentage,
    @NotNull Long cashBoxId,
    Boolean active
) {}
```

### 3.2 `RecoverySupplierConfigResponseDTO`
```java
public record RecoverySupplierConfigResponseDTO(
    Long id,
    Long projectAreaId,
    String projectAreaName,
    Long supplierId,
    String supplierName,
    String supplierCuit,
    BigDecimal recoveryPercentage,
    Long cashBoxId,
    String cashBoxName,
    BigDecimal cashBoxCurrentBalance,
    Boolean active,
    int totalRecoveryEventsCount,                     // para mostrar uso histórico
    BigDecimal totalRecoveredAmount                   // suma de eventos GENERATED - REVERSED - ADJUSTED
) {}
```

### 3.3 `RecoveryEventResponseDTO`
```java
public record RecoveryEventResponseDTO(
    Long id,
    RecoveryEventType eventType,
    Long transactionalDocumentId,
    String transactionalDocumentReference,            // "Factura A 0001-00012345"
    Long creditNoteDocumentId,
    String creditNoteDocumentReference,
    Long reversesEventId,
    Long supplierId,
    String supplierName,
    BigDecimal snapshotPercentage,
    Long snapshotCashBoxId,
    String snapshotCashBoxName,
    BigDecimal documentNet,
    BigDecimal documentIva,
    BigDecimal recoveredAmount,
    Long cashBoxMovementId,
    LocalDateTime occurredAt,
    Long triggeredByUserId,
    String triggeredByUserName
) {}
```

### 3.4 Hook visual en `ProjectAreaResponseDTO`
Agregar campo:
```java
Boolean isRecoverySector;                             // habilita la sección extra en frontend
```

---

## 4. Servicio: `RecoveryService` (núcleo de la lógica)

**Ubicación:** `service/recovery/RecoveryService.java`

### 4.1 Generación de recupero (`generateForDocument`)

Invocado por `TransactionalDocumentService` después de:
- Crear un TD asignándole un sector con `isRecoverySector=true`.
- Actualizar un TD para asignarle ese sector (cuando antes estaba en otro).
- Update de un TD que ya está en el sector y cambia su monto.

```text
generateForDocument(TransactionalDocument doc):
  1. Si doc.projectArea no es recovery sector → no-op.
  2. Si doc.fiscalLetter != 'A' → throw RecoveryNotApplicableException (no debería llegar — validación previa)
  3. Resolver RecoverySupplierConfig por (sector, supplier, active=true). Si no existe → throw.
  4. Calcular:
        net = doc.netSubtotal
        iva = doc.totalIva
        recovered = net.multiply(config.percentage).divide(100).add(iva)
  5. Crear CashBoxMovement:
        type = RECUPERO_AUTOMATICO
        amount = recovered (positivo)
        movementDate = doc.documentDate (o today si null)
        comment = "Recupero automático — Factura " + doc.fullReference
        cashPayment = null
  6. Crear RecoveryEvent:
        eventType = GENERATED
        snapshotPercentage = config.percentage
        snapshotCashBox = config.cashBox
        documentNet = net, documentIva = iva, recoveredAmount = recovered
        cashBoxMovement = (paso 5)
  7. Recalcular saldo cacheado de la caja.
  8. Retornar el RecoveryEvent.
```

### 4.2 Reverso por edición / eliminación (`reverseForDocument`)

```text
reverseForDocument(TransactionalDocument doc, ReverseReason reason):
  1. Buscar último RecoveryEvent activo para doc.id (GENERATED no superado por REVERSED).
  2. Si no existe → no-op.
  3. Calcular reverseAmount = -lastEvent.recoveredAmount
  4. Crear CashBoxMovement type=RECUPERO_REVERSO con amount = reverseAmount, comment con motivo.
  5. Crear RecoveryEvent:
        eventType = REVERSED
        reversesEvent = lastEvent
        recoveredAmount = reverseAmount (negativo)
        snapshot* = copiados de lastEvent (NO se relee config)
  6. Recalcular saldo de la caja.
```

**Casos que disparan reverso:**

| Acción del usuario sobre el TD                    | Reverso |
|---------------------------------------------------|---------|
| Cambiar `projectArea` a otro sector               | ✓       |
| Cambiar `supplier`                                | ✓ (+ regenerar si nuevo supplier está configurado) |
| Cambiar `fiscalLetter` (ej. de A a otra)         | ✓ (en realidad bloquear edición, ver §5.3) |
| Eliminar (soft) el TD                             | ✓       |
| Modificar montos del TD (item details, linked entities) | ✓ + regenerar con nuevos montos |
| Cambiar fecha del TD                              | (solo si afecta el cálculo — no en este modelo) |

### 4.3 Reverso parcial por nota de crédito (`adjustForCreditNote`)

Invocado cuando se crea una nota de crédito asociada al sector Recupero, vinculada a una factura A previamente recuperada.

```text
adjustForCreditNote(TransactionalDocument creditNote):
  1. Validar: creditNote.relatedDocument != null y apunta a una factura A en sector Recupero.
  2. Buscar último RecoveryEvent GENERATED de la factura original (no totalmente reversado).
  3. Calcular ajuste proporcional:
        creditNet = creditNote.netSubtotal
        creditIva = creditNote.totalIva
        adjustment = -(creditNet * snapshotPercentage / 100 + creditIva)
        (usa el snapshotPercentage del evento original, NO el % vigente)
  4. Crear CashBoxMovement type=RECUPERO_REVERSO con amount = adjustment
  5. Crear RecoveryEvent:
        eventType = ADJUSTED_CREDIT_NOTE
        creditNoteDocument = creditNote
        reversesEvent = lastEvent
        snapshot* = copiados del evento original
        recoveredAmount = adjustment
  6. Recalcular saldo de la caja.
```

> Si la factura original fue parcialmente reversada por notas previas, el sistema sigue ajustando proporcionalmente — el saldo neto recuperado por factura es siempre `SUM(events.recoveredAmount WHERE transactionalDocumentId = X)`.

### 4.4 Idempotencia

Cada `RecoveryEvent` registra `cashBoxMovement` y los métodos `generate`/`reverse`/`adjust` se ejecutan dentro de la transacción del TD. Si la transacción del TD se hace rollback, también el evento de recupero. Dual-write seguro.

### 4.5 Hooks de invocación

Inyectar `RecoveryService` en:
- `TransactionalDocumentService.create()`        → tras persistir, llamar `generateForDocument()` si aplica.
- `TransactionalDocumentService.update()`        → comparar diff y llamar `reverseForDocument()` + `generateForDocument()` según corresponda.
- `TransactionalDocumentService.softDelete()`    → llamar `reverseForDocument()` con `reason=DOCUMENT_DELETED`.
- `TransactionalDocumentService.createCreditNote()` (o equivalente) → llamar `adjustForCreditNote()`.
- `RepairService.update/delete`, `FuelLoadService.update/delete`, `SalaryPaymentService.update/delete`, `StockPurchaseService.update/delete` → cuando una entidad linkeada al TD cambia, el `DocumentTotalRecalculator` ya re-genera el net del TD. Después de eso, llamar `RecoveryService.regenerateIfNeeded(doc)` (que internamente revierte el último evento y crea uno nuevo si los montos cambian).

---

## 5. Endpoints REST

### 5.1 Configuración (admin del sector Recupero)

`/api/v1/project-areas/{id}/recovery-suppliers`

| Método | Path                                      | Permiso                  | Descripción |
|---|---|---|---|
| `GET`    | `/project-areas/{id}/recovery-suppliers`  | `RECOVERY_VIEW`          | Listar configs del sector (404 si no es recovery sector) |
| `POST`   | `/project-areas/{id}/recovery-suppliers`  | `RECOVERY_MANAGE`        | Agregar config |
| `PUT`    | `/project-areas/{id}/recovery-suppliers/{configId}` | `RECOVERY_MANAGE` | Editar % o caja |
| `DELETE` | `/project-areas/{id}/recovery-suppliers/{configId}` | `RECOVERY_MANAGE` | Soft delete (active=false) |

### 5.2 Histórico de eventos

| Método | Path                                            | Permiso          | Descripción |
|---|---|---|---|
| `GET` | `/recovery-events`                              | `RECOVERY_VIEW`  | Listado paginado con filtros (rango fecha, supplier, eventType, transactionalDocumentId) |
| `GET` | `/recovery-events/{id}`                         | `RECOVERY_VIEW`  | Detalle |
| `GET` | `/transactional-documents/{id}/recovery-events` | `RECOVERY_VIEW`  | Eventos asociados a un TD específico |

### 5.3 Validación cruzada en endpoints existentes

- En el endpoint de `TransactionalDocument` (POST/PUT), si el body trae `projectAreaId` apuntando a un recovery sector y el TD no es factura A → respuesta `400 Bad Request` con mensaje:
  ```
  El sector "Recupero" solo admite Facturas tipo A. El comprobante actual es {tipo} {letra}.
  ```
- Si el proveedor no está configurado en ese sector → `400` con:
  ```
  El proveedor {nombre} no está configurado en el esquema de recupero. 
  Agregalo en la configuración del sector antes de asociar este comprobante.
  ```

### 5.4 Permisos nuevos

```java
public class AppPermissions {
    // ... existentes
    public static final String RECOVERY_VIEW   = "RECOVERY_VIEW";    // ver configs y eventos
    public static final String RECOVERY_MANAGE = "RECOVERY_MANAGE";  // crear/editar/eliminar configs
}
```

Por default solo el rol Admin tiene `RECOVERY_MANAGE`. `RECOVERY_VIEW` también para roles con permiso financiero.

---

## 6. Frontend

### 6.1 Detección de la magia

En el formulario de crear/editar `ProjectArea`:
- Al guardar, si el `name` (lower-trimmed) === `"recupero"` y NO existe ya un sector con `isRecoverySector=true` para el tenant → backend marca `isRecoverySector=true` automáticamente.
- Si ya existe uno → simplemente queda como un sector normal con ese nombre (sin warning ni mensaje).
- Esta lógica vive en `ProjectAreaService.create()`:
  ```java
  if ("recupero".equals(name.toLowerCase().trim())
      && !projectAreaRepository.existsByTenantIdAndIsRecoverySectorTrue(tenantId)) {
      area.setIsRecoverySector(true);
  }
  ```

### 6.2 Sección extra en detalle de `ProjectArea`

`domains/configuration/project-areas/project-area-detail/`

```
[Datos generales del sector]                            (siempre visible)
─────────────────────────────────────────────────────
[Configuración de Recupero]                             ← solo si isRecoverySector === true
─────────────────────────────────────────────────────
  Proveedores con recupero asociado:
  
  ┌────────────────────────────────────────────────────────────────┐
  │ + Agregar proveedor                                            │
  ├────────────────────────────────────────────────────────────────┤
  │ Proveedor          │ % Recupero │ Caja Destino     │ Saldo  │ ⋮│
  │ Distribuidora ABC  │   90,00 %  │ Caja Recupero    │$1.2M   │ ⋮│
  │ Insumos XYZ        │   75,00 %  │ Caja Banco Nac.  │$340K   │ ⋮│
  │ Refacciones DEF    │  100,00 %  │ Caja Recupero    │$880K   │ ⋮│
  └────────────────────────────────────────────────────────────────┘
  
  [Ver historial de eventos de recupero]
```

### 6.3 Modal "Agregar proveedor a recupero"

```
┌───────────────────────────────────────────────────┐
│ Agregar proveedor con recupero                    │
├───────────────────────────────────────────────────┤
│ Proveedor:    [🔎 Buscar proveedor...      ▼]    │ ← autocomplete
│                                                   │
│ % Recupero:   [____] %  (sobre el neto de        │
│                          comprobantes)            │
│                                                   │
│ Caja destino: [Seleccionar caja...          ▼]   │ ← autocomplete CashBox
│                                                   │
│ ℹ El IVA siempre se recupera al 100%.            │
│ ℹ Solo se generará recupero para facturas A.     │
│                                                   │
│              [Cancelar]    [Guardar]              │
└───────────────────────────────────────────────────┘
```

### 6.4 Validación visual en el formulario de `TransactionalDocument`

Cuando el usuario abre el campo "Sector" en el form de TD:
- Si selecciona el sector con `isRecoverySector=true`:
  - Si el TD no es factura A → mensaje inline rojo "Este sector solo admite facturas tipo A".
  - Si el proveedor no está configurado → mensaje inline rojo "El proveedor seleccionado no tiene configuración de recupero" + link "Configurar".
- Cambio de sector después de guardado → modal de confirmación: "Esta acción revertirá el recupero generado de esta factura ($X). ¿Continuar?".

### 6.5 Pantalla de histórico de eventos

`domains/recovery/events-list/` (ruta `/recupero/eventos`, accesible solo si el tenant tiene un sector con `isRecoverySector=true`).

Filtros: fecha, proveedor, tipo de evento, factura, caja.

Tabla:
| Fecha | Tipo | Factura | Proveedor | Net | IVA | % | Recuperado | Caja | Movimiento |
|---|---|---|---|---|---|---|---|---|---|
| 22/04 | GENERADO | A 0001-00012345 | Distribuidora ABC | $100 | $21 | 90% | **+$111** | Caja Recupero | #4521 |
| 23/04 | NOTA CRÉDITO | A 0001-00099 | Distribuidora ABC | $50 | $10.50 | 90% | **−$55.50** | Caja Recupero | #4533 |

Color verde para positivos (`GENERATED`), rojo para negativos (`REVERSED`/`ADJUSTED_CREDIT_NOTE`).

### 6.6 Frontend: estructura de archivos

```
domains/recovery/                                       — solo se carga si feature activa
├── recovery.routes.ts
├── recovery-events-list/
│   ├── recovery-events-list.ts
│   └── recovery-events-list.html|.scss
└── recovery-supplier-configs/                         — embedded, no es ruta
    ├── recovery-supplier-list.ts                      ← componente embebido en project-area-detail
    ├── add-recovery-supplier-modal.ts
    └── edit-recovery-supplier-modal.ts
```

Prefijo BEM: `rec-`.

En la navegación: agregar entrada **"Recupero"** (con icono `payments`) **solo visible si el tenant tiene el sector activo**, vía guard que consulta un endpoint ligero `GET /api/v1/recovery/active` → retorna `{ active: boolean, projectAreaId: Long }`.

---

## 7. Mensajes i18n

```properties
recovery.notRecoverySector=El sector indicado no está configurado como sector de recupero
recovery.supplier.notConfigured=El proveedor {0} no está configurado en el esquema de recupero. Agregalo antes de asociar este comprobante.
recovery.supplier.alreadyConfigured=Este proveedor ya tiene una configuración en este sector
recovery.document.invalidType=El sector de recupero solo admite Facturas tipo A. El comprobante actual es {0} {1}
recovery.document.noSupplier=No se puede asociar al sector de recupero un comprobante sin proveedor
recovery.config.deleteWithEvents=No se puede eliminar la configuración: tiene {0} eventos de recupero asociados. Desactivala en su lugar.
recovery.cashBox.required=La caja destino es obligatoria
recovery.percentage.range=El porcentaje de recupero debe estar entre 0 y 100
recovery.event.cannotReverse=Este evento ya fue revertido o ajustado
```

---

## 8. Casos de uso ilustrados

### 8.1 Caso happy-path

1. Admin crea sector con name="Recupero" → `isRecoverySector=true` automático.
2. Admin abre detalle del sector → ve sección extra → agrega proveedor "Distribuidora ABC" con 90 % a "Caja Recupero".
3. Operador registra **Factura A 0001-12345** del proveedor Distribuidora ABC, neto $100, IVA $21, asociada al sector "Recupero".
4. Backend: `TransactionalDocumentService.create()` persiste TD → llama `RecoveryService.generateForDocument()` → genera `CashBoxMovement` +$111 + `RecoveryEvent` GENERATED.
5. Saldo de "Caja Recupero" aumenta en $111. En el detalle de la caja se ve el movimiento con descripción "Recupero automático — Factura A 0001-00012345".

### 8.2 Caso de modificación

6. Operador descubre error: la factura era $200 net + $42 IVA. Edita el TD.
7. Backend: detecta cambio en montos → llama `RecoveryService.regenerateIfNeeded()`:
   - Crea `RecoveryEvent` REVERSED con −$111 (cancela el original).
   - Crea `RecoveryEvent` GENERATED con +$222 (90 % de 200 + 42).
8. Saldo neto del recupero para esta factura: $222. Saldo total caja se actualiza.

### 8.3 Caso de nota de crédito

9. Tres días después se emite Nota de Crédito A asociada a la Factura A 0001-12345 por $50 net + $10.50 IVA.
10. Backend: al crearse la NC vinculada → llama `RecoveryService.adjustForCreditNote()`:
    - Snapshot del % original = 90 %.
    - Ajuste = −(50 × 0.9 + 10.50) = **−$55.50**.
    - Crea `CashBoxMovement` RECUPERO_REVERSO −$55.50 + `RecoveryEvent` ADJUSTED_CREDIT_NOTE.
11. Saldo caja decrece $55.50. Histórico muestra los 3 eventos vinculados a la factura.

### 8.4 Caso edge: cambio de proveedor

12. Operador cambia el proveedor del TD a "Insumos XYZ" (que también tiene config en este sector, 75 %, otra caja).
13. Backend:
    - Reversa para Distribuidora ABC en su caja (−$222).
    - Genera para Insumos XYZ en su caja (+($200 × 0.75 + $42) = +$192).

### 8.5 Caso edge: cambio a sector no-Recupero

14. Operador cambia el sector del TD a "Obra Norte".
15. Backend: solo reversa (−$192 de la caja de Insumos XYZ). No genera nada porque el nuevo sector no es recovery.

---

## 9. Checklist de Implementación

### Backend
- [ ] Migración Liquibase: `is_recovery_sector` en `project_areas` + backfill + unique partial index.
- [ ] Migración: tablas `recovery_supplier_configs`, `recovery_events`.
- [ ] Migración: agregar `RECUPERO_AUTOMATICO`, `RECUPERO_REVERSO` al enum de `CashBoxMovementType`.
- [ ] Entidades: `RecoverySupplierConfig`, `RecoveryEvent`.
- [ ] Enums: `RecoveryEventType`, extensión de `CashBoxMovementType`.
- [ ] DTOs (request/response).
- [ ] MapStruct mappers.
- [ ] Repositorios con queries para histórico y validaciones.
- [ ] `RecoveryService` con `generateForDocument`, `reverseForDocument`, `regenerateIfNeeded`, `adjustForCreditNote`.
- [ ] Modificar `ProjectAreaService.create()` para auto-flag por nombre.
- [ ] Modificar `TransactionalDocumentService` (create/update/delete + create credit note) → llamar al service.
- [ ] Modificar services de entidades linkeables (`Repair`, `FuelLoad`, `SalaryPayment`, `StockPurchase`) → invocar `regenerateIfNeeded` tras `DocumentTotalRecalculator`.
- [ ] Validaciones en TD: tipo factura A + proveedor configurado.
- [ ] `RecoveryConfigController` y `RecoveryEventController`.
- [ ] Endpoint `GET /recovery/active` para guard del frontend.
- [ ] Permisos `RECOVERY_VIEW`, `RECOVERY_MANAGE` en `AppPermissions`.
- [ ] Mensajes i18n.
- [ ] Tests críticos:
  - [ ] Generación con factura A standard (caso del enunciado).
  - [ ] Rechazo de factura B/C asignada al sector.
  - [ ] Rechazo de proveedor no configurado.
  - [ ] Reverso por edición de monto + regeneración.
  - [ ] Reverso por nota de crédito proporcional.
  - [ ] Cambio de proveedor entre dos configurados.
  - [ ] Solo un sector con `isRecoverySector=true` por tenant.
  - [ ] Snapshot del % no se afecta por cambios posteriores en config.

### Frontend
- [ ] Modelos TypeScript.
- [ ] Servicios HTTP `RecoveryConfigService`, `RecoveryEventService`.
- [ ] Componente `recovery-supplier-list` embebido condicionalmente en `project-area-detail`.
- [ ] Modales agregar/editar config (con autocomplete de proveedor y caja).
- [ ] Validación inline en form de TD (sector → tipo factura + proveedor).
- [ ] Modal de confirmación al cambiar sector que dispare reverso.
- [ ] Pantalla `recovery-events-list` con filtros y badges de tipo.
- [ ] Guard de ruta `/recupero` consultando `GET /recovery/active`.
- [ ] Entrada "Recupero" en navegación visible condicionalmente.

### Validación end-to-end
- [ ] Crear sector "Recupero" → verificar flag persistido + sección extra visible.
- [ ] Renombrar sector a "Recupero Banco" → verificar que la funcionalidad sigue activa.
- [ ] Intentar crear segundo sector "Recupero" → verificar que queda como sector normal.
- [ ] Configurar proveedor 90 % + caja → registrar factura A → verificar +$111 en caja.
- [ ] Editar factura aumentando monto → verificar reverso + regeneración.
- [ ] Crear nota de crédito → verificar ajuste proporcional.
- [ ] Eliminar factura → verificar reverso completo.
- [ ] Linkear un Repair de $50 a la factura → verificar regeneración con neto actualizado.
- [ ] Cambiar % en config → verificar que recuperos históricos NO cambian.
- [ ] Desactivar config de proveedor → intentar registrar nueva factura → verificar rechazo claro.
