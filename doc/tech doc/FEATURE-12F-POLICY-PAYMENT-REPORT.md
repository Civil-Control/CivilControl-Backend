# FEATURE-12F: Reporte de Pagos de Póliza

## Resumen

Reporte financiero que muestra los pagos realizados a pólizas de seguro en un período determinado. A diferencia de otros reportes, el eje principal es el **pago** (PolicyPayment), no la póliza en sí. Para pólizas de tipo AUTOMOTOR, se discrimina el pago mostrando los vehículos asegurados y su premio mensual individual, permitiendo al usuario visualizar la **diferencia entre el monto pagado y el premio esperado** de la póliza.

### Particularidades clave
1. **Lo que se reporta es el egreso** (PolicyPayment.amount), no atributos de la póliza
2. **Cada pago se vincula a una póliza**, y la póliza tiene un `premioMensual` esperado
3. **Diferencia pago vs. premio**: si el pago difiere del premio mensual de la póliza, se muestra la diferencia (positiva = pagó de más, negativa = pagó de menos)
4. **Discriminación por vehículo** (solo para AUTOMOTOR): cada póliza automotor tiene `PolicyVehicle[]` con su propio `premioMensual`, permitiendo desglosar qué porción del pago corresponde a cada vehículo
5. **Filtro de tipo de póliza**: aunque actualmente solo existe AUTOMOTOR, se muestra el filtro para escalabilidad futura

---

## Modelo de Datos Involucrado

### Entidades principales
- **PolicyPayment**: `id`, `insurancePolicy` (ManyToOne), `paymentDate`, `amount`, `periodFrom`, `periodTo`, `notes`, `deleted`
- **InsurancePolicy**: `id`, `policyNumber`, `termNumber`, `policyType` (enum), `policyStatus` (enum), `paymentFrequency` (enum), `premioTotal`, `premioMensual`, `periodicDueDay`, `effectiveFrom`, `effectiveTo`, `autoPolicy` (OneToOne)
- **AutoPolicy**: `id`, `insurancePolicy` (OneToOne), `policyVehicles` (OneToMany → PolicyVehicle)
- **PolicyVehicle**: `id`, `vehicle` (ManyToOne → Vehicle), `autoPolicy` (ManyToOne), `sumInsured`, `premioTotal`, `premioMensual`, `effectiveFrom`, `effectiveTo`, `cancellationDate`
- **Vehicle**: `id`, `licensePlate`, `brand`, `model`, `projectArea` (ManyToOne → ProjectArea)

### Enums
- **PolicyType**: AUTOMOTOR("automotor")
- **PolicyStatus**: COTIZADO, ACTIVO, VENCIDO, CANCELADO, EXPIRADO
- **PaymentFrequency**: MENSUAL, BIMESTRAL, TRIMESTRAL, SEMI_ANUAL, ANUAL, PAGO_UNICO

### Dimensiones de agrupación
1. **Tipo de Póliza** (PolicyType) — nivel superior
2. **Póliza** (InsurancePolicy.policyNumber) — nivel inferior

> El tipo de póliza como agrupador superior permite que cuando se agreguen nuevos tipos (ej: vida, incendio), los pagos se agrupen naturalmente.

---

## Backend

### DTOs (`model/dto/report/policyPayment/`)

#### PolicyPaymentReportFilterDTO
```java
record PolicyPaymentReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<String> policyTypes,       // Filtro por tipo de póliza (enum names)
    Long insurancePolicyId,         // Filtro por póliza específica
    BigDecimal minAmount,
    BigDecimal maxAmount
)
```

#### PolicyPaymentReportDTO (respuesta principal)
```java
@Builder
record PolicyPaymentReportDTO(
    PolicyPaymentReportFilterDTO filters,
    List<PolicyPaymentReportTypeGroupDTO> typeGroups,
    BigDecimal totalPaidAmount,          // Suma de todos los pagos
    BigDecimal totalExpectedAmount,       // Suma de premioMensual de las pólizas pagadas
    BigDecimal totalDifference,           // totalPaidAmount - totalExpectedAmount
    int totalPaymentCount,
    int totalPolicyCount,                 // Cant. de pólizas distintas con pagos
    LocalDateTime generatedAt,
    String reportName,                    // "Reporte de Pagos de Póliza"
    String periodDescription
)
```

