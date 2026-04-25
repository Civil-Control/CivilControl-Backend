# Feature 16 — Reporte de Pagos Emitidos + Estado del Cheque

## Resumen

Reporte que muestra todos los pagos emitidos por la empresa en un período, con diferenciación por método de pago (Efectivo, Transferencia, Cheque). Incorpora gestión de **estado del cheque** con un nuevo endpoint y migración asociada.

Este reporte introduce una particularidad respecto al resto: el usuario puede **alternar entre dos modos de agrupación** (Método→Proveedor o Proveedor→Método) desde el toolbar, sin recargar filtros.

> **Dependencia con F15 (Tesorería):** Esta feature consume las entidades `BankAccount`, `Checkbook` y `CashBox` introducidas en F15. Los cambios de estado del cheque disparan los movimientos `CHEQUE_COBRADO`/`CHEQUE_RECHAZADO`/`CHEQUE_CANCELADO` sobre la `BankAccount` asociada (lógica documentada en F15 §5.4). F15 **debe estar implementada y migrada antes** de iniciar F16.

---

## Parte A — Gestión de Estado del Cheque (precondición de la feature)

### A.1 Nuevo enum: `CheckStatus`

**Ubicación:** `model/enums/payment/CheckStatus.java`

```java
public enum CheckStatus {
    PENDIENTE,    // Default — emitido, en circulación, esperando ser cobrado
    COBRADO,      // El banco lo debitó, salió el dinero
    RECHAZADO,    // El banco lo rebotó (sin fondos, diferencia de firma, etc.)
    CANCELADO,    // Anulado por el emisor antes de ser cobrado
    VENCIDO       // Derivado: PENDIENTE con dueDate < hoy
}
```

**Decisión de diseño — `VENCIDO` es derivado, no persistido:**
- Se calcula en el backend al momento de leer cada cheque: `if (status == PENDIENTE && dueDate < today) → VENCIDO`.
- Razón: evita estados inconsistentes (un cheque "vencido" un día y "no vencido" al siguiente requeriría un job batch). Mantiene la fuente de verdad simple.
- En BD se guarda solo el estado **operativo** (PENDIENTE / COBRADO / RECHAZADO / CANCELADO).
- En todas las respuestas (DTOs, exporters, frontend) se expone el estado **efectivo** ya derivado.

### A.2 Modificación a `CheckPayment`

**Archivo:** `model/entity/payment/CheckPayment.java`

Agregar campos:
```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 20)
@Builder.Default
private CheckStatus status = CheckStatus.PENDIENTE;

@Column(name = "settled_date")
private LocalDate settledDate;       // fecha real de cobro/rechazo/cancelación

@Column(name = "status_comment", length = 500)
private String statusComment;        // motivo del cambio (ej: "Sin fondos", "Anulado por error en monto")

@Column(name = "status_changed_at")
private LocalDateTime statusChangedAt;

@Column(name = "status_changed_by")
private Long statusChangedByUserId;  // auditoría: usuario que hizo el cambio
```

### A.3 Migración de BD

**Archivo:** `db/changelog/db.changelog-{nro}.xml` (siguiendo convención existente del proyecto)

```sql
ALTER TABLE check_payments ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE check_payments ADD COLUMN settled_date DATE NULL;
ALTER TABLE check_payments ADD COLUMN status_comment VARCHAR(500) NULL;
ALTER TABLE check_payments ADD COLUMN status_changed_at TIMESTAMP NULL;
ALTER TABLE check_payments ADD COLUMN status_changed_by BIGINT NULL;

-- Backfill: cheques existentes quedan en PENDIENTE (cubierto por el DEFAULT).
-- Índice para acelerar filtros por estado en el reporte:
CREATE INDEX idx_check_payments_status ON check_payments(tenant_id, status);
```

### A.4 DTOs nuevos

**`model/dto/payment/CheckStatusUpdateDTO.java`**
```java
public record CheckStatusUpdateDTO(
    @NotNull CheckStatus status,                 // operativo: PENDIENTE | COBRADO | RECHAZADO | CANCELADO
    LocalDate settledDate,                       // requerido si status == COBRADO o RECHAZADO
    @Size(max = 500) String statusComment        // recomendado si status == RECHAZADO o CANCELADO
) {}
```

