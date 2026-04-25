# Reporte de Cuenta Corriente de Proveedores

## Estructura de Capas

| Capa | Agrupación | Entidad fuente | Descripción |
|------|-----------|----------------|-------------|
| 1 | Proveedor | Supplier | Una entrada por proveedor con saldo anterior, totales del período y saldo final |
| 2 | Movimientos | TransactionalDocument + PaymentDetails | Línea de tiempo cronológica de débitos y créditos en el período, con saldo acumulado |

**Nota:** Este reporte rompe intencionalmente con el patrón de 3 capas del resto del módulo. La unidad natural de análisis de una cuenta corriente es el proveedor, no el sector. Agrupar por `ProjectArea` fragmentaría artificialmente el saldo de cada proveedor (un mismo proveedor opera en múltiples sectores y la deuda real no es divisible por sector). `ProjectArea` se usa como **filtro** de movimientos, no como capa de agrupación.

## Entidades Fuente

### Débitos: `TransactionalDocument`
**Ubicación:** `model/entity/TransactionalDocument.java`

Cada comprobante de compra (Factura A/B/C, Nota de Débito A/B/C) genera un débito en la cuenta corriente del proveedor.

Campos relevantes:
- `id`, `date`, `documentType`, `branchCode`, `documentNumber`
- `supplier` — ManyToOne
- `total` — BigDecimal (monto del débito)
- `paid` — Boolean
- `creditNoteApplications`, `appliedCredits` — para resolver NC aplicadas

### Créditos por Pago: `PaymentDetails`
**Ubicación:** `model/entity/payment/PaymentDetails.java`

Cada pago registrado al proveedor genera un crédito.

Campos relevantes:
- `id`, `paymentDate`, `supplier`, `amount`
- `paidDocuments` — ManyToMany → TransactionalDocument (comprobantes imputados)
- `cashPayment` / `transferPayment` / `checkPayment` — medio de pago

### Créditos por Nota de Crédito: `TransactionalDocument` (tipo NOTA_CREDITO_*)
Las NC se modelan como `TransactionalDocument` con `documentType` de tipo crédito. En la línea de tiempo del proveedor se muestran como **crédito** (no como débito), restando del saldo.

### Saldo Anterior: `TransactionalDocument` + `PaymentDetails` con `date < startDate`
El saldo al inicio del período se calcula como:
```
saldoAnterior = SUM(débitos antes de startDate) − SUM(créditos antes de startDate)
```

## Repositorios — Métodos a Agregar

### `TransactionalDocumentRepository`
- `findAllBySupplierIdInAndDateBetween(Collection<Long> supplierIds, LocalDate from, LocalDate to)` — débitos del período
- `sumDebitBySupplierIdBeforeDate(Long supplierId, LocalDate beforeDate)` — débitos antes del período (excluye NC)
- `sumCreditBySupplierIdBeforeDate(Long supplierId, LocalDate beforeDate)` — NC antes del período
- Los listados deben filtrar `deleted = false`

### `PaymentRepository`
- Reutilizar `sumAmountBySupplierId(supplierId, fromDate, toDate)` (ya existe) para totales en el período
- Agregar `sumAmountBySupplierIdBeforeDate(Long supplierId, LocalDate beforeDate)` — créditos por pago antes del período
- Agregar `findAllBySupplierIdInAndPaymentDateBetween(Collection<Long> supplierIds, LocalDate from, LocalDate to)`

### `SupplierRepository`
- Reutilizar `findAllWithFilters` existente con el filtro `hasPendingBalance` ya disponible
- Si se filtra por proveedores específicos: usar `findAllById`

**Optimización N+1:** todas las queries reciben colecciones de `supplierIds` para una sola pasada y luego se indexan en `Map<Long, ...>` antes de armar las capas.

## DTOs a Crear

### `model/dto/report/supplierAccount/SupplierAccountReportDTO.java`
```java
@Builder
public record SupplierAccountReportDTO(
    SupplierAccountReportFilterDTO filters,
    List<SupplierAccountReportSupplierGroupDTO> supplierGroups,
    int supplierCount,
    int pendingSupplierCount,                // proveedores con saldo final > 0
    int settledSupplierCount,                // proveedores con saldo final == 0
    BigDecimal totalPreviousBalance,         // suma de saldos anteriores
    BigDecimal totalDebited,                 // suma de débitos del período
    BigDecimal totalCredited,                // suma de créditos del período (pagos + NC)
    BigDecimal totalPendingBalance,          // suma de saldos finales pendientes
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
```