#### PolicyPaymentReportTypeGroupDTO
```java
@Builder
record PolicyPaymentReportTypeGroupDTO(
    String policyTypeName,               // PolicyType.displayName ("automotor")
    String policyTypeKey,                // PolicyType.name() ("AUTOMOTOR")
    BigDecimal subtotalPaid,
    BigDecimal subtotalExpected,
    BigDecimal subtotalDifference,
    int paymentCount,
    int policyCount,
    List<PolicyPaymentReportPolicyGroupDTO> policyGroups
)
```

#### PolicyPaymentReportPolicyGroupDTO
```java
@Builder
record PolicyPaymentReportPolicyGroupDTO(
    Long insurancePolicyId,
    String policyNumber,
    String termNumber,
    String policyStatus,                  // displayName
    String paymentFrequency,              // displayName
    BigDecimal premioMensual,             // Premio mensual de la póliza
    BigDecimal totalPaid,                 // Suma de pagos de esta póliza en el período
    BigDecimal expectedAmount,            // premioMensual × cant. meses del período (o pagos)
    BigDecimal difference,                // totalPaid - expectedAmount
    int paymentCount,
    List<PolicyPaymentReportPaymentDTO> payments,
    List<PolicyPaymentReportVehicleDTO> insuredVehicles  // Solo para AUTOMOTOR, null para otros tipos
)
```

#### PolicyPaymentReportPaymentDTO
```java
record PolicyPaymentReportPaymentDTO(
    Long id,
    LocalDate paymentDate,
    BigDecimal amount,
    LocalDate periodFrom,
    LocalDate periodTo,
    String notes,
    String policyNumber,                  // Para vista plana
    String policyTypeName,                // Para vista plana
    BigDecimal premioMensual,             // Premio esperado de la póliza
    BigDecimal difference                 // amount - premioMensual
)
```

#### PolicyPaymentReportVehicleDTO
```java
record PolicyPaymentReportVehicleDTO(
    Long vehicleId,
    String licensePlate,
    String brand,
    String model,
    BigDecimal premioMensual,            // Premio mensual del vehículo en la póliza
    BigDecimal sumInsured,               // Suma asegurada del vehículo
    String projectAreaName               // Área del vehículo
)
```

### Servicio (ReportService)

Métodos a agregar en `IReportService` y `ReportService`:

- `PolicyPaymentReportDTO generatePolicyPaymentReport(PolicyPaymentReportFilterDTO filters)`
- `ResponseEntity<byte[]> generatePolicyPaymentReportFile(PolicyPaymentReportFilterDTO filters, ReportFormat format)`

#### Lógica de generación:
1. Validar filtros (startDate/endDate requeridos)
2. Consultar `PolicyPaymentRepository.findAllWithFilters(...)` con parámetros mapeados y `Pageable.ofSize(10000)`
   - Para el filtro de `policyTypes`, se itera y filtra después (el repo no tiene ese filtro directo, se hará post-query o se agrega al query)
3. Para cada `PolicyPayment`, acceder a `payment.getInsurancePolicy()` para obtener datos de la póliza
4. Agrupar por `InsurancePolicy.policyType` (nivel 1), luego por `InsurancePolicy.id` (nivel 2)
5. Para cada póliza AUTOMOTOR: acceder a `insurancePolicy.getAutoPolicy().getPolicyVehicles()` para obtener vehículos asegurados y sus premios individuales
6. Calcular diferencias:
   - **Por pago**: `payment.amount - insurancePolicy.premioMensual`
   - **Por póliza en el período**: `sumaPagos - (premioMensual × cantidadPagos)` (se usa la cantidad de pagos como referencia, no los meses del período, para simplificar)
7. Construir jerarquía de DTOs