**Modificar `CheckPaymentResponseDTO`** para incluir:
```java
public record CheckPaymentResponseDTO(
    Long id,
    PaymentDetailsResponseDTO paymentDetails,
    String type,
    LocalDate dueDate,
    String checkNumber,
    String bankName,
    CheckStatus status,                          // estado efectivo (puede ser VENCIDO derivado)
    CheckStatus persistedStatus,                 // estado real persistido (sin derivación)
    LocalDate settledDate,
    String statusComment,
    LocalDateTime statusChangedAt
) implements PaymentResponseDTO {}
```

### A.5 Endpoint nuevo

**`PATCH /api/v1/payments/check/{id}/status`**

| Atributo | Valor |
|---|---|
| Permiso | `PAYMENT_UPDATE` |
| Body | `CheckStatusUpdateDTO` |
| Respuesta | `CheckPaymentResponseDTO` actualizado |

**Validaciones en el service:**
- No se puede transicionar desde `COBRADO` o `CANCELADO` a otro estado (estados terminales). Excepción: admin con permiso especial — fuera de scope.
- `settledDate` requerido si `status ∈ {COBRADO, RECHAZADO}`.
- `settledDate` no puede ser anterior a `paymentDate`.
- Auditar: `statusChangedAt = now()`, `statusChangedByUserId = currentUser.id`.
- No se permite setear `VENCIDO` manualmente (es derivado).

**Integración con Tesorería (F15) — lógica de saldo de `BankAccount`:**
- Al crear el `CheckPayment` (precondición F15), se generó un movimiento `CHEQUE_EMITIDO` con `−amount`.
- Transición a `COBRADO` → registrar `CHEQUE_COBRADO` (informativo, saldo = 0).
- Transición a `RECHAZADO` → registrar `CHEQUE_RECHAZADO` (libera reserva, `+amount`).
- Transición a `CANCELADO` → registrar `CHEQUE_CANCELADO` (libera reserva, `+amount`).
- Esta llamada se hace via `BankAccountMovementService` inyectado en `PaymentService`.

### A.6 Frontend — UI del estado del cheque

En la vista de detalle del pago tipo cheque (`payment-detail` ya existente):
- Mostrar badge del estado con color:
  - `PENDIENTE` → gris
  - `VENCIDO` → naranja con icono de alerta
  - `COBRADO` → verde
  - `RECHAZADO` → rojo
  - `CANCELADO` → gris oscuro tachado
- Botón "Cambiar estado" abre modal con select de estados válidos + campo `settledDate` (visible si COBRADO/RECHAZADO) + comentario.
- Permiso `PAYMENT_UPDATE` para mostrar el botón.

---

## Parte B — Reporte de Pagos Emitidos

## Estructura de Capas

| Capa | Modo "Por Método" (default) | Modo "Por Proveedor" |
|------|----------------------------|----------------------|
| 1 | Método de Pago | Proveedor |
| 2 | Proveedor | Método de Pago |
| 3 | Pago individual (PaymentDetails) | Pago individual (PaymentDetails) |

**Decisión de diseño — Doble modo de agrupación:**
El usuario alterna entre los dos modos desde un toggle en el toolbar. Backend devuelve la estructura ya armada según el parámetro `groupBy=METHOD|SUPPLIER`. Esto evita lógica de pivot en frontend y garantiza consistencia entre vista y exporters.

## Entidad Fuente: `PaymentDetails`

**Ubicación:** `model/entity/payment/PaymentDetails.java`

Cada `PaymentDetails` se asocia 1-a-1 con uno de: `CashPayment`, `TransferPayment`, `CheckPayment`. El método se infiere por la presencia de la subentidad correspondiente.

**Campos relevantes:**
- `id`, `paymentDate`, `supplier`, `amount`, `comment`
- `paidDocuments` — ManyToMany → TransactionalDocument
- `cashPayment` / `transferPayment` / `checkPayment` — exactamente uno presente

