# Feature 12 — Reportes Independientes por Módulo

## Resumen

Replicar la arquitectura de 3 capas del reporte de salarios para 4 módulos de egreso:

| # | Módulo | Capa 1 (Agrupación principal) | Capa 2 (Agrupación secundaria) | Capa 3 (Registros individuales) |
|---|--------|-------------------------------|-------------------------------|-------------------------------|
| 1 | Facturación | Resumen por Sector | Resumen por Proveedor | Comprobantes de Compra |
| 2 | Pago de Servicios | Resumen por Sector | Resumen por Edificio | Pagos de Servicios |
| 3 | Cargas de Combustible | Resumen por Sector | Resumen por Vehículo | Cargas de Combustible |
| 4 | Reparaciones | Resumen por Sector | Resumen por Vehículo | Reparaciones |

---

## Arquitectura de Referencia (Reporte de Salarios)

### Backend — Archivos existentes

| Capa | Archivo | Función |
|------|---------|---------|
| DTO | `model/dto/report/salary/SalaryReportDTO.java` | Respuesta principal del reporte |
| DTO | `model/dto/report/salary/SalaryReportAreaGroupDTO.java` | Capa 1 — agrupación por sector |
| DTO | `model/dto/report/salary/SalaryReportEmployeeGroupDTO.java` | Capa 2 — agrupación por empleado |
| DTO | `model/dto/report/salary/SalaryReportPaymentDTO.java` | Capa 3 — pago individual |
| DTO | `model/dto/report/salary/SalaryReportFilterDTO.java` | Filtros del reporte |
| Service | `service/implementation/ReportService.java` | Métodos `generateSalaryReport`, `buildAreaGroups`, `buildEmployeeGroups` |
| Service | `service/port/IReportService.java` | Interface con `generateSalaryReport`, `generateSalaryReportFile` |
| Export | `service/export/SalaryReportExcelExporter.java` | Excel con 3 hojas |
| Export | `service/export/SalaryReportPdfExporter.java` | PDF con 3 secciones |
| Controller | `controller/ReportController.java` | Endpoints GET `/salary` y `/salary/download` |

### Frontend — Archivos existentes

| Capa | Archivo | Función |
|------|---------|---------|
| Component | `domains/reports/salary-report/salary-report.ts` | Componente principal |
| Template | `domains/reports/salary-report/salary-report.html` | Vista con 3 capas expandibles |
| Styles | `domains/reports/salary-report/salary-report.scss` | Estilos BEM con prefijo `sr-` |
| Service | `domains/reports/salary-report/services/salary-report.service.ts` | HTTP service |
| Model | `shared/models/salary-report.model.ts` | Interfaces TypeScript |
| Shared | `domains/reports/shared/report-sidebar/report-sidebar.ts` | Sidebar reutilizable |
| Routes | `domains/reports/reports.routes.ts` | Lazy-loaded routes |
| Nav | `core/config/navigation.config.ts` | Tabs de navegación |

---

## Patrón Exacto a Replicar por Cada Módulo

### 1. Backend DTOs (paquete `model/dto/report/{module}/`)

Cada módulo necesita 5 DTOs (records con `@Builder` y `@Schema`):

```
{Module}ReportDTO.java              — Igual que SalaryReportDTO
{Module}ReportAreaGroupDTO.java     — Capa 1 (igual estructura, cambia nombre de capa 2)
{Module}Report{Entity}GroupDTO.java — Capa 2 (empleado→proveedor/edificio/vehículo)
{Module}Report{Item}DTO.java        — Capa 3 (pago→comprobante/pago servicio/carga/reparación)
{Module}ReportFilterDTO.java        — Filtros específicos del módulo
```

**Campos obligatorios en el DTO principal (`{Module}ReportDTO`):**
- `filters` — los filtros aplicados
- `areaGroups` — List de grupos por sector
- `totalAmount` — BigDecimal total general
- `totalCount` — int cantidad total
- `totalsByXxx` — Map con subtotales por dimensión relevante (frecuencia en salarios, tipo combustible en fuel, etc.)
- `generatedAt` — LocalDateTime
- `reportName` — String  
- `periodDescription` — String

### 2. Backend Service (en `ReportService.java`)

Agregar 4 pares de métodos por módulo:

```java
// En IReportService.java:
{Module}ReportDTO generate{Module}Report({Module}ReportFilterDTO filters);
ResponseEntity<byte[]> generate{Module}ReportFile({Module}ReportFilterDTO filters, ReportFormat format);

// En ReportService.java (implementación):
// - Método público generateXxxReport()
// - Método privado buildXxxAreaGroups()
// - Método privado buildXxx{Entity}Groups()
// - Método privado buildXxxSubtotals() (si aplica)
// - Método privado validateXxxFilters()
// - Método privado buildXxxPeriodDescription()
```

### 3. Backend Exporters (en `service/export/`)

Cada módulo necesita 2 exporters:

```
{Module}ReportExcelExporter.java  — 3 hojas: "Resumen por Área", "Resumen por {Entity}", "Detalle"
{Module}ReportPdfExporter.java    — 3 secciones con page break
```

