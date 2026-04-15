# Reporte de Pago de Servicios

## Estructura de Capas

| Capa | Agrupación | Entidad fuente | Campo de agrupación |
|------|-----------|----------------|---------------------|
| 1 | Sector / Área | ProjectArea | `servicePayment.projectArea` |
| 2 | Edificio | Building (via ServiceAssignment) | `servicePayment.serviceAssignment.building` |
| 3 | Pago individual | ServicePayment | Cada pago de servicio |

**Nota:** ServicePayment se vincula a un ServiceAssignment, que a su vez puede estar asociado a un Building o a un Vehicle. Para este reporte, la agrupación principal de capa 2 es **por Edificio** (building). Los pagos que NO tienen edificio (asignados a vehículo u otro) se agrupan en un grupo "Sin Edificio asignado".

## Entidad Fuente: ServicePayment

**Ubicación:** `model/entity/serviceSupplier/ServicePayment.java`

**Campos relevantes:**
- `id` — Long
- `serviceAssignment` — ServiceAssignment (ManyToOne, required)
  - `serviceAssignment.serviceSupplier` → ServiceSupplier (nombre, CUIT, tipo servicio)
  - `serviceAssignment.building` → Building (nombre, dirección) **o** null
  - `serviceAssignment.vehicle` → Vehicle (patente) **o** null
  - `serviceAssignment.subjectType` — enum (BUILDING, VEHICLE)
  - `serviceAssignment.serviceType` — enum
  - `serviceAssignment.accountNumber`, `accountHolder`
- `projectArea` — ProjectArea (ManyToOne)
- `projectAreaTask` — ProjectAreaTask (ManyToOne, optional)
- `paymentDate` — LocalDate
- `amount` — BigDecimal
- `year` — Integer (2000-2100)
- `period` — Integer (1-12, mes)
- `referenceNumber` — String (unique per tenant)
- `comment` — String
- `paymentMethod` — PaymentMethod enum

**Datos del Repository existente:** `ServicePaymentRepository`
- Filtros existentes: subjectType, serviceAssignmentId, serviceSupplierId, buildingId, vehicleId, projectAreaId, serviceType, year, period, dateRange, amountRange

## DTOs a Crear

### `model/dto/report/servicePayment/ServicePaymentReportDTO.java`
```java
@Builder
public record ServicePaymentReportDTO(
    ServicePaymentReportFilterDTO filters,
    List<ServicePaymentReportAreaGroupDTO> areaGroups,
    BigDecimal totalAmount,
    int totalCount,
    Map<String, BigDecimal> totalsByServiceType,  // Dimensión: tipo de servicio (ELECTRICIDAD, GAS, AGUA, etc.)
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
```

### `model/dto/report/servicePayment/ServicePaymentReportAreaGroupDTO.java`
```java
@Builder
public record ServicePaymentReportAreaGroupDTO(
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    BigDecimal subtotalAmount,
    int paymentCount,
    Map<String, BigDecimal> subtotalsByServiceType,
    List<ServicePaymentReportBuildingGroupDTO> buildingGroups
) {}
```

### `model/dto/report/servicePayment/ServicePaymentReportBuildingGroupDTO.java`
```java
@Builder
public record ServicePaymentReportBuildingGroupDTO(
    Long buildingId,                 // null si no tiene edificio
    String buildingName,             // "Sin Edificio asignado" si null
    BigDecimal totalAmount,
    int paymentCount,
    Map<String, BigDecimal> subtotalsByServiceType,
    List<ServicePaymentReportItemDTO> payments
) {}
```

### `model/dto/report/servicePayment/ServicePaymentReportItemDTO.java`
```java
public record ServicePaymentReportItemDTO(
    Long id,
    LocalDate paymentDate,
    BigDecimal amount,
    Integer year,
    Integer period,
    String serviceType,
    String supplierName,
    String supplierTradeName,
    String referenceNumber,
    PaymentMethod paymentMethod,
    Long projectAreaTaskId,
    String projectAreaTaskName
) {}
```

### `model/dto/report/servicePayment/ServicePaymentReportFilterDTO.java`
```java
public record ServicePaymentReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<Long> projectAreaIds,
    String serviceType,               // filtro por tipo de servicio
    PaymentMethod paymentMethod,
    Integer year,                     // filtro por año
    Integer period,                   // filtro por período/mes
    BigDecimal minAmount,
    BigDecimal maxAmount
) {}
```

## Exporters

### Excel — `ServicePaymentReportExcelExporter.java`

**Hoja 1: "Resumen por Área"**
Columnas: `Área | Cant. Pagos | [columna por cada ServiceType activo] | Total`

**Hoja 2: "Resumen por Edificio"**
Agrupado por área → filas de edificios → subtotal área → TOTAL GENERAL
Columnas: `Edificio | [ServiceTypes] | Total`

**Hoja 3: "Detalle de Pagos"**
Columnas: `Área | Edificio | Proveedor | Fecha | Tipo Servicio | Año/Período | Referencia | Método Pago | Monto`
Con subtotales por edificio y por área.

### PDF — `ServicePaymentReportPdfExporter.java`
Misma estructura en 3 secciones. Mismos colores.

## Controller Endpoints

```
GET /api/v1/reports/service-payments
GET /api/v1/reports/service-payments/download?format=PDF|EXCEL
```

**Parámetros:**
- `startDate`, `endDate` — LocalDate
- `projectAreaIds` — List<Long>
- `serviceType` — String (opcional)
- `paymentMethod` — PaymentMethod (opcional)
- `year` — Integer (opcional)
- `period` — Integer (opcional)
- `minAmount`, `maxAmount` — BigDecimal

## Service — Lógica de agrupación

```java
public ServicePaymentReportDTO generateServicePaymentReport(ServicePaymentReportFilterDTO filters) {
    validateServicePaymentFilters(filters);
    // 1. Obtener ServicePayments filtrados
    // 2. Agrupar por projectArea
    // 3. Dentro de cada área, agrupar por building (via serviceAssignment.building)
    //    - Si serviceAssignment.building es null → grupo "Sin Edificio asignado"
    // 4. Dentro de cada building, listar pagos ordenados por fecha desc
    // 5. Calcular subtotales por serviceType en cada nivel
}
```

**Acceso a datos:** Necesitarás join fetch en la query o usar los repositorios existentes. La agrupación por building requiere navegar `servicePayment.serviceAssignment.building`.

## Frontend

### Archivo: `shared/models/service-payment-report.model.ts`

### Componente: `domains/reports/service-payment-report/`

**Prefijo SCSS:** `spr-` (service-payment-report)

**Filtros del sidebar:**
1. Período (Desde/Hasta)
2. Sectores/Áreas — chips
3. Tipo de Servicio — select (Todos, Electricidad, Gas, Agua, Internet, etc.)
4. Método de Pago — select
5. Año — number input
6. Período/Mes — select (1-12)
7. Rango de Montos

**KPI cards:**
1. Total General ($)
2. Cantidad de Pagos
3. Un KPI por cada tipo de servicio activo

**Tabla de detalle (Capa 3):**
| Fecha | Proveedor | Tipo Servicio | Año/Período | Referencia | M. Pago | Monto |

### Route: `/informes/servicios`
### Tab label: `Servicios`

## Columnas dinámicas

La dimensión dinámica es `ServiceType` (tipo string del enum de servicio).
Solo se muestran tipos de servicio que tienen datos.