**Subentidades específicas:**
- `CheckPayment`: `checkNumber`, `bankName`, `dueDate`, `status` (nuevo), `settledDate` (nuevo)
- `TransferPayment`: `transactionNumber`, `bankName`
- `CashPayment`: sin campos adicionales

## Repositorios — Métodos a Agregar

### `PaymentRepository`
- Reutilizar `findAllWithFilters(...)` ya existente (acepta paymentMethod, fechas, supplier, etc.).
- Agregar variante `findAllForReport(filters)` sin paginación que retorne `List<PaymentDetails>` con fetch eager de subentidades, supplier y paidDocuments para evitar N+1.

### `CheckPaymentRepository`
- Agregar `List<CheckPayment> findByPaymentDetailsIdIn(Collection<Long> ids)` para enriquecer con estado en una sola query.

## DTOs a Crear

### `model/dto/report/issuedPayment/IssuedPaymentReportDTO.java`
```java
@Builder
public record IssuedPaymentReportDTO(
    IssuedPaymentReportFilterDTO filters,
    IssuedPaymentReportGroupBy groupBy,                     // METHOD | SUPPLIER
    List<IssuedPaymentReportPrimaryGroupDTO> primaryGroups, // estructura de capa 1
    BigDecimal totalAmount,
    int totalCount,
    Map<PaymentMethod, BigDecimal> totalsByMethod,          // siempre presente
    Map<PaymentMethod, Integer> countsByMethod,             // siempre presente
    CheckSummaryDTO checkSummary,                           // resumen de cheques (siempre, aunque sea null por filtros)
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
```

### `model/dto/report/issuedPayment/CheckSummaryDTO.java`
```java
@Builder
public record CheckSummaryDTO(
    int totalChecks,
    BigDecimal totalChecksAmount,
    int pendingCount,            BigDecimal pendingAmount,
    int overdueCount,            BigDecimal overdueAmount,    // VENCIDO derivado
    int settledCount,            BigDecimal settledAmount,    // COBRADO
    int rejectedCount,           BigDecimal rejectedAmount,
    int cancelledCount,          BigDecimal cancelledAmount
) {}
```

### `model/dto/report/issuedPayment/IssuedPaymentReportPrimaryGroupDTO.java`
```java
@Builder
public record IssuedPaymentReportPrimaryGroupDTO(
    String groupKey,                                        // "CHECK" / "TRANSFER" / "CASH" o supplierId.toString()
    String groupLabel,                                      // "Cheque" / "Banco Galicia S.A."
    String groupSubLabel,                                   // null o CUIT del proveedor
    BigDecimal subtotalAmount,
    int paymentCount,
    Map<PaymentMethod, BigDecimal> subtotalsByMethod,       // poblado solo en modo SUPPLIER
    List<IssuedPaymentReportSecondaryGroupDTO> secondaryGroups
) {}
```

### `model/dto/report/issuedPayment/IssuedPaymentReportSecondaryGroupDTO.java`
```java
@Builder
public record IssuedPaymentReportSecondaryGroupDTO(
    String groupKey,
    String groupLabel,
    String groupSubLabel,
    BigDecimal subtotalAmount,
    int paymentCount,
    List<IssuedPaymentReportItemDTO> payments
) {}
```

### `model/dto/report/issuedPayment/IssuedPaymentReportItemDTO.java`
```java
public record IssuedPaymentReportItemDTO(
    Long id,
    LocalDate paymentDate,
    PaymentMethod method,
    Long supplierId,
    String supplierLegalName,
    String supplierTradeName,
    String supplierCuit,
    BigDecimal amount,
    String comment,
    int linkedDocumentCount,                  // cantidad de comprobantes imputados
    String paymentMethodReference,            // "Cheque N° 12345 Galicia" / "Transf. ABC123 Nación" / "Efectivo"

    // ── Datos de tesorería (F15) ──
    Long bankAccountId,                       // null si método == CASH sin caja, requerido si CHECK/TRANSFER
    String bankAccountName,
    String bankName,                          // resuelto desde BankAccount, no string libre
    Long cashBoxId,                           // null si método != CASH o no existe caja asignada
    String cashBoxName,

    // ── Específico de cheque (null si no aplica) ──
    String checkNumber,
    LocalDate checkDueDate,
    CheckStatus checkStatus,                  // estado efectivo (con VENCIDO derivado)
    CheckStatus checkPersistedStatus,         // estado persistido
    LocalDate checkSettledDate,
    String checkStatusComment,
    Long checkbookId,                         // null si el cheque no fue emitido desde una chequera
    String checkbookName,
    String checkbookNumber,

    // ── Específico de transferencia (null si no aplica) ──
    String transferTransactionNumber
) {}
```

