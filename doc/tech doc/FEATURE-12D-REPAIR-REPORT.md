# Reporte de Reparaciones

## Estructura de Capas

| Capa | Agrupación | Entidad fuente | Campo de agrupación |
|------|-----------|----------------|---------------------|
| 1 | Sector / Área | ProjectArea | `repair.vehicle.projectArea` (área del vehículo) |
| 2 | Vehículo | Vehicle | `repair.vehicle` |
| 3 | Reparación individual | Repair + RepairItems | Cada reparación con sus ítems |

**Nota:** Repair NO tiene projectArea directamente. El sector se obtiene del vehículo (`repair.vehicle.projectArea`). Si el vehículo no tiene área, va al grupo "Sin Área asignada".

## Entidades Fuente

### Repair (`model/entity/vehicle/Repair.java`)

**Campos relevantes:**
- `id` — Long
- `date` — LocalDate
- `vehicle` — Vehicle (ManyToOne, required)
  - `vehicle.licensePlate` — String (patente)
  - `vehicle.brand`, `vehicle.model`
  - `vehicle.projectArea` — ProjectArea del vehículo
- `description` — String
- `mileage` — Integer (km al momento de la reparación, optional)
- `supplier` — Supplier (ManyToOne, optional — para reparaciones externas)
  - `supplier.legalName`, `supplier.tradeName`, `supplier.cuit`
- `items` — List<RepairItem> (OneToMany)
- `repairOrder` — RepairOrder (ManyToOne, optional)

### RepairItem (`model/entity/vehicle/RepairItem.java`)

- `id` — Long
- `itemType` — RepairItemType enum (MATERIAL, MANO_DE_OBRA)
- `description` — String
- `amount` — BigDecimal (nullable)
- `transactionalDocument` — TransactionalDocument (optional — vinculación a factura)
- `sortOrder` — Integer

**Costo de reparación:** Se calcula sumando `amount` de todos los items. Los items pueden no tener monto (amount null).

**Datos del Repository existente:** `RepairRepository`
- Filtros: dateFrom/dateTo, vehicleId, vehicleLicensePlate, projectAreaId, costRange (min/max), supplierId

## DTOs a Crear

### `model/dto/report/repair/RepairReportDTO.java`
```java
@Builder
public record RepairReportDTO(
    RepairReportFilterDTO filters,
    List<RepairReportAreaGroupDTO> areaGroups,
    BigDecimal totalAmount,
    int totalCount,
    BigDecimal totalMaterialCost,                    // Total materiales
    BigDecimal totalLaborCost,                       // Total mano de obra
    Map<RepairItemType, BigDecimal> totalsByItemType, // Dimensión: tipo de ítem
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
```

### `model/dto/report/repair/RepairReportAreaGroupDTO.java`
```java
@Builder
public record RepairReportAreaGroupDTO(
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    BigDecimal subtotalAmount,
    int repairCount,
    BigDecimal materialSubtotal,
    BigDecimal laborSubtotal,
    List<RepairReportVehicleGroupDTO> vehicleGroups
) {}
```

### `model/dto/report/repair/RepairReportVehicleGroupDTO.java`
```java
@Builder
public record RepairReportVehicleGroupDTO(
    Long vehicleId,
    String vehicleLicensePlate,
    String vehicleBrand,
    String vehicleModel,
    BigDecimal totalAmount,
    int repairCount,
    BigDecimal materialSubtotal,
    BigDecimal laborSubtotal,
    List<RepairReportItemDTO> repairs
) {}
```

### `model/dto/report/repair/RepairReportItemDTO.java`
```java
public record RepairReportItemDTO(
    Long id,
    LocalDate date,
    String description,
    Integer mileage,
    BigDecimal materialCost,        // suma de items MATERIAL
    BigDecimal laborCost,           // suma de items MANO_DE_OBRA
    BigDecimal totalCost,           // materialCost + laborCost
    String supplierName,            // nombre del proveedor (si externo)
    int itemCount,                  // cantidad de ítems
    Long projectAreaTaskId,
    String projectAreaTaskName,
    Boolean hasLinkedDocuments      // si algún item tiene doc vinculado
) {}
```