### `model/dto/report/supplierAccount/SupplierAccountReportSupplierGroupDTO.java`
```java
@Builder
public record SupplierAccountReportSupplierGroupDTO(
    Long supplierId,
    String supplierLegalName,
    String supplierTradeName,
    String supplierCuit,
    BigDecimal previousBalance,              // saldo al startDate
    BigDecimal totalDebited,                 // débitos del período
    BigDecimal totalPaid,                    // pagos del período
    BigDecimal totalCreditNotes,             // NC del período
    BigDecimal finalBalance,                 // saldo al endDate
    SupplierAccountStatus status,            // PENDIENTE | CANCELADO
    int movementCount,
    List<SupplierAccountMovementDTO> movements
) {}
```

### `model/dto/report/supplierAccount/SupplierAccountMovementDTO.java`
```java
public record SupplierAccountMovementDTO(
    LocalDate date,
    SupplierAccountMovementType type,        // INVOICE | DEBIT_NOTE | CREDIT_NOTE | PAYMENT
    String reference,                        // "FA 0001-00001234" o "Pago #42"
    String description,                      // tipo legible o concepto del pago
    BigDecimal debit,                        // 0 si es crédito
    BigDecimal credit,                       // 0 si es débito
    BigDecimal accumulatedBalance,           // saldo acumulado tras este movimiento
    String paymentMethod,                    // null si no aplica
    Long sourceId                            // id del documento o del pago
) {}
```

### Enums nuevos

**`model/enums/report/SupplierAccountStatus.java`**
```java
public enum SupplierAccountStatus {
    PENDIENTE,   // saldo final > 0
    CANCELADO    // saldo final == 0 en el período
}
```

**`model/enums/report/SupplierAccountMovementType.java`**
```java
public enum SupplierAccountMovementType {
    INVOICE,        // factura (débito)
    DEBIT_NOTE,     // nota de débito (débito)
    CREDIT_NOTE,    // nota de crédito (crédito)
    PAYMENT         // pago (crédito)
}
```

### `model/dto/report/supplierAccount/SupplierAccountReportFilterDTO.java`
```java
public record SupplierAccountReportFilterDTO(
    LocalDate startDate,                     // obligatorio
    LocalDate endDate,                       // obligatorio
    List<Long> supplierIds,                  // multiselect opcional
    List<Long> projectAreaIds,               // filtra movimientos, no agrupa
    SupplierAccountStatus statusFilter,      // null = todos
    DocumentType documentType,               // filtro de tipo de comprobante (opcional)
    PaymentMethod paymentMethod,             // filtro de método de pago (opcional)
    BigDecimal minFinalBalance,
    BigDecimal maxFinalBalance,
    Boolean onlyWithMovementsInPeriod        // true = excluir proveedores sin movimientos en el período
) {}
```

## Exporters

### Excel — `SupplierAccountReportExcelExporter.java`

**Hoja 1: "Resumen por Proveedor"**
Columnas: `Proveedor (Razón Social) | CUIT | Saldo Anterior | Débitos Período | Pagos | NC Aplicadas | Saldo Final | Estado | Cant. Mov.`
- Ordenado: pendientes primero (DESC por `finalBalance`), luego cancelados.
- Fila final TOTAL GENERAL con sumatorias de columnas numéricas.
- Estado se renderiza con fondo de color: PENDIENTE en `DeviceRgb(243, 156, 18)` (naranja), CANCELADO en verde claro.

**Hoja 2: "Detalle de Movimientos"**
Agrupado por proveedor (fila merge azul claro con razón social + CUIT).
Columnas: `Fecha | Tipo | Referencia | Concepto | Débito | Crédito | Saldo Acumulado | M. Pago`
- Fila inicial por proveedor: "Saldo anterior al [startDate]" con `accumulatedBalance` = previousBalance.
- Filas de movimientos cronológicas (ASC por fecha).
- Fila final por proveedor: subtotal con `Saldo Final`.

### PDF — `SupplierAccountReportPdfExporter.java`
Misma estructura en 2 secciones con `AreaBreak(NEXT_PAGE)`. Mismos colores estándar:
- Header: `DeviceRgb(41, 128, 185)`
- Subtotal: `DeviceRgb(220, 220, 220)`
- Total / Estado PENDIENTE: `DeviceRgb(243, 156, 18)`
- Estado CANCELADO: `DeviceRgb(120, 180, 120)`