### `model/enums/report/IssuedPaymentReportGroupBy.java`
```java
public enum IssuedPaymentReportGroupBy {
    METHOD,    // Capa 1 = método, Capa 2 = proveedor
    SUPPLIER   // Capa 1 = proveedor, Capa 2 = método
}
```

### `model/dto/report/issuedPayment/IssuedPaymentReportFilterDTO.java`
```java
public record IssuedPaymentReportFilterDTO(
    LocalDate startDate,                          // obligatorio
    LocalDate endDate,                            // obligatorio
    List<PaymentMethod> paymentMethods,           // multiselect (default: todos)
    List<Long> supplierIds,
    List<Long> projectAreaIds,                    // filtra por área de los docs imputados
    List<CheckStatus> checkStatuses,              // filtro de estado del cheque (incluye VENCIDO derivado)
    List<Long> bankAccountIds,                    // filtro por cuenta bancaria (F15)
    List<Long> cashBoxIds,                        // filtro por caja (F15)
    List<Long> checkbookIds,                      // filtro por chequera (F15)
    BigDecimal minAmount,
    BigDecimal maxAmount,
    Boolean onlyOverdueChecks,                    // shortcut: cheques pendientes con dueDate < hoy
    IssuedPaymentReportGroupBy groupBy            // METHOD (default) | SUPPLIER
) {}
```

## Exporters

### Excel — `IssuedPaymentReportExcelExporter.java`

**Hoja 1: "Resumen por {Capa1}"**
Si `groupBy == METHOD`:
Columnas: `Método | Cant. Pagos | Total $`
Si `groupBy == SUPPLIER`:
Columnas: `Proveedor (Razón Social) | CUIT | Cant. Pagos | [columna por cada PaymentMethod activo] | Total $`

**Hoja 2: "Resumen por {Capa2}"**
Mismo concepto, con la otra dimensión como agrupación principal y un nivel de subgrupo dentro.

**Hoja 3: "Detalle de Pagos"**
Columnas: `Fecha | Método | Proveedor | CUIT | Monto | Cuenta Bancaria / Caja | Cheque N° | Chequera | Banco | Vencimiento | Estado | Fecha Cobro | Comentario | Cant. Comprob.`
- Subtotales por capa 2 y capa 1 según `groupBy`.
- Filas de cheques pintadas según estado (mismo coloreado que en frontend, con fondo sutil).

**Hoja 4 (opcional pero recomendada): "Resumen de Cheques"**
Tarjeta resumen con todos los counts/amounts del `CheckSummaryDTO`.

### PDF — `IssuedPaymentReportPdfExporter.java`
Misma estructura en 3-4 secciones con `AreaBreak(NEXT_PAGE)`. Mismos colores estándar:
- Header: `DeviceRgb(41, 128, 185)`
- Subtotal: `DeviceRgb(220, 220, 220)`
- Total: `DeviceRgb(243, 156, 18)`

**Coloreado de estados de cheques (consistente con frontend):**
- `PENDIENTE` → `DeviceRgb(160, 160, 160)` (gris)
- `VENCIDO` → `DeviceRgb(243, 156, 18)` (naranja)
- `COBRADO` → `DeviceRgb(120, 180, 120)` (verde)
- `RECHAZADO` → `DeviceRgb(220, 80, 80)` (rojo)
- `CANCELADO` → `DeviceRgb(100, 100, 100)` (gris oscuro)

Reusar helpers `headerCell()`, `cell()`, `cellCenter()`, `cellAmount()`.

## Controller Endpoints

```
GET  /api/v1/reports/issued-payments
GET  /api/v1/reports/issued-payments/download?format=PDF|EXCEL
PATCH /api/v1/payments/check/{id}/status        # parte A
```

