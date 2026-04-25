# Reporte de Ventas

## Modelo de "venta" — fuente dual

En el sistema una venta puede materializarse por dos caminos **independientes**:

1. **Comprobante de Venta** (`SalesDocument`) — factura, nota de crédito o nota de débito.
2. **Certificación** (`Certification`) — avance de obra de un `WorkContract`, con su propio ciclo de estados.

Los ciclos *facturación → cobro* y *certificación → cobro* son **ortogonales**: una venta puede estar certificada y cobrada sin haber sido facturada todavía, certificada y luego facturada y cobrada, o facturada y cobrada sin certificación. Para que el reporte refleje **todas las ventas reales** sin duplicar montos, la fuente del reporte es la unión deduplicada de:

- **Todos los `SalesDocument`** del período (incluye las certificaciones que ya están vinculadas a uno — la certificación viaja como **metadato** del SD, no como fila propia).
- **Las `Certification` huérfanas** del período, definidas como aquellas con `salesDocument IS NULL` (incluye las que tienen `status = COBRADO` pero todavía no se facturaron).

> **Regla de no duplicación:** una `Certification` con `salesDocument` no nulo NUNCA produce una fila propia en la capa de detalle. Su monto certificado se reporta junto al `SalesDocument` que la representa contablemente. Solo las **huérfanas** (sin SD) producen filas propias y aportan al `totalCertifiedOnly`. Esto asegura que ningún peso se cuenta dos veces.

| Caso real | ¿Aparece en el reporte? | ¿Cómo? |
|---|---|---|
| Facturada y cobrada (sin certificación) | Sí | Fila `SalesDocument` |
| Facturada, certificada y cobrada | Sí | Fila `SalesDocument` con la cert. como metadato |
| Certificada y cobrada (sin facturar) | Sí | Fila `Certification` huérfana (status = `COBRADO`) |
| Certificada `PRESENTADO` / `APROBADO` (sin facturar) | Sí | Fila `Certification` huérfana |
| Certificada `FACTURADO` (sin SD vinculado) | Sí | Fila `Certification` huérfana — caso anómalo de datos pero no se omite |

## Estructura de Capas

| Capa | Agrupación | Entidades fuente | Campo de agrupación |
|------|-----------|------------------|---------------------|
| 1 | Sector / Área | ProjectArea | `salesDocument.projectArea` o `certification.contract.projectArea` |
| 2 | Cliente | Client | `salesDocument.client` o `certification.contract.client` |
| 3 | Venta individual | SalesDocument **o** Certification huérfana | Una fila por entidad de la unión |

Las certificaciones huérfanas se agrupan reusando los campos del contrato (`contract.client` y `contract.projectArea`) — `WorkContract.client` y `WorkContract.projectArea` son no-nulos, así que toda huérfana cae siempre en un grupo válido.

## Entidades fuente

### `SalesDocument` — `model/entity/sales/SalesDocument.java`

**Campos relevantes para el reporte:**
- `id` — Long
- `documentType` — SalesDocumentType enum (FACTURA_A, FACTURA_B, FACTURA_C, NOTA_DEBITO_A/B/C, NOTA_CREDITO_A/B/C)
- `branchCode` — String (5 dígitos, punto de venta AFIP)
- `documentNumber` — String (8 dígitos, número comprobante AFIP)
- `date` — LocalDate (fecha de emisión)
- `client` — Client (ManyToOne) → razón social, nombre fantasía, CUIT, condición IVA
- `purchaseOrderReference` — String (opcional, número pedido del cliente)
- `netTotal` — BigDecimal
- `ivaTotal` — BigDecimal
- `ivaExemptTotal` — BigDecimal
- `otherTaxes` — BigDecimal
- `total` — BigDecimal
- `discountPercentage` — BigDecimal
- `projectArea` — ProjectArea (ManyToOne)
- `paid` — Boolean (cobrado / pendiente de cobro)
- `comment` — String

### `Certification` — `model/entity/contracts/Certification.java`