Reusar helpers `headerCell()`, `cell()`, `cellCenter()`, `cellAmount()`.

## Controller Endpoints

```
GET /api/v1/reports/supplier-accounts
GET /api/v1/reports/supplier-accounts/download?format=PDF|EXCEL
```

**Parámetros (@RequestParam):**
- `startDate`, `endDate` — LocalDate **(obligatorios)**
- `supplierIds` — List<Long>
- `projectAreaIds` — List<Long>
- `statusFilter` — SupplierAccountStatus (opcional)
- `documentType` — DocumentType (opcional)
- `paymentMethod` — PaymentMethod (opcional)
- `minFinalBalance`, `maxFinalBalance` — BigDecimal
- `onlyWithMovementsInPeriod` — Boolean

**Permisos:**
- `REPORT_FINANCIAL` para `GET /supplier-accounts`
- `REPORT_EXPORT` para `GET /supplier-accounts/download`

## Service — Lógica de cálculo

```java
// En ReportService.java:

public SupplierAccountReportDTO generateSupplierAccountReport(SupplierAccountReportFilterDTO filters) {
    validateSupplierAccountFilters(filters);  // startDate y endDate obligatorios; startDate <= endDate

    // 1. Determinar proveedores candidatos
    List<Supplier> suppliers = resolveCandidateSuppliers(filters);
    List<Long> supplierIds = suppliers.stream().map(Supplier::getId).toList();

    // 2. Cargar datos del período en queries batch (evitar N+1)
    Map<Long, List<TransactionalDocument>> docsBySupplier =
        loadDocumentsInPeriod(supplierIds, filters);
    Map<Long, List<PaymentDetails>> paymentsBySupplier =
        loadPaymentsInPeriod(supplierIds, filters);

    // 3. Calcular saldo anterior por proveedor (queries SUM batch o por proveedor)
    Map<Long, BigDecimal> previousBalances = computePreviousBalances(supplierIds, filters.startDate());

    // 4. Construir grupos por proveedor
    List<SupplierAccountReportSupplierGroupDTO> groups = suppliers.stream()
        .map(s -> buildSupplierGroup(s, previousBalances, docsBySupplier, paymentsBySupplier))
        .filter(g -> applyStatusAndBalanceFilters(g, filters))
        .filter(g -> !Boolean.TRUE.equals(filters.onlyWithMovementsInPeriod()) || g.movementCount() > 0)
        .sorted(orderingPendingFirstByBalanceDesc())
        .toList();

    // 5. Calcular totales globales
    return buildReportDto(filters, groups);
}

private SupplierAccountReportSupplierGroupDTO buildSupplierGroup(...) {
    // a) Crear lista de movimientos: docs + payments + NC, todos como SupplierAccountMovementDTO
    // b) Ordenar cronológicamente ASC
    // c) Recorrer y calcular accumulatedBalance partiendo de previousBalance
    // d) Calcular totalDebited, totalPaid, totalCreditNotes, finalBalance
    // e) Determinar status: finalBalance.compareTo(ZERO) > 0 ? PENDIENTE : CANCELADO
}
```

**Métodos privados (mismo estilo del resto de reportes):**
- `validateSupplierAccountFilters(filters)`
- `resolveCandidateSuppliers(filters)`
- `loadDocumentsInPeriod(supplierIds, filters)`
- `loadPaymentsInPeriod(supplierIds, filters)`
- `computePreviousBalances(supplierIds, startDate)`
- `buildSupplierGroup(supplier, previousBalances, docsMap, paymentsMap)`
- `buildMovementTimeline(docs, payments, previousBalance)`
- `buildSupplierAccountPeriodDescription(filters)`

**Notas de implementación:**
- `previousBalance` y `accumulatedBalance` se calculan SIEMPRE en backend, nunca en frontend, para garantizar consistencia con Excel/PDF.
- Las NC se identifican por `DocumentType` (NOTA_CREDITO_A/B/C) y se contabilizan como **crédito**, NO como débito.
- Los pagos sin documentos imputados (pagos independientes) también se incluyen como crédito.
- Si `filters.documentType` o `filters.projectAreaIds` están seteados, filtran el conjunto de movimientos pero NO el cálculo del `previousBalance` (el saldo anterior considera siempre la totalidad histórica del proveedor).

## Frontend

### Archivo: `shared/models/supplier-account-report.model.ts`

Interfaces espejo exactas de los DTOs. Labels para `SupplierAccountStatus` y `SupplierAccountMovementType`.

