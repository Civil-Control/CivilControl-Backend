# Reporte de Cargas de Combustible

## Estructura de Capas

| Capa | Agrupación | Entidad fuente | Campo de agrupación |
|------|-----------|----------------|---------------------|
| 1 | Sector / Área | ProjectArea | `fuelLoad.projectArea` |
| 2 | Vehículo | Vehicle | `fuelLoad.vehicle` |
| 3 | Carga individual | FuelLoad | Cada carga de combustible |

**Nota:** FuelLoad puede tener `vehicle = null` (caso de bidones/tambores). Esas cargas se agrupan en un grupo "Sin Vehículo asignado".

## Entidad Fuente: FuelLoad

**Ubicación:** `model/entity/gasStation/FuelLoad.java`

**Campos relevantes:**
- `id` — Long
- `date` — LocalDate
- `branchCode` — String (5 dígitos, código sucursal)
- `ticketNumber` — String (8 dígitos)
- `fuelType` — FuelType enum (NAFTA_SUPER, NAFTA_PREMIUM, GASOIL, GASOIL_PREMIUM, GNC)
- `liters` — BigDecimal (0.01 - 5000)
- `pricePerLiter` — BigDecimal
- `totalAmount` — BigDecimal (calculado: liters × pricePerLiter)
- `vehicle` — Vehicle (ManyToOne, optional)
  - `vehicle.licensePlate` — String (patente)
  - `vehicle.brand`, `vehicle.model` — datos descriptivos
- `gasStation` — GasStation (ManyToOne, required)
  - `gasStation.name` — nombre de la estación
- `projectArea` — ProjectArea (ManyToOne)
- `projectAreaTask` — ProjectAreaTask (ManyToOne, optional)
- `transactionalDocument` — TransactionalDocument (ManyToOne, optional — vinculación a factura)

**Datos del Repository existente:** `FuelLoadRepository`
- Filtros: dateFrom/dateTo, branchCode, ticketNumber, fuelType, vehicleId, vehicleLicensePlate, projectAreaId, gasStationId, totalAmountRange

## DTOs a Crear

### `model/dto/report/fuelLoad/FuelLoadReportDTO.java`
```java
@Builder
public record FuelLoadReportDTO(
    FuelLoadReportFilterDTO filters,
    List<FuelLoadReportAreaGroupDTO> areaGroups,
    BigDecimal totalAmount,
    int totalCount,
    BigDecimal totalLiters,                        // Total de litros cargados
    Map<FuelType, BigDecimal> totalsByFuelType,    // Dimensión: tipo de combustible
    Map<FuelType, BigDecimal> litersByFuelType,    // Litros por tipo
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
```

### `model/dto/report/fuelLoad/FuelLoadReportAreaGroupDTO.java`
```java
@Builder
public record FuelLoadReportAreaGroupDTO(
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    BigDecimal subtotalAmount,
    int loadCount,
    BigDecimal subtotalLiters,
    Map<FuelType, BigDecimal> subtotalsByFuelType,
    List<FuelLoadReportVehicleGroupDTO> vehicleGroups
) {}
```

### `model/dto/report/fuelLoad/FuelLoadReportVehicleGroupDTO.java`
```java
@Builder
public record FuelLoadReportVehicleGroupDTO(
    Long vehicleId,                   // null si no tiene vehículo
    String vehicleLicensePlate,       // "Sin Vehículo" si null
    String vehicleBrand,
    String vehicleModel,
    BigDecimal totalAmount,
    int loadCount,
    BigDecimal totalLiters,
    Map<FuelType, BigDecimal> subtotalsByFuelType,
    List<FuelLoadReportItemDTO> loads
) {}
```

### `model/dto/report/fuelLoad/FuelLoadReportItemDTO.java`
```java
public record FuelLoadReportItemDTO(
    Long id,
    LocalDate date,
    FuelType fuelType,
    BigDecimal liters,
    BigDecimal pricePerLiter,
    BigDecimal totalAmount,
    String gasStationName,
    String ticketNumber,
    String branchCode,
    Long projectAreaTaskId,
    String projectAreaTaskName,
    Boolean hasLinkedDocument
) {}
```