**Campos relevantes para el reporte:**
- `id` — Long
- `certificationNumber` — Integer
- `certificationDate` — LocalDate (fecha de presentación / emisión de la certificación)
- `certifiedAmount` — BigDecimal
- `status` — `CertificationStatus` (PRESENTADO, APROBADO, FACTURADO, COBRADO)
- `salesDocument` — SalesDocument (ManyToOne, **opcional** — si es null, es huérfana)
- `contract` — WorkContract (`contractNumber`, `description`, `client`, `projectArea`, `projectAreaTask`)
- `comment` — String

### Repositorios

**`SalesDocumentRepository`**
- Filtros existentes: clientId, documentType, dateFrom/dateTo, minTotal/maxTotal, paid, projectAreaId
- **Agregar si no existe:** `findAllWithFiltersNoPage(...)` análogo al de `TransactionalDocumentRepository`

**`CertificationRepository`**
- **Agregar:** `List<Certification> findBySalesDocumentIdIn(Collection<Long> salesDocumentIds)` — para resolver vínculos en una sola query y evitar N+1 al adjuntar metadata a los SDs.
- **Agregar:** `List<Certification> findOrphansForReport(LocalDate startDate, LocalDate endDate, List<Long> projectAreaIds, List<Long> clientIds, Long workContractId, CertificationStatus status, BigDecimal minAmount, BigDecimal maxAmount)` — devuelve certificaciones con `salesDocument IS NULL` filtradas sobre `certificationDate`, `contract.projectArea.id IN :areaIds`, `contract.client.id IN :clientIds`, `contract.id = :workContractId`, `status`, `certifiedAmount BETWEEN minAmount AND maxAmount`.

## DTOs a Crear

### `model/dto/report/sales/SalesReportDTO.java`
```java
@Builder
public record SalesReportDTO(
    SalesReportFilterDTO filters,
    List<SalesReportAreaGroupDTO> areaGroups,
    BigDecimal totalAmount,                  // facturado + certificado-huérfano
    int totalCount,                          // SDs + certificaciones huérfanas
    Map<SalesDocumentType, BigDecimal> totalsByDocumentType,
    BigDecimal totalNet,
    BigDecimal totalIva,
    BigDecimal totalIvaExempt,
    BigDecimal totalOtherTaxes,
    BigDecimal totalInvoiced,                // suma de SalesDocument.total
    BigDecimal totalCertifiedOnly,           // suma de Certification.certifiedAmount (solo huérfanas)
    BigDecimal totalCertifiedLinked,         // suma de certifiedAmount de certs vinculadas a un SD (informativo, NO se suma a totalAmount)
    BigDecimal totalPaidAmount,              // SD.paid=true + Certification(huérfana).status=COBRADO
    BigDecimal totalUnpaidAmount,
    int invoiceCount,
    int certificationOnlyCount,
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
```

### `model/dto/report/sales/SalesReportAreaGroupDTO.java`
```java
@Builder
public record SalesReportAreaGroupDTO(
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    BigDecimal subtotalAmount,
    int rowCount,
    Map<SalesDocumentType, BigDecimal> subtotalsByDocumentType,
    BigDecimal subtotalNet,
    BigDecimal subtotalIva,
    BigDecimal subtotalInvoiced,
    BigDecimal subtotalCertifiedOnly,
    List<SalesReportClientGroupDTO> clientGroups
) {}
```

### `model/dto/report/sales/SalesReportClientGroupDTO.java`
```java
@Builder
public record SalesReportClientGroupDTO(
    Long clientId,
    String clientBusinessName,
    String clientTradeName,
    String clientCuit,
    IvaCondition clientIvaCondition,
    BigDecimal totalAmount,
    int rowCount,
    Map<SalesDocumentType, BigDecimal> subtotalsByDocumentType,
    BigDecimal subtotalNet,
    BigDecimal subtotalIva,
    BigDecimal subtotalInvoiced,
    BigDecimal subtotalCertifiedOnly,
    List<SalesReportRowDTO> rows                       // unión ordenada por fecha desc
) {}
```

### `model/dto/report/sales/SalesReportRowDTO.java`