### `model/dto/report/repair/RepairReportFilterDTO.java`
```java
public record RepairReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<Long> projectAreaIds,
    Long vehicleId,               // filtro por vehículo específico
    Long supplierId,              // filtro por proveedor (reparaciones externas)
    BigDecimal minAmount,
    BigDecimal maxAmount
) {}
```

## Exporters

### Excel — `RepairReportExcelExporter.java`

**Hoja 1: "Resumen por Área"**
Columnas: `Área | Cant. Reparaciones | Materiales $ | Mano de Obra $ | Total $`

**Hoja 2: "Resumen por Vehículo"**
Agrupado por área → filas de vehículos → subtotal área → TOTAL GENERAL
Columnas: `Vehículo (Patente) | Marca/Modelo | Cant. Rep. | Materiales $ | Mano de Obra $ | Total $`

**Hoja 3: "Detalle de Reparaciones"**
Columnas: `Área | Vehículo | Fecha | Descripción | Km | Proveedor | Materiales $ | M.O. $ | Total $ | Ítems`
Con subtotales por vehículo y por área.

### PDF — `RepairReportPdfExporter.java`
Misma estructura en 3 secciones. Mismos colores.

## Controller Endpoints

```
GET /api/v1/reports/repairs
GET /api/v1/reports/repairs/download?format=PDF|EXCEL
```

**Parámetros:**
- `startDate`, `endDate` — LocalDate
- `projectAreaIds` — List<Long>
- `vehicleId` — Long (opcional)
- `supplierId` — Long (opcional)
- `minAmount`, `maxAmount` — BigDecimal

## Service — Lógica de agrupación

```java
public RepairReportDTO generateRepairReport(RepairReportFilterDTO filters) {
    validateRepairFilters(filters);
    // 1. Obtener Repairs filtrados CON items cargados (JOIN FETCH items)
    // 2. Para cada repair, calcular materialCost y laborCost sumando items por tipo
    // 3. Agrupar por vehicle.projectArea (NO repair.projectArea, que no existe)
    //    - Si vehicle.projectArea == null → grupo "Sin Área asignada"
    // 4. Dentro de cada área, agrupar por vehicle
    // 5. Dentro de cada vehículo, listar reparaciones ordenadas por fecha desc
    // 6. Calcular materialSubtotal y laborSubtotal en cada nivel
}
```

**Importante:**
- La entidad Repair NO tiene campo `projectArea` propio. Se usa `repair.vehicle.projectArea`.
- El costo total se calcula sumando items, no hay campo `cost` en Repair (fue removido en V54).
- Hay que hacer JOIN FETCH de `items` para evitar N+1 queries.

## Frontend

### Archivo: `shared/models/repair-report.model.ts`

### Componente: `domains/reports/repair-report/`

**Prefijo SCSS:** `rr-` (repair-report)

**Filtros del sidebar:**
1. Período (Desde/Hasta)
2. Sectores/Áreas — chips
3. Vehículo — búsqueda por patente (opcional, input text)
4. Proveedor — búsqueda por nombre (opcional, para filtrar reparaciones externas)
5. Rango de Montos

**KPI cards:**
1. Total General ($)
2. Cantidad de Reparaciones
3. Total Materiales ($)
4. Total Mano de Obra ($)

**Tabla de detalle (Capa 3):**
| Fecha | Descripción | Km | Proveedor | Materiales $ | M.O. $ | Total $ |

### Route: `/informes/reparaciones`
### Tab label: `Reparaciones`

## Columnas dinámicas

En este módulo la dimensión dinámica son las dos categorías fijas: **Materiales** y **Mano de Obra** (RepairItemType). No son dinámicas como en los otros módulos. Siempre se muestran ambas columnas.

## Particularidades

- **Cálculo de costos:** A diferencia de otros módulos donde el monto está en la entidad principal, aquí hay que iterar los RepairItem y sumar montos por tipo.
- **Proveedor opcional:** Solo reparaciones externas tienen proveedor. Reparaciones internas muestran "-" o "Interno".
- **Kilometraje:** Se muestra en la capa de detalle pero no en las capas de resumen. Es un dato informativo.
- **Items sin monto:** Algunos RepairItem pueden tener `amount = null`. Se cuentan como $0 en las sumas.
- **Área vía vehículo:** Recordar que el área se obtiene de `repair.vehicle.projectArea`, no de la reparación directamente.
