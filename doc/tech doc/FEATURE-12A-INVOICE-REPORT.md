# Reporte de Facturación (Comprobantes de Compra)

## Estructura de Capas

| Capa | Agrupación | Entidad fuente | Campo de agrupación |
|------|-----------|----------------|---------------------|
| 1 | Sector / Área | ProjectArea | `transactionalDocument.projectArea` |
| 2 | Proveedor | Supplier | `transactionalDocument.supplier` |
| 3 | Comprobante individual | TransactionalDocument | Cada comprobante |

## Entidad Fuente: TransactionalDocument

**Ubicación:** `model/entity/TransactionalDocument.java`

**Campos relevantes para el reporte:**
- `id` — Long
- `documentType` — DocumentType enum (FACTURA_A, FACTURA_B, FACTURA_C, NOTA_CREDITO_A, etc.)
- `branchCode` — String (código sucursal, 5 dígitos)
- `documentNumber` — String (número comprobante, 8 dígitos)
- `date` — LocalDate
- `supplier` — Supplier (ManyToOne) → nombre legal, nombre fantasía, CUIT
- `totalAmount` — BigDecimal (monto total del comprobante)
- `netTotal` — BigDecimal
- `ivaTotal` — BigDecimal  
- `projectArea` — ProjectArea (ManyToOne)
- `projectAreaTask` — ProjectAreaTask (ManyToOne)
- `paymentMethod` — PaymentMethod enum
- `comment` — String
- `paid` — Boolean

**Datos del Repository existente:** `TransactionalDocumentRepository`
- Ya tiene `findAllWithFilters` y `findAllWithFiltersNoPage`
- Filtros existentes: supplierId, documentType, dateFrom/dateTo, minTotal/maxTotal, paid, projectAreaId

## DTOs a Crear

### `model/dto/report/invoice/InvoiceReportDTO.java`
```java
@Builder
public record InvoiceReportDTO(
    InvoiceReportFilterDTO filters,
    List<InvoiceReportAreaGroupDTO> areaGroups,
    BigDecimal totalAmount,
    int totalCount,
    Map<DocumentType, BigDecimal> totalsByDocumentType,  // Dimensión: tipo de documento
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
```

### `model/dto/report/invoice/InvoiceReportAreaGroupDTO.java`
```java
@Builder
public record InvoiceReportAreaGroupDTO(
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    BigDecimal subtotalAmount,
    int documentCount,
    Map<DocumentType, BigDecimal> subtotalsByDocumentType,
    List<InvoiceReportSupplierGroupDTO> supplierGroups
) {}
```

### `model/dto/report/invoice/InvoiceReportSupplierGroupDTO.java`
```java
@Builder
public record InvoiceReportSupplierGroupDTO(
    Long supplierId,
    String supplierLegalName,
    String supplierTradeName,
    String supplierCuit,
    BigDecimal totalAmount,
    int documentCount,
    Map<DocumentType, BigDecimal> subtotalsByDocumentType,
    List<InvoiceReportDocumentDTO> documents
) {}
```

### `model/dto/report/invoice/InvoiceReportDocumentDTO.java`
```java
public record InvoiceReportDocumentDTO(
    Long id,
    LocalDate date,
    DocumentType documentType,
    String branchCode,
    String documentNumber,
    BigDecimal totalAmount,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    PaymentMethod paymentMethod,
    Long projectAreaTaskId,
    String projectAreaTaskName,
    Boolean paid
) {}
```

### `model/dto/report/invoice/InvoiceReportFilterDTO.java`
```java
public record InvoiceReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<Long> projectAreaIds,
    DocumentType documentType,         // filtro opcional por tipo
    PaymentMethod paymentMethod,      // filtro opcional por método de pago
    Boolean paid,                      // filtro por estado de pago
    BigDecimal minAmount,
    BigDecimal maxAmount
) {}
```

## Exporters

### Excel — `InvoiceReportExcelExporter.java`

**Hoja 1: "Resumen por Área"**
Columnas: `Área | Cant. Docs | [columna por cada DocumentType activo] | Total`

**Hoja 2: "Resumen por Proveedor"**
Agrupado por área (fila merge azul) → filas de proveedores → subtotal área → TOTAL GENERAL
Columnas: `Proveedor (Razón Social) | CUIT | [DocumentTypes] | Total`

**Hoja 3: "Detalle de Comprobantes"**
Columnas: `Área | Proveedor | Fecha | Tipo Doc. | Punto Venta-Nro | Neto | IVA | Total | Método Pago | Pagado`
Con subtotales por proveedor y por área.

### PDF — `InvoiceReportPdfExporter.java`
Misma estructura en 3 secciones con page break. Mismos colores y formateo que SalaryReportPdfExporter.

## Controller Endpoints

```
GET /api/v1/reports/invoices
GET /api/v1/reports/invoices/download?format=PDF|EXCEL
```

**Parámetros (@RequestParam):**
- `startDate`, `endDate` — LocalDate
- `projectAreaIds` — List<Long>
- `documentType` — DocumentType (opcional)
- `paymentMethod` — PaymentMethod (opcional)
- `paid` — Boolean (opcional)
- `minAmount`, `maxAmount` — BigDecimal

## Service — Lógica de agrupación

```java
// En ReportService.java:

public InvoiceReportDTO generateInvoiceReport(InvoiceReportFilterDTO filters) {
    validateInvoiceFilters(filters);
    // 1. Obtener TransactionalDocuments filtrados por fecha, área, tipo, monto, etc.
    // 2. Agrupar por projectArea (stream → groupingBy → sorted)
    // 3. Dentro de cada área, agrupar por supplier
    // 4. Dentro de cada supplier, listar docs ordenados por fecha desc
    // 5. Calcular subtotales por DocumentType en cada nivel
    // 6. Construir InvoiceReportDTO
}
```

**Nota:** Los TransactionalDocuments son la entidad principal (comprobantes de compra/facturas de proveedor). NO confundir con SalesDocument (comprobantes de venta a clientes).

## Frontend

### Archivo: `shared/models/invoice-report.model.ts`

Interfaces espejo exactas de los DTOs del backend. Usar `DocumentType` enum existente en el frontend si existe, o crear labels.

### Componente: `domains/reports/invoice-report/`

**Prefijo SCSS:** `ir-` (invoice-report)

**Filtros del sidebar:**
1. Período (Desde/Hasta) — igual que salarios
2. Sectores/Áreas — chips, igual que salarios
3. Tipo de Comprobante — select (Todos, Factura A, Factura B, Factura C, NC A, NC B, NC C, etc.)
4. Método de Pago — select (Todos, Efectivo, Transferencia, Cheque)
5. Estado de Pago — select (Todos, Pagado, No Pagado)
6. Rango de Montos — igual que salarios

**KPI cards:**
1. Total General ($)
2. Cantidad de Comprobantes
3. Un KPI por cada tipo de documento que tenga registros

**Tabla de detalle (Capa 3):**
| Fecha | Tipo | PV-Nro | Neto | IVA | Total | M. Pago | Pagado |

### Route: `/informes/facturacion`
### Tab label: `Facturación`

## Columnas dinámicas

La dimensión dinámica es `DocumentType` (equivalente a `SalaryFrequency` en salarios).
Solo se muestran las columnas de tipos de documento que realmente tienen datos en el reporte generado.

Orden de tipos: FACTURA_A → FACTURA_B → FACTURA_C → NOTA_CREDITO_A → NOTA_CREDITO_B → NOTA_CREDITO_C → (otros)