Una sola lista por cliente, discriminada por `kind`. Mantiene la jerarquía de 3 capas y simplifica el orden cronológico.

```java
public record SalesReportRowDTO(
    SalesReportRowKind kind,                            // INVOICE | CERTIFICATION_ONLY
    LocalDate date,                                     // SD.date | Certification.certificationDate
    BigDecimal amount,                                  // SD.total | Certification.certifiedAmount
    Boolean paid,                                       // SD.paid | (status == COBRADO)

    // Campos específicos de SalesDocument (null si kind = CERTIFICATION_ONLY)
    Long salesDocumentId,
    SalesDocumentType documentType,
    String branchCode,
    String documentNumber,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    BigDecimal ivaExemptTotal,
    BigDecimal otherTaxes,
    String purchaseOrderReference,
    Long projectAreaTaskId,
    String projectAreaTaskName,
    List<SalesReportCertificationLinkDTO> linkedCertifications,   // certs con salesDocumentId == this.id

    // Campos específicos de Certification huérfana (null si kind = INVOICE)
    Long certificationId,
    Integer certificationNumber,
    CertificationStatus certificationStatus,
    Long contractId,
    String contractNumber,
    String contractDescription
) {}
```

### `model/dto/report/sales/SalesReportRowKind.java`
```java
public enum SalesReportRowKind {
    INVOICE,                // SalesDocument (factura/NC/ND)
    CERTIFICATION_ONLY      // Certificación sin SalesDocument vinculado (huérfana)
}
```

### `model/dto/report/sales/SalesReportCertificationLinkDTO.java`
```java
public record SalesReportCertificationLinkDTO(
    Long certificationId,
    Integer certificationNumber,
    BigDecimal certifiedAmount,
    CertificationStatus status,
    Long contractId,
    String contractNumber,
    String contractDescription
) {}
```

### `model/dto/report/sales/SalesReportFilterDTO.java`
```java
public record SalesReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<Long> projectAreaIds,
    List<Long> clientIds,
    SalesDocumentType documentType,                  // filtra solo filas INVOICE
    IvaCondition ivaCondition,
    Boolean paid,                                    // ver "Reinterpretación del filtro paid"
    Long workContractId,                             // aplica a SDs vía sus certs vinculadas y a huérfanas vía contract.id
    Boolean onlyLinkedToCertifications,              // true = solo SDs con cert vinculada (excluye huérfanas y SDs sin cert)
    Boolean includeCertificationsOnly,               // true (default) = incluir filas CERTIFICATION_ONLY; false = solo facturas
    CertificationStatus certificationStatus,         // filtra solo filas CERTIFICATION_ONLY (huérfanas)
    BigDecimal minAmount,
    BigDecimal maxAmount
) {}
```

### Reinterpretación del filtro `paid`

- `paid = true` → incluir `SalesDocument.paid = true` **y** `Certification(huérfana).status = COBRADO`.
- `paid = false` → incluir `SalesDocument.paid = false` **y** `Certification(huérfana).status ≠ COBRADO`.
- `paid = null` → no filtrar.

Si el usuario combina `paid = true` con `certificationStatus = APROBADO` (contradicción para huérfanas), el filtro más específico (`certificationStatus`) gana sobre las huérfanas y `paid` se aplica solo a los SDs.

Si `onlyLinkedToCertifications = true`, las filas `CERTIFICATION_ONLY` se excluyen automáticamente (la combinación es semánticamente inválida).

Si `includeCertificationsOnly = false`, las huérfanas se excluyen sin importar el resto de filtros.

## Exporters

### Excel — `SalesReportExcelExporter.java`

**Hoja 1: "Resumen por Área"**
Columnas: `Área | Facturas | Certif. sueltas | [columna por cada SalesDocumentType activo] | Neto | IVA | Total Facturado | Total Certif. sin facturar | Total`
Fila final: TOTAL GENERAL.