**Parámetros del reporte (@RequestParam):**
- `startDate`, `endDate` — LocalDate **(obligatorios)**
- `paymentMethods` — List<PaymentMethod>
- `supplierIds` — List<Long>
- `projectAreaIds` — List<Long>
- `checkStatuses` — List<CheckStatus>
- `bankName` — String
- `minAmount`, `maxAmount` — BigDecimal
- `onlyOverdueChecks` — Boolean
- `groupBy` — IssuedPaymentReportGroupBy (default `METHOD`)

**Permisos:**
- `REPORT_FINANCIAL` para `GET /issued-payments`
- `REPORT_EXPORT` para `GET /issued-payments/download`
- `PAYMENT_UPDATE` para `PATCH /payments/check/{id}/status`

## Service — Lógica

```java
public IssuedPaymentReportDTO generateIssuedPaymentReport(IssuedPaymentReportFilterDTO filters) {
    validateIssuedPaymentFilters(filters);

    // 1. Obtener pagos del período (fetch eager de subentidades, supplier, paidDocuments)
    List<PaymentDetails> payments = paymentRepository.findAllForReport(filters);

    // 2. Derivar estado efectivo de cheques (VENCIDO si dueDate < today && status == PENDIENTE)
    Map<Long, CheckStatus> effectiveCheckStatus = computeEffectiveCheckStatuses(payments);

    // 3. Aplicar filtro post-query de checkStatuses (incluye VENCIDO derivado)
    List<PaymentDetails> filtered = applyCheckStatusFilter(payments, filters, effectiveCheckStatus);

    // 4. Mapear a IssuedPaymentReportItemDTO
    List<IssuedPaymentReportItemDTO> items = filtered.stream()
        .map(p -> toReportItem(p, effectiveCheckStatus))
        .toList();

    // 5. Armar capas según filters.groupBy()
    List<IssuedPaymentReportPrimaryGroupDTO> primaryGroups =
        switch (filters.groupBy() == null ? METHOD : filters.groupBy()) {
            case METHOD   -> buildGroupsByMethodThenSupplier(items);
            case SUPPLIER -> buildGroupsBySupplierThenMethod(items);
        };

    // 6. Calcular totalsByMethod, countsByMethod (siempre presentes)
    // 7. Calcular CheckSummaryDTO recorriendo todos los cheques del filtered
    // 8. Construir IssuedPaymentReportDTO
}
```

**Métodos privados:**
- `validateIssuedPaymentFilters(filters)` — startDate y endDate obligatorios
- `computeEffectiveCheckStatuses(payments)` — aplica derivación VENCIDO
- `applyCheckStatusFilter(payments, filters, statusMap)`
- `toReportItem(payment, statusMap)`
- `buildGroupsByMethodThenSupplier(items)`
- `buildGroupsBySupplierThenMethod(items)`
- `buildCheckSummary(items)`
- `buildIssuedPaymentPeriodDescription(filters)`

```java
public CheckPaymentResponseDTO updateCheckStatus(Long checkPaymentId, CheckStatusUpdateDTO dto, Long currentUserId) {
    CheckPayment check = checkPaymentRepository.findById(checkPaymentId)
        .orElseThrow(() -> new PaymentNotFoundException(checkPaymentId));

    validateCheckStatusTransition(check.getStatus(), dto.status());
    validateSettledDate(check, dto);

    check.setStatus(dto.status());
    check.setSettledDate(dto.settledDate());
    check.setStatusComment(dto.statusComment());
    check.setStatusChangedAt(LocalDateTime.now());
    check.setStatusChangedByUserId(currentUserId);

    return checkPaymentMapper.toResponse(checkPaymentRepository.save(check));
}
```

## Frontend

### Archivos

- `shared/models/issued-payment-report.model.ts` — interfaces espejo + labels y colores de `CheckStatus`
- `domains/reports/issued-payment-report/` — componente principal
- `domains/reports/issued-payment-report/services/issued-payment-report.service.ts`
- `domains/finance/payment/check-status-modal/` — modal nuevo de cambio de estado del cheque
- Modificación a `domains/finance/payment/payment-detail/` — agregar badge y botón

