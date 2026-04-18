# FEATURE-12E: Reporte de Compras de Stock

## Resumen

Reporte financiero que muestra las compras de stock realizadas en un período, con agrupación jerárquica por **Categoría de Stock** → **Item de Stock**, mostrando cantidades, precios unitarios y totales. Sigue la misma arquitectura que los reportes ya implementados (salarios, facturación, servicios, combustible, reparaciones).

---

## Modelo de Datos Involucrado

### Entidades principales
- **StockPurchase**: `id`, `date`, `stockId` (FK), `quantity`, `unitPrice`, `totalAmount`, `notes`, `transactionalDocumentId` (FK opcional), `documentSortOrder`
- **Stock**: `id`, `name`, `stockCategory` (enum), `quantity`, `deleted`, `building` (ManyToOne → Building)
- **Building**: `id`, `name`, `code`, `buildingType`, `projectArea` (ManyToOne → ProjectArea)

### Enums
- **StockCategory**: HERRAMIENTAS_MANUALES, HERRAMIENTAS_ELECTRICAS, EQUIPOS_PESADOS, MAQUINARIA, INSUMOS, SEGURIDAD_PERSONAL, ROPA_TRABAJO, EQUIPAMIENTO_OBRA, ACCESORIO_VEHICULAR, LIMPIEZA_MANTENIMIENTO, REPUESTOS, OTROS

### Dimensiones de agrupación
1. **Categoría de Stock** (StockCategory) — nivel superior
2. **Item de Stock** (Stock.name) — nivel inferior

> Nota: No se agrupa por Área/Edificio porque StockPurchase usa `stockId` como FK simple (Long), no como relación JPA, y no todos los stocks tienen edificio asignado. La categoría del stock es la dimensión natural de agrupación.

---

## Backend

### DTOs (`model/dto/report/stockPurchase/`)

#### StockPurchaseReportFilterDTO
```java
record StockPurchaseReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<String> stockCategories,   // Filtro por categorías (enum names)
    Long stockId,                   // Filtro por item específico
    BigDecimal minAmount,
    BigDecimal maxAmount
)
```

#### StockPurchaseReportDTO (respuesta principal)
```java
@Builder
record StockPurchaseReportDTO(
    StockPurchaseReportFilterDTO filters,
    List<StockPurchaseReportCategoryGroupDTO> categoryGroups,
    BigDecimal totalAmount,
    int totalCount,
    BigDecimal totalQuantity,
    Map<String, BigDecimal> totalsByCategory,  // categoryDisplayName → subtotal
    LocalDateTime generatedAt,
    String reportName,           // "Reporte de Compras de Stock"
    String periodDescription     // "Período: 01/03/2026 - 31/03/2026"
)
```

#### StockPurchaseReportCategoryGroupDTO
```java
@Builder
record StockPurchaseReportCategoryGroupDTO(
    String categoryName,         // StockCategory.displayName
    String categoryKey,          // StockCategory.name() para key en frontend
    BigDecimal subtotalAmount,
    int purchaseCount,
    BigDecimal subtotalQuantity,
    List<StockPurchaseReportStockGroupDTO> stockGroups
)
```

#### StockPurchaseReportStockGroupDTO
```java
@Builder
record StockPurchaseReportStockGroupDTO(
    Long stockId,
    String stockName,
    BigDecimal totalAmount,
    int purchaseCount,
    BigDecimal totalQuantity,
    List<StockPurchaseReportItemDTO> purchases
)
```

#### StockPurchaseReportItemDTO
```java
record StockPurchaseReportItemDTO(
    Long id,
    LocalDate date,
    String stockName,              // Para vista plana
    String stockCategoryName,      // Para vista plana
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    String notes,
    boolean hasLinkedDocument
)
```

### Servicio (ReportService)

Métodos a agregar en `IReportService` y `ReportService`:

- `StockPurchaseReportDTO generateStockPurchaseReport(StockPurchaseReportFilterDTO filters)`
- `ResponseEntity<byte[]> generateStockPurchaseReportFile(StockPurchaseReportFilterDTO filters, ReportFormat format)`

#### Lógica de generación:
1. Validar filtros (startDate/endDate requeridos)
2. Consultar `StockPurchaseRepository.findAllWithFilters(...)` con `Pageable.ofSize(10000)`
3. Para cada `StockPurchase`, obtener el `Stock` asociado via `StockRepository.findById(sp.getStockId())`
4. Agrupar por `Stock.stockCategory` (nivel 1), luego por `Stock.id` (nivel 2)
5. Calcular subtotales por categoría y por item
6. Construir la jerarquía de DTOs

### Controller (ReportController)

Dos endpoints nuevos:

- `GET /reports/stock-purchases` → `generateStockPurchaseReport()`
  - Params: `startDate`, `endDate`, `stockCategories` (lista), `stockId`, `minAmount`, `maxAmount`
- `GET /reports/stock-purchases/download` → `downloadStockPurchaseReport()`
  - Params: los mismos + `format` (EXCEL/PDF)

### Exportadores