**Hoja 2: "Resumen por Cliente"**
Agrupado por área (fila merge azul claro) → filas de clientes → subtotal área → TOTAL GENERAL.
Columnas: `Cliente (Razón Social) | CUIT | Cond. IVA | Facturas | Certif. sueltas | [SalesDocumentTypes] | Neto | IVA | Total Facturado | Total Certif. sin facturar | Total`

**Hoja 3: "Detalle de Ventas"**
Una sola tabla unificada con columna `Origen` que distingue `Factura` vs `Certificación`.
Columnas: `Área | Cliente | Fecha | Origen | Tipo Doc. | PV-Nro | Neto | IVA | Exento | Otros Imp. | Total | Cobrado | Contrato(s) | Certif. N° | Monto Certif.`

Reglas de llenado:
- **Filas `INVOICE`:** `Origen = "Factura"`, `Tipo Doc.` = enum traducido, `PV-Nro` = `branchCode-documentNumber`, columnas impositivas con sus valores, `Cobrado` = sí/no, `Contrato(s)/Certif. N°/Monto Certif.` se concatenan con `;` si hay múltiples certs vinculadas (vacío si no hay ninguna).
- **Filas `CERTIFICATION_ONLY`:** `Origen = "Certificación"`, `Tipo Doc.` muestra `CERTIF. (status)` (ej. `CERTIF. (PRESENTADO)`), `PV-Nro` vacío, `Neto/IVA/Exento/Otros Imp.` vacíos, `Total` = `certifiedAmount`, `Cobrado` = sí/no según `status == COBRADO`, `Contrato`/`Certif. N°`/`Monto Certif.` se llenan con datos de la propia certificación.
- Subtotales por cliente y por área. Fila final TOTAL GENERAL con dos sub-líneas: "Facturado" y "Certificado sin facturar".

### PDF — `SalesReportPdfExporter.java`
Misma estructura en 3 secciones con `AreaBreak(NEXT_PAGE)`. Mismos colores y formateo que `SalaryReportPdfExporter`:
- Header: `DeviceRgb(41, 128, 185)`
- Subtotal: `DeviceRgb(220, 220, 220)`
- Total: `DeviceRgb(243, 156, 18)`
- Filas `CERTIFICATION_ONLY`: fondo `DeviceRgb(252, 248, 227)` (amarillo claro) para distinguirlas visualmente.

Reusar helpers `headerCell()`, `cell()`, `cellCenter()`, `cellAmount()`.

## Controller Endpoints

```
GET /api/v1/reports/sales
GET /api/v1/reports/sales/download?format=PDF|EXCEL
```

**Parámetros (@RequestParam):**
- `startDate`, `endDate` — LocalDate
- `projectAreaIds` — List<Long>
- `clientIds` — List<Long>
- `documentType` — SalesDocumentType (opcional, solo afecta INVOICE)
- `ivaCondition` — IvaCondition (opcional)
- `paid` — Boolean (opcional)
- `workContractId` — Long (opcional)
- `onlyLinkedToCertifications` — Boolean (opcional)
- `includeCertificationsOnly` — Boolean (default true)
- `certificationStatus` — CertificationStatus (opcional, solo afecta CERTIFICATION_ONLY)
- `minAmount`, `maxAmount` — BigDecimal

## Service — Lógica de agrupación