### Constantes compartidas

```typescript
export const CheckStatusLabels: Record<CheckStatus, string> = {
  PENDIENTE: 'Pendiente',
  VENCIDO:   'Vencido',
  COBRADO:   'Cobrado',
  RECHAZADO: 'Rechazado',
  CANCELADO: 'Cancelado',
};

export const CheckStatusColors: Record<CheckStatus, { bg: string; fg: string; icon: string }> = {
  PENDIENTE: { bg: '#e9ecef', fg: '#495057', icon: 'schedule' },
  VENCIDO:   { bg: '#fff3cd', fg: '#856404', icon: 'warning' },
  COBRADO:   { bg: '#d4edda', fg: '#155724', icon: 'check_circle' },
  RECHAZADO: { bg: '#f8d7da', fg: '#721c24', icon: 'cancel' },
  CANCELADO: { bg: '#dee2e6', fg: '#6c757d', icon: 'block' },
};
```

### Componente: `domains/reports/issued-payment-report/`

**Prefijo SCSS:** `ipr-` (issued-payment-report)

**Filtros del sidebar:**
1. Período (Desde/Hasta) — **obligatorio**
2. Métodos de Pago — multiselect (Efectivo / Transferencia / Cheque)
3. Proveedores — multiselect
4. Sectores/Áreas — chips
5. Estado del Cheque — multiselect (Todos / Pendiente / Vencido / Cobrado / Rechazado / Cancelado) — habilitado solo si "Cheque" está en métodos
6. Cuenta Bancaria — multiselect autocomplete de `BankAccount` (F15)
7. Caja — multiselect autocomplete de `CashBox` (F15) — habilitado solo si "Efectivo" está en métodos
8. Chequera — multiselect autocomplete de `Checkbook` (F15) — habilitado solo si "Cheque" está en métodos
9. Rango de Monto
10. Toggle: "Solo cheques vencidos no cobrados"

**Toolbar — Toggle de modo de agrupación (clave de la UX):**

```
┌──────────────────────────────────────────────────────────────┐
│  Período: 01/03/2026 — 31/03/2026                            │
│  Agrupar por:  [● Método]  [○ Proveedor]                     │
│  [Expandir todo] [Colapsar todo]   [📊 Excel] [📄 PDF]      │
└──────────────────────────────────────────────────────────────┘
```

**Diferenciación visual entre modos:**

- **Modo "Por Método"** (default): la capa 1 muestra **chips grandes con ícono del método**:
  - 💵 Efectivo (verde claro)
  - 🏦 Transferencia (azul claro)
  - 📃 Cheque (naranja claro)
  - Dentro de cada chip se listan los proveedores con su monto.

- **Modo "Por Proveedor"**: la capa 1 muestra **tarjetas de proveedor con barras horizontales segmentadas** que visualizan la mezcla de métodos:
  - Cada proveedor tiene una barra dividida proporcionalmente entre los 3 colores de los métodos usados.
  - Capa 2: filas etiquetadas "Efectivo / Transferencia / Cheque" con su subtotal.

Esta dualidad visual hace evidente al usuario "qué está viendo": chips por método vs barras por proveedor.

**KPI cards (siempre visibles, independiente del modo):**
1. Total Pagado ($)
2. Cantidad de Pagos
3. Subtotal por Método (3 mini-cards: Efectivo / Transferencia / Cheque)
4. **Cheques Pendientes** (cantidad + monto) — gris
5. **Cheques Vencidos** (cantidad + monto) — naranja con icono de alerta
6. **Cheques Cobrados** (cantidad + monto) — verde
7. Cheques Rechazados / Cancelados — collapsable

**Tabla de detalle (Capa 3):**
| Fecha | Método | Proveedor | Monto | Referencia (Cheque/Transf.) | Cuenta / Caja | Banco | Chequera | Estado | Vencimiento | Cobrado el | Comp. |

- Filas de cheques con el coloreado de fondo según estado (CSS `--ipr-status-{status}`).
- Click en fila de cheque abre modal de cambio de estado (si tiene permiso `PAYMENT_UPDATE`).

### Modal de Cambio de Estado — `check-status-modal`