#### Query personalizado necesario:
Se necesita extender `PolicyPaymentRepository` o hacer un query que permita filtrar por `policyType`. Opciones:
- **Opción A**: Agregar un nuevo query `findAllForReport(...)` que haga JOIN con InsurancePolicy y filtre por policyType
- **Opción B**: Usar el query existente `findAllWithFilters(...)` y filtrar por policyType en memoria (aceptable dado el `Pageable.ofSize(10000)`)

Se elige **Opción B** para mantener consistencia con otros reportes y evitar modificar el repo existente.

### Controller (ReportController)

Dos endpoints nuevos:

- `GET /reports/policy-payments` → `generatePolicyPaymentReport()`
  - Params: `startDate`, `endDate`, `policyTypes` (lista), `insurancePolicyId`, `minAmount`, `maxAmount`
- `GET /reports/policy-payments/download` → `downloadPolicyPaymentReport()`
  - Params: los mismos + `format` (EXCEL/PDF)

### Exportadores

#### PolicyPaymentReportExcelExporter
4 hojas:
1. **Resumen por Tipo**: Tipo de Póliza | Cant. Pagos | Total Pagado | Premio Esperado | Diferencia
2. **Resumen por Póliza**: Agrupado por tipo (header coloreado), Póliza | Estado | Frecuencia | Premio Mensual | Cant. Pagos | Total Pagado | Esperado | Diferencia, con subtotales
3. **Detalle de Pagos**: Tipo | Póliza | Fecha Pago | Período Desde | Período Hasta | Monto | Premio Esperado | Diferencia | Notas, con subtotales
4. **Vehículos Asegurados** (solo si hay pólizas AUTOMOTOR): Póliza | Patente | Marca/Modelo | Área | Suma Asegurada | Premio Mensual

#### PolicyPaymentReportPdfExporter
4 secciones (con page break):
1. Resumen por Tipo de Póliza
2. Resumen por Póliza
3. Detalle de Pagos
4. Vehículos Asegurados (solo AUTOMOTOR)

---

## Frontend

### Modelo (`shared/models/policy-payment-report.model.ts`)

```typescript
interface PolicyPaymentReportFilters {
    startDate: string | null;
    endDate: string | null;
    policyTypes: string[];
    insurancePolicyId: number | null;
    minAmount: number | null;
    maxAmount: number | null;
}

interface PolicyPaymentReportVehicle { ... }
interface PolicyPaymentReportPayment { ... }
interface PolicyPaymentReportPolicyGroup { ... }
interface PolicyPaymentReportTypeGroup { ... }
interface PolicyPaymentReportResponse { ... }
```

### Servicio (`domains/reports/policy-payment-report/services/policy-payment-report.service.ts`)

- `getPolicyPaymentReport(filters)` → GET `/reports/policy-payments`
- `downloadPolicyPaymentReport(format, filters)` → GET `/reports/policy-payments/download` con `responseType: 'blob'`

### Componente (`domains/reports/policy-payment-report/`)

**Prefix SCSS:** `ppr-` (policy-payment-report)

**Sidebar — Filtros:**
| Filtro | Tipo | Comportamiento |
|--------|------|----------------|
| Período | Dos inputs date (Desde/Hasta) | Default: último mes |
| Tipo de Póliza | Chips seleccionables | Valores del enum PolicyType (actualmente solo AUTOMOTOR). Vacío = todos |
| Rango de Montos | Dos inputs number (Mín/Máx) | Filtra por monto de pago |

**Sidebar — Agrupación:**
| Toggle | Default |
|--------|---------|
| Tipo de Póliza | ✅ ON |
| Póliza | ✅ ON |

**KPI Cards:**
| KPI | Valor | Nota |
|-----|-------|------|
| Total Pagado | `totalPaidAmount` formateado como moneda | Suma real de pagos |
| Premio Esperado | `totalExpectedAmount` formateado como moneda | Suma de premios mensuales |
| Diferencia | `totalDifference` formateado como moneda | Con color: verde si ≥ 0, rojo si < 0 |
| Cant. Pagos | `totalPaymentCount` | |