```java
// En ReportService.java:

public SalesReportDTO generateSalesReport(SalesReportFilterDTO filters) {
    validateSalesFilters(filters);

    // --- Fuente 1: SalesDocuments ---
    List<SalesDocument> docs = salesDocumentRepository.findAllWithFiltersNoPage(...);
    List<Long> docIds = docs.stream().map(SalesDocument::getId).toList();
    Map<Long, List<Certification>> certIndex = certificationRepository
        .findBySalesDocumentIdIn(docIds)
        .stream()
        .collect(groupingBy(c -> c.getSalesDocument().getId()));

    // Aplicar filtros que el repo no soporta nativamente:
    // - workContractId            -> docs con al menos una cert en certIndex cuyo contract.id == workContractId
    // - onlyLinkedToCertifications -> docs con certIndex no vacío

    // --- Fuente 2: Certificaciones huérfanas (solo si includeCertificationsOnly != false y onlyLinkedToCertifications != true) ---
    List<Certification> orphans = Boolean.TRUE.equals(filters.onlyLinkedToCertifications())
        || Boolean.FALSE.equals(filters.includeCertificationsOnly())
            ? List.of()
            : certificationRepository.findOrphansForReport(
                filters.startDate(), filters.endDate(),
                filters.projectAreaIds(), filters.clientIds(),
                filters.workContractId(),
                filters.certificationStatus(),
                filters.minAmount(), filters.maxAmount());

    // Filtro paid sobre huérfanas: status==COBRADO si paid==true, status!=COBRADO si paid==false
    if (filters.paid() != null && filters.certificationStatus() == null) {
        orphans = orphans.stream()
            .filter(c -> filters.paid() == (c.getStatus() == CertificationStatus.COBRADO))
            .toList();
    }
    // Filtro ivaCondition sobre huérfanas: contract.client.ivaCondition

    // --- Construir filas unificadas ---
    List<SalesReportRowDTO> invoiceRows = docs.stream()
        .map(d -> toInvoiceRow(d, certIndex.getOrDefault(d.getId(), List.of())))
        .toList();
    List<SalesReportRowDTO> certOnlyRows = orphans.stream()
        .map(this::toCertificationOnlyRow)
        .toList();

    // --- Agrupar (área -> cliente -> filas) sobre la unión, ordenando por fecha desc ---
    List<SalesReportAreaGroupDTO> areaGroups = buildSalesAreaGroups(invoiceRows, certOnlyRows);

    // --- KPIs ---
    BigDecimal totalInvoiced       = sum(invoiceRows, SalesReportRowDTO::amount);
    BigDecimal totalCertifiedOnly  = sum(certOnlyRows, SalesReportRowDTO::amount);
    BigDecimal totalAmount         = totalInvoiced.add(totalCertifiedOnly);
    BigDecimal totalCertifiedLinked = certIndex.values().stream()
        .flatMap(List::stream)
        .map(Certification::getCertifiedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    // ... totalsByDocumentType, totalNet/IVA/Exempt/OtherTaxes, totalPaid/Unpaid

    return SalesReportDTO.builder()...build();
}
```

**Métodos privados:**
- `validateSalesFilters(filters)`
- `toInvoiceRow(SalesDocument doc, List<Certification> linkedCerts)` — clave dedupe: las certs linkeadas viajan acá como metadata, no como fila.
- `toCertificationOnlyRow(Certification orphan)` — solo si `salesDocument == null`.
- `buildSalesAreaGroups(invoiceRows, certOnlyRows)` — extrae área desde el SD o desde `contract.projectArea` según `kind`.
- `buildSalesClientGroups(rowsForArea)`
- `buildSalesTotalsByDocumentType(invoiceRows)` — solo cuenta filas INVOICE.
- `buildSalesPeriodDescription(filters)`

**Optimización N+1:**
- Una sola query a `CertificationRepository.findBySalesDocumentIdIn(docIds)` para resolver metadata de SDs.
- Una sola query a `CertificationRepository.findOrphansForReport(...)` con `JOIN FETCH contract, contract.client, contract.projectArea` para evitar lazy loads al construir filas y subtotales.

**Nota:** Las **Notas de Crédito** se incluyen sumando con su signo natural en el total general (mismo comportamiento que el reporte de Facturación de compras). Aparecen como columnas dinámicas independientes en las hojas de resumen, igual que `NOTA_CREDITO_A/B/C` en `InvoiceReport`.

## Frontend

### Archivo: `shared/models/sales-report.model.ts`

Interfaces espejo exactas de los DTOs del backend, incluido el discriminator `SalesReportRowKind`. Reusar enum/labels de `SalesDocumentType`, `IvaCondition` y `CertificationStatus` ya existentes en frontend.

### Componente: `domains/reports/sales-report/`

**Prefijo SCSS:** `slr-` (sales-report)