```
┌─────────────────────────────────────────┐
│  Cambiar estado del cheque              │
├─────────────────────────────────────────┤
│  Cheque N° 12345 — Banco Galicia        │
│  Vencimiento: 15/04/2026                │
│  Estado actual: [● Pendiente]           │
│                                         │
│  Nuevo estado:                          │
│   ○ Cobrado                             │
│   ○ Rechazado                           │
│   ○ Cancelado                           │
│                                         │
│  Fecha de cobro/rechazo: [_____]        │ ← visible si Cobrado o Rechazado
│  Comentario:             [_____]        │ ← recomendado para Rechazado/Cancelado
│                                         │
│         [Cancelar]   [Confirmar]        │
└─────────────────────────────────────────┘
```

- Validación cliente: si `nuevo estado ∈ {COBRADO, RECHAZADO}` → `settledDate` requerida y >= `paymentDate`.
- No se permite seleccionar `VENCIDO` (no aparece en el select).
- No se permite cambiar si el estado actual es `COBRADO` o `CANCELADO` (terminal): el botón no se muestra.

### Route: `/informes/pagos-emitidos`
### Tab label: `Pagos Emitidos`

## Particularidades

- **Doble modo de agrupación** con toggle en toolbar y diferenciación visual marcada (chips vs barras).
- **Estado del cheque persistido + estado derivado VENCIDO**: única fuente de verdad sin necesidad de jobs batch.
- **Estados terminales** (`COBRADO`, `CANCELADO`) — no transicionables.
- **`CheckSummaryDTO` siempre presente** en la respuesta para mantener KPIs consistentes incluso si el filtro excluye cheques (se devuelve con counts en 0).
- **Migración de BD requerida** (única feature de reportes con migración) para los nuevos campos de `check_payments`.
- **Auditoría de cambios** de estado vía `statusChangedAt` y `statusChangedByUserId`.
- **Mensajes i18n nuevos** (en `messages_es.properties`):
  ```properties
  check.status.transition.invalid=Transición de estado inválida: de {0} a {1}
  check.status.terminal=El cheque ya está en estado terminal y no puede modificarse
  check.status.settledDate.required=La fecha de cobro/rechazo es obligatoria
  check.status.settledDate.beforePayment=La fecha de cobro no puede ser anterior a la fecha del pago
  check.status.cannotSetDerived=No se puede establecer manualmente el estado VENCIDO
  ```

## Checklist de Implementación

### Parte A — Estado del cheque
- [ ] Enum `CheckStatus`
- [ ] Migración de BD para `check_payments` (5 columnas + índice)
- [ ] Modificar entidad `CheckPayment`
- [ ] Modificar `CheckPaymentResponseDTO` y mapper
- [ ] DTO `CheckStatusUpdateDTO`
- [ ] Método `updateCheckStatus` en `PaymentService` + `IPaymentService`
- [ ] Endpoint `PATCH /payments/check/{id}/status` en `PaymentController`
- [ ] Mensajes i18n
- [ ] Frontend: badge + botón en `payment-detail`
- [ ] Frontend: modal `check-status-modal` con validaciones

### Parte B — Reporte
- [ ] Enum `IssuedPaymentReportGroupBy`
- [ ] DTOs (`Report`, `Primary/SecondaryGroup`, `Item`, `CheckSummary`, `Filter`)
- [ ] Repos: `findAllForReport`, `findByPaymentDetailsIdIn`
- [ ] Interface en `IReportService` (2 métodos)
- [ ] Implementación en `ReportService` (~10 métodos privados + 2 públicos)
- [ ] Excel Exporter (3-4 hojas)
- [ ] PDF Exporter (3-4 secciones con coloreado de estados)
- [ ] 2 endpoints en `ReportController`
- [ ] Frontend model + service
- [ ] Frontend component (`.ts`, `.html`, `.scss`) con toggle de agrupación
- [ ] Diferenciación visual: chips para "Por Método", barras segmentadas para "Por Proveedor"
- [ ] Route en `reports.routes.ts`
- [ ] Tab en `navigation.config.ts`
- [ ] Test manual: ambos modos, todos los estados de cheque, filtros, Excel, PDF