**Formato Excel exacto a replicar:**
- **Hoja 1 "Resumen por Área"**: Header de título + metadatos + tabla con columnas `Área | Cant. | [columnas dinámicas] | Total` + fila TOTAL GENERAL
- **Hoja 2 "Resumen por {Entity}"**: Filas agrupadas por área (con merge y fondo azul claro) + fila subtotal por área + fila TOTAL GENERAL
- **Hoja 3 "Detalle"**: Todas las transacciones con columnas específicas, subtotales por entidad, subtotales por área, TOTAL GENERAL

**Formato PDF exacto a replicar:**
- 3 secciones con `AreaBreak(NEXT_PAGE)`
- mismo header: título centrado, empresa, subtítulo, metadatos, tabla
- Colores: Header `DeviceRgb(41, 128, 185)`, Subtotal `DeviceRgb(220, 220, 220)`, Total `DeviceRgb(243, 156, 18)`
- Cell helpers: `headerCell()`, `cell()`, `cellCenter()`, `cellAmount()`

### 4. Backend Controller (en `ReportController.java`)

Agregar endpoints por módulo:

```java
@PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
@GetMapping("/{module-path}")
public ResponseEntity<{Module}ReportDTO> get{Module}Report(@RequestParam params...)

@PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
@GetMapping("/{module-path}/download")
public ResponseEntity<byte[]> download{Module}Report(@RequestParam params..., @RequestParam ReportFormat format)
```

### 5. Frontend Service (en `domains/reports/{module}-report/services/`)

```typescript
@Injectable({ providedIn: 'root' })
export class {Module}ReportService {
  get{Module}Report(filters): Observable<{Module}ReportResponse>
  download{Module}Report(format, filters): Observable<Blob>
  private buildParams(filters): HttpParams
}
```

### 6. Frontend Component (en `domains/reports/{module}-report/`)

```
{module}-report.ts       — Component con expand/collapse, formatters, filter state
{module}-report.html     — Sidebar + KPIs + toolbar + 3 capas jerárquicas
{module}-report.scss     — Copiar estructura sr-* → {prefix}-*
```

**Estructura HTML obligatoria:**
1. `<app-report-sidebar>` con filtros del módulo
2. KPI cards (Total General, Cantidad, subtotales por dimensión)
3. Toolbar (período, Expandir/Colapsar todo, botones Excel/PDF)
4. Grupos jerárquicos expandibles (Área → Entity → tabla de detalle)
5. Footer con TOTAL GENERAL

### 7. Frontend Models (en `shared/models/`)

```typescript
{module}-report.model.ts — Interfaces espejo de los DTOs del backend
```

### 8. Routes y Navegación

```typescript
// reports.routes.ts — agregar child route
{ path: '{module-path}', loadComponent: async () => { ... } }

// navigation.config.ts — agregar sección al módulo 'informes'
{ id: '{module-path}', label: '{Label}', permissions: ['REPORT_FINANCIAL'] }
```

### 9. Permisos (NO se necesitan nuevos)

Los permisos existentes son suficientes:
- `REPORT_VIEW` — ver reportes
- `REPORT_FINANCIAL` — generar reportes financieros
- `REPORT_EXPORT` — exportar a archivo

### 10. Migraciones

NO se necesitan migraciones de base de datos. Los reportes son de lectura pura sobre tablas existentes.

### 11. Mensajes i18n (en `messages_es.properties`)

Ya existen los mensajes genéricos de error:
```properties
report.generation.excel.error=Error al generar el reporte Excel
report.generation.pdf.error=Error al generar el reporte PDF
report.filters.null=Los filtros no pueden ser nulos
```

No se necesitan mensajes nuevos a menos que se agreguen validaciones específicas por módulo.

---

## Orden de Implementación Recomendado

1. **Facturación** — Es el módulo de facturación (comprobantes de compra), agrupa por proveedor
2. **Pago de Servicios** — Agrupa por edificio, tiene dimensión año/período
3. **Cargas de Combustible** — Agrupa por vehículo, tiene tipo de combustible como dimensión
4. **Reparaciones** — Agrupa por vehículo, tiene items de mano de obra + materiales

Cada módulo se implementa end-to-end: DTOs → Service → Exporter → Controller → Frontend.

---

## Checklist de Implementación por Módulo

- [ ] DTOs del reporte (5 archivos: Report, AreaGroup, EntityGroup, ItemDetail, Filter)
- [ ] Interface en IReportService (2 métodos)
- [ ] Implementación en ReportService (6 métodos privados + 2 públicos)
- [ ] Excel Exporter (3 hojas)
- [ ] PDF Exporter (3 secciones)
- [ ] Endpoints en ReportController (2 endpoints)
- [ ] Frontend model (interfaces TypeScript)
- [ ] Frontend service (2 métodos HTTP)
- [ ] Frontend component (.ts, .html, .scss)
- [ ] Route en reports.routes.ts
- [ ] Tab en navigation.config.ts
- [ ] Test manual: Preview, Excel, PDF