### `model/dto/report/fuelLoad/FuelLoadReportFilterDTO.java`
```java
public record FuelLoadReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<Long> projectAreaIds,
    FuelType fuelType,                // filtro por tipo de combustible
    Long vehicleId,                   // filtro por vehículo específico
    Long gasStationId,                // filtro por estación
    BigDecimal minAmount,
    BigDecimal maxAmount
) {}
```

## Exporters

### Excel — `FuelLoadReportExcelExporter.java`

**Hoja 1: "Resumen por Área"**
Columnas: `Área | Cant. Cargas | Litros | [columna $ por cada FuelType activo] | Total $`

**Hoja 2: "Resumen por Vehículo"**
Agrupado por área → filas de vehículos → subtotal área → TOTAL GENERAL
Columnas: `Vehículo (Patente) | Marca/Modelo | Litros | [FuelTypes $] | Total $`

**Hoja 3: "Detalle de Cargas"**
Columnas: `Área | Vehículo | Fecha | Tipo Comb. | Litros | $/Litro | Total | Estación | Ticket`
Con subtotales por vehículo y por área.

### PDF — `FuelLoadReportPdfExporter.java`
Misma estructura en 3 secciones. Mismos colores.

## Controller Endpoints

```
GET /api/v1/reports/fuel-loads
GET /api/v1/reports/fuel-loads/download?format=PDF|EXCEL
```

**Parámetros:**
- `startDate`, `endDate` — LocalDate
- `projectAreaIds` — List<Long>
- `fuelType` — FuelType (opcional)
- `vehicleId` — Long (opcional)
- `gasStationId` — Long (opcional)
- `minAmount`, `maxAmount` — BigDecimal

## Service — Lógica de agrupación

```java
public FuelLoadReportDTO generateFuelLoadReport(FuelLoadReportFilterDTO filters) {
    validateFuelLoadFilters(filters);
    // 1. Obtener FuelLoads filtrados (usar findAllWithFiltersNoPage o query custom)
    // 2. Agrupar por projectArea
    // 3. Dentro de cada área, agrupar por vehicle
    //    - FuelLoads con vehicle == null → grupo "Sin Vehículo asignado"
    // 4. Dentro de cada vehículo, listar cargas ordenadas por fecha desc
    // 5. Calcular subtotales por fuelType en cada nivel
    // 6. Calcular litros totales en cada nivel
}
```

## Frontend

### Archivo: `shared/models/fuel-load-report.model.ts`

Incluir enum/labels de FuelType:
```typescript
export const FuelTypeLabels: Record<string, string> = {
  NAFTA_SUPER: 'Nafta Súper',
  NAFTA_PREMIUM: 'Nafta Premium',
  GASOIL: 'Gasoil',
  GASOIL_PREMIUM: 'Gasoil Premium',
  GNC: 'GNC',
};
```

### Componente: `domains/reports/fuel-load-report/`

**Prefijo SCSS:** `flr-` (fuel-load-report)

**Filtros del sidebar:**
1. Período (Desde/Hasta)
2. Sectores/Áreas — chips
3. Tipo de Combustible — select (Todos, Nafta Súper, Nafta Premium, Gasoil, Gasoil Premium, GNC)
4. Vehículo — búsqueda por patente (opcional, input text)
5. Estación de Servicio — select o búsqueda
6. Rango de Montos

**KPI cards:**
1. Total General ($)
2. Cantidad de Cargas
3. Total Litros
4. Un KPI por cada tipo de combustible activo ($)

**Tabla de detalle (Capa 3):**
| Fecha | Tipo Comb. | Litros | $/Litro | Total | Estación | Ticket |

### Route: `/informes/combustible`
### Tab label: `Combustible`

## Columnas dinámicas

La dimensión dinámica es `FuelType`.
Solo se muestran tipos de combustible que tienen datos.
Orden fijo: NAFTA_SUPER → NAFTA_PREMIUM → GASOIL → GASOIL_PREMIUM → GNC

## Particularidades

- Este reporte tiene una métrica adicional que los otros no: **litros totales** (además del monto $). Se muestra en KPIs, en cada nivel de agrupación, y en las hojas de Excel/PDF.
- El campo `pricePerLiter` permite análisis de precio por litro pero solo se muestra en la capa 3 de detalle.
- El campo `hasLinkedDocument` indica si la carga está vinculada a un comprobante de compra (solo informativo).