**Tabla de pagos — Columnas:**
| Columna | Visible siempre | Ancho |
|---------|----------------|-------|
| Fecha | ✅ | 85px |
| Tipo | Solo si `!groupByType` | 110px |
| Póliza | Solo si `!groupByPolicy` | 130px |
| Período | ✅ | 160px (desde - hasta) |
| Monto | ✅ | 110px |
| Premio Esp. | ✅ | 110px |
| Diferencia | ✅ | 110px (con color) |
| Notas | ✅ | flexible |

**Diferencia con color:**
- `difference > 0` → texto rojo (pagó más de lo esperado = sobrecosto)
- `difference < 0` → texto naranja/amber (pagó menos = ¿deuda?)
- `difference === 0` → texto verde (coincide)

**Sección de vehículos asegurados:**
Dentro de cada grupo de póliza (cuando está expandida y es AUTOMOTOR), debajo de la tabla de pagos se muestra un mini-panel colapsable "Vehículos asegurados (N)" con una tabla simple:
| Patente | Marca/Modelo | Área | Suma Asegurada | Premio Mensual |

**Sorting:** Por fecha (desc default), monto, diferencia

**4 modos de visualización:**
1. Tipo + Póliza → jerarquía completa
2. Solo Tipo → pagos agrupados por tipo de póliza
3. Solo Póliza → pólizas sin agrupar por tipo (merged)
4. Flat → tabla plana con columnas dinámicas

### Ruta y Navegación
- **Route**: `/informes/polizas`
- **Tab label**: `Pólizas`
- **Permission**: `REPORT_FINANCIAL`
- En `reports.routes.ts`: lazy load del componente `PolicyPaymentReport`
- En `navigation.config.ts`: nuevo item en la sección Informes

---

## Archivos a crear/modificar

### Backend (crear)
- `model/dto/report/policyPayment/PolicyPaymentReportFilterDTO.java`
- `model/dto/report/policyPayment/PolicyPaymentReportDTO.java`
- `model/dto/report/policyPayment/PolicyPaymentReportTypeGroupDTO.java`
- `model/dto/report/policyPayment/PolicyPaymentReportPolicyGroupDTO.java`
- `model/dto/report/policyPayment/PolicyPaymentReportPaymentDTO.java`
- `model/dto/report/policyPayment/PolicyPaymentReportVehicleDTO.java`
- `service/export/PolicyPaymentReportExcelExporter.java`
- `service/export/PolicyPaymentReportPdfExporter.java`

### Backend (modificar)
- `service/port/IReportService.java` — agregar 2 métodos
- `service/implementation/ReportService.java` — implementar generación + helpers
- `controller/ReportController.java` — agregar 2 endpoints

### Frontend (crear)
- `shared/models/policy-payment-report.model.ts`
- `domains/reports/policy-payment-report/services/policy-payment-report.service.ts`
- `domains/reports/policy-payment-report/policy-payment-report.ts`
- `domains/reports/policy-payment-report/policy-payment-report.html`
- `domains/reports/policy-payment-report/policy-payment-report.scss`

### Frontend (modificar)
- `shared/models/index.ts` — agregar export
- `domains/reports/reports.routes.ts` — agregar ruta
- `core/config/navigation.config.ts` — agregar tab

---

## Decisiones de diseño

1. **Diferencia = pagado - esperado** (no al revés): un valor positivo significa sobrecosto para la empresa, que es lo más intuitivo para un reporte financiero de egresos.

2. **esperado por póliza = premioMensual × cantidadPagos**: se usa la cantidad de pagos realizados como multiplicador (no los meses calendario del período), porque una póliza podría no tener pagos todos los meses si la frecuencia es diferente. Esto simplifica el cálculo y es más preciso.

3. **Vehículos asegurados como información de contexto**: no se los agrupa como dimensión principal, sino que se muestran como detalle complementario dentro de cada póliza, ya que el foco del reporte es el pago, no la cobertura.

4. **Filtro de tipo de póliza visible siempre**: aunque solo existe AUTOMOTOR, se muestra para que el usuario sepa que el sistema contempla la distinción y para que funcione automáticamente cuando se agreguen nuevos tipos.