**Filtros del sidebar:**
1. Período (Desde/Hasta) — igual que salarios
2. Sectores/Áreas — chips
3. Clientes — multiselect con búsqueda
4. Tipo de Comprobante — select (Todos, Factura A/B/C, NC A/B/C, ND A/B/C) — solo afecta facturas
5. Condición IVA del Cliente — select (Todos, RI, Monotributista, Exento, Consumidor Final)
6. Estado de Cobro — select (Todos, Cobrado, Pendiente)
7. Contrato — selector con búsqueda (opcional)
8. Estado de Certificación — select (Todos, Presentado, Aprobado, Facturado, Cobrado) — solo afecta filas de certificaciones sueltas
9. Toggle: "Incluir certificaciones sin facturar" (default ON)
10. Toggle: "Solo facturas con certificación vinculada" (mutuamente excluyente con el anterior; al activarse desactiva y bloquea el #9)
11. Rango de Montos

**KPI cards:**
1. Total General ($) — facturado + certificado sin facturar
2. Total Facturado / Total Certificado sin facturar (split visual)
3. Total Neto / Total IVA
4. Cantidad: Facturas + Certif. sueltas
5. Cobrado vs Pendiente (con %)
6. Total Certificado vinculado a facturas (informativo, no se suma)
7. Un KPI por cada `SalesDocumentType` con datos

**Tabla de detalle (Capa 3):**
| Origen | Fecha | Tipo / Estado | PV-Nro / Certif. N° | Neto | IVA | Total | Cobrado | Contrato |

- Filas `INVOICE`: badge azul "Factura", muestra `documentType` traducido, `PV-Nro`, columnas impositivas, certs vinculadas listadas en columna `Contrato` con `;`.
- Filas `CERTIFICATION_ONLY`: badge ámbar "Certificación" + chip de `status`, muestra `Certif. N°`, columnas Neto/IVA vacías o con guion, `Total` = monto certificado, `Contrato` = `contractNumber - description`.
- Concatenación con `;` o fila expandible secundaria si hay múltiples certs vinculadas a una factura.

### Route: `/informes/ventas`
### Tab label: `Ventas`

## Columnas dinámicas

La dimensión dinámica es `SalesDocumentType` (equivalente a `DocumentType` en el reporte de facturación de compras). Solo se muestran las columnas de tipos que realmente tienen datos en el reporte generado. Las filas `CERTIFICATION_ONLY` no participan de estas columnas (aportan a `subtotalCertifiedOnly` aparte).

Orden de tipos: `FACTURA_A → FACTURA_B → FACTURA_C → NOTA_DEBITO_A → NOTA_DEBITO_B → NOTA_DEBITO_C → NOTA_CREDITO_A → NOTA_CREDITO_B → NOTA_CREDITO_C`

## Particularidades

- **Fuente dual deduplicada:** primer reporte que combina dos entidades fuente (`SalesDocument` + `Certification` huérfana) en una capa de detalle unificada con discriminator `kind`. La regla "una cert con SD vinculado nunca produce fila propia" garantiza que no haya doble conteo de pesos.
- **Cobertura completa de las ventas reales del sistema:** facturadas, certificadas-sin-facturar, certificadas-y-facturadas, todas las combinaciones cobradas/pendientes.
- **Agrupación por Cliente** — primer reporte con esta capa, simétrica al agrupamiento por Proveedor del reporte de Facturación. Para certificaciones huérfanas el cliente se obtiene vía `contract.client`.
- **Datos de entidades vinculadas en la capa de detalle** sin romper la jerarquía de 3 capas: las certs vinculadas viajan como `linkedCertifications` dentro de la fila INVOICE, no como capa adicional.
- **Desagregación impositiva más rica** que Facturación: Neto + IVA + Exento + Otros Impuestos. Las filas `CERTIFICATION_ONLY` no la tienen (no son comprobantes fiscales).
- **Filtro `paid` reinterpretado** para que abarque ambas fuentes coherentemente (`SD.paid` ↔ `Certification.status == COBRADO`).
- `IvaCondition` se usa como **filtro y dato descriptivo del cliente**, NO como dimensión dinámica de columnas.