```typescript
export const SupplierAccountStatusLabels: Record<string, string> = {
  PENDIENTE: 'Pendiente',
  CANCELADO: 'Cancelado',
};

export const SupplierAccountMovementTypeLabels: Record<string, string> = {
  INVOICE: 'Factura',
  DEBIT_NOTE: 'Nota de Débito',
  CREDIT_NOTE: 'Nota de Crédito',
  PAYMENT: 'Pago',
};
```

### Componente: `domains/reports/supplier-account-report/`

**Prefijo SCSS:** `sar-` (supplier-account-report)

**Filtros del sidebar:**
1. Período (Desde/Hasta) — **obligatorio**
2. Proveedores — multiselect con búsqueda
3. Sectores/Áreas — chips (filtra movimientos)
4. Estado de Saldo — select (Todos / Pendientes / Cancelados)
5. Tipo de Comprobante — select
6. Método de Pago — select
7. Rango de Saldo Final
8. Toggle: "Solo proveedores con movimientos en el período"

**KPI cards:**
1. Cantidad de Proveedores Pendientes (con badge naranja)
2. Cantidad de Proveedores Cancelados (con badge verde)
3. Saldo Pendiente Total ($)
4. Total Facturado en el Período
5. Total Pagado en el Período
6. Saldo Anterior Total

**Vista de capa 1 (Proveedores):**
Tarjetas/filas expandibles con: razón social, CUIT, badge de estado, saldo anterior, débitos, pagos, NC, **saldo final** (destacado).
Ordenamiento por defecto: PENDIENTE primero, DESC por `finalBalance`.

**Vista de capa 2 (Movimientos del proveedor expandido):**
Tabla con columnas:
| Fecha | Tipo | Referencia | Débito | Crédito | Saldo Acumulado | M. Pago |

- Fila inicial: "Saldo anterior al DD/MM/YYYY" con valor de `previousBalance`.
- Filas de movimientos con coloreado sutil (débitos en gris, créditos en verde claro, NC en celeste).
- Fila final: "Saldo final" destacado en naranja si pendiente / verde si cancelado.

**Toolbar:**
- Período actual
- Botones Expandir/Colapsar todos
- Botones Excel / PDF

### Route: `/informes/cuenta-corriente-proveedores`
### Tab label: `Cta. Cte. Proveedores`

## Particularidades

- **Reporte de 2 capas** (única excepción al patrón de 3 capas del módulo). Justificado por la naturaleza no-jerárquica de "saldo por proveedor".
- **Saldo anterior** y **saldo acumulado** son responsabilidad del backend, no del frontend.
- **NC se contabilizan como crédito**, no como débito (aunque sean `TransactionalDocument`).
- **`projectAreaIds` filtra movimientos pero NO el saldo anterior** — esa decisión preserva la veracidad del saldo histórico del proveedor.
- **No hay dimensiones dinámicas** (`Map<X, BigDecimal>`) en este reporte: todos los KPIs son escalares fijos.
- **Permisos existentes** (`REPORT_VIEW`, `REPORT_FINANCIAL`, `REPORT_EXPORT`) — sin permisos nuevos.
- **Sin migraciones de DB** — solo lectura sobre tablas existentes.
- **Mensajes i18n** — reutilizar genéricos existentes; agregar uno nuevo si se necesita: `report.supplierAccount.dateRange.required=El rango de fechas es obligatorio`.

## Checklist de Implementación

- [ ] Enums (`SupplierAccountStatus`, `SupplierAccountMovementType`)
- [ ] DTOs (4 archivos: `Report`, `SupplierGroup`, `Movement`, `Filter`)
- [ ] Repositorios: nuevos métodos en `TransactionalDocumentRepository` y `PaymentRepository`
- [ ] Interface en `IReportService` (2 métodos)
- [ ] Implementación en `ReportService` (~8 métodos privados + 2 públicos)
- [ ] Excel Exporter (2 hojas)
- [ ] PDF Exporter (2 secciones)
- [ ] 2 endpoints en `ReportController`
- [ ] Frontend model
- [ ] Frontend service
- [ ] Frontend component (`.ts`, `.html`, `.scss`)
- [ ] Route en `reports.routes.ts`
- [ ] Tab en `navigation.config.ts`
- [ ] Test manual: Preview, Excel, PDF con casos: solo pendientes, solo cancelados, mixto, proveedor sin movimientos en período pero con saldo anterior