#### StockPurchaseReportExcelExporter
3 hojas:
1. **Resumen por Categoría**: Categoría | Cant. Compras | Cantidad Total | Total ($)
2. **Resumen por Item**: Agrupado por categoría (header coloreado), Item | Cant. Compras | Cantidad | Total ($), con subtotales
3. **Detalle de Compras**: Categoría | Item | Fecha | Cantidad | P. Unitario | Total | Notas, con subtotales por item y categoría

#### StockPurchaseReportPdfExporter
3 secciones (con page break):
1. Resumen por Categoría
2. Resumen por Item de Stock
3. Detalle de Compras

---

## Frontend

### Modelo (`shared/models/stock-purchase-report.model.ts`)

```typescript
interface StockPurchaseReportFilters {
    startDate: string | null;
    endDate: string | null;
    stockCategories: string[];
    stockId: number | null;
    minAmount: number | null;
    maxAmount: number | null;
}

interface StockPurchaseReportItem { ... }         // Espeja StockPurchaseReportItemDTO
interface StockPurchaseReportStockGroup { ... }   // Espeja StockPurchaseReportStockGroupDTO
interface StockPurchaseReportCategoryGroup { ... } // Espeja StockPurchaseReportCategoryGroupDTO
interface StockPurchaseReportResponse { ... }      // Espeja StockPurchaseReportDTO
```

### Servicio (`domains/reports/stock-purchase-report/services/stock-purchase-report.service.ts`)

- `getStockPurchaseReport(filters)` → GET `/reports/stock-purchases`
- `downloadStockPurchaseReport(format, filters)` → GET `/reports/stock-purchases/download` con `responseType: 'blob'`

### Componente (`domains/reports/stock-purchase-report/`)

**Prefix SCSS:** `spr-` (stock-purchase-report)

**Sidebar — Filtros:**
| Filtro | Tipo | Comportamiento |
|--------|------|----------------|
| Período | Dos inputs date (Desde/Hasta) | Default: último mes |
| Categorías de Stock | Chips seleccionables | Valores del enum StockCategory. Vacío = todas |
| Rango de Montos | Dos inputs number (Mín/Máx) | Permite filtrar por totalAmount |

**Sidebar — Agrupación:**
| Toggle | Default |
|--------|---------|
| Categoría | ✅ ON |
| Item de Stock | ✅ ON |

**KPI Cards:**
| KPI | Valor |
|-----|-------|
| Total General | `totalAmount` formateado como moneda |
| Cant. Compras | `totalCount` |
| Cantidad Total | `totalQuantity` formateado como número |

**Tabla — Columnas:**
| Columna | Visible siempre | Ancho |
|---------|----------------|-------|
| Fecha | ✅ | 85px |
| Categoría | Solo si `!groupByCategory` | 140px |
| Item | Solo si `!groupByStock` | 140px |
| Cantidad | ✅ | 90px |
| P. Unitario | ✅ | 100px |
| Total | ✅ | 110px |
| Notas | ✅ | flexible |

**Sorting:** Por fecha (desc default), cantidad, precio unitario, total

**4 modos de visualización:**
1. Categoría + Item → jerarquía completa
2. Solo Categoría → compras agrupadas por categoría
3. Solo Item → items sin agrupar por categoría (merged)
4. Flat → tabla plana con columnas dinámicas

### Ruta y Navegación
- **Route**: `/informes/compras-stock`
- **Tab label**: `Compras de Stock`
- **Permission**: `REPORT_FINANCIAL`
- En `reports.routes.ts`: lazy load del componente `StockPurchaseReport`
- En `navigation.config.ts`: nuevo item en la sección Informes

---

## Archivos a crear/modificar

### Backend (crear)
- `model/dto/report/stockPurchase/StockPurchaseReportFilterDTO.java`
- `model/dto/report/stockPurchase/StockPurchaseReportDTO.java`
- `model/dto/report/stockPurchase/StockPurchaseReportCategoryGroupDTO.java`
- `model/dto/report/stockPurchase/StockPurchaseReportStockGroupDTO.java`
- `model/dto/report/stockPurchase/StockPurchaseReportItemDTO.java`
- `service/export/StockPurchaseReportExcelExporter.java`
- `service/export/StockPurchaseReportPdfExporter.java`

### Backend (modificar)
- `service/port/IReportService.java` — agregar 2 métodos
- `service/implementation/ReportService.java` — implementar generación + helpers
- `controller/ReportController.java` — agregar 2 endpoints

### Frontend (crear)
- `shared/models/stock-purchase-report.model.ts`
- `domains/reports/stock-purchase-report/services/stock-purchase-report.service.ts`
- `domains/reports/stock-purchase-report/stock-purchase-report.ts`
- `domains/reports/stock-purchase-report/stock-purchase-report.html`
- `domains/reports/stock-purchase-report/stock-purchase-report.scss`

### Frontend (modificar)
- `shared/models/index.ts` — agregar export
- `domains/reports/reports.routes.ts` — agregar ruta
- `core/config/navigation.config.ts` — agregar tab
