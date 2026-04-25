# Feature 17 — Previsiones de Gastos (Budget Forecasts)

## Resumen

Sistema de **previsión de gastos por período** (típicamente semanal, ej. 18/04 al 24/04). Cada previsión es una **planilla** de ítems esperados, donde cada ítem puede:
- Estar asociado a una **categoría con relación directa** (Salarios, Servicios, Reparaciones, Compras de Stock, Patentes), o
- Ser un **gasto libre** (texto + monto, sin relación a entidades del sistema).

La previsión funciona como una **foto separada** de los registros reales: cargar una previsión NO crea ningún `SalaryPayment`, `ServicePayment`, etc. Para materializarla, el usuario presiona **"Aplicar gasto"** sobre cada ítem (o batch sobre todos los pendientes), lo que dispara la creación del registro real en la entidad correspondiente y deja el ítem trazado al ID generado (con opción de revertir).

Soporta **importación / exportación / plantilla Excel** siguiendo el mismo patrón que la feature de Asistencia (F9), y un sistema adicional de **Plantillas Frecuentes** (`BudgetForecastTemplate`) para acelerar carga de previsiones recurrentes.

> **Nota de dependencias:** Esta feature consume entidades existentes (`SalaryPayment`, `ServicePayment`, `Repair`, `StockPurchase`, `LicencePlatePayment`) y se beneficia de F15 Tesorería (al aplicar un gasto, el pago generado puede asociarse a una `CashBox` o `BankAccount`). No es estrictamente bloqueante con F15: si F15 no está implementada, los pagos se aplican sin asociación de tesorería.

---

## 1. Modelo de Datos

### 1.1 Entidad raíz: `BudgetForecast` (Previsión)

**Ubicación:** `model/entity/forecast/BudgetForecast.java`

```java
@Entity
@Table(name = "budget_forecasts")
public class BudgetForecast extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;                                // "Previsión Semana 17 — Abril 2026"

    @Column(length = 1000)
    private String description;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BudgetForecastStatus status = BudgetForecastStatus.BORRADOR;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;   // cacheado, recalculado al modificar items

    @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal appliedAmount = BigDecimal.ZERO; // suma de items con applicationStatus == APLICADO

    @Column(name = "created_from_template_id")
    private Long createdFromTemplateId;                 // FK informativa a BudgetForecastTemplate (opcional)

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    @Column(name = "confirmed_by_user_id")
    private Long confirmedByUserId;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @OneToMany(mappedBy = "budgetForecast", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetForecastItem> items = new ArrayList<>();
}
```

**Reglas:**
- `periodFrom <= periodTo`. Sin restricción de longitud (puede ser un día, una semana, un mes).
- Sin restricción de superposición entre previsiones (el usuario puede tener varias previsiones que cubran el mismo rango si tiene sentido, ej. "Previsión Salarios" + "Previsión Operativa").
- `totalAmount` se recalcula tras cualquier add/update/delete de items.
- Soft delete (`deleted = true`) — solo permitido en estado `BORRADOR` o `CERRADA`.

### 1.2 Enum: `BudgetForecastStatus`

```java
public enum BudgetForecastStatus {
    BORRADOR,     // Editable. Items pueden agregarse/eliminarse/modificarse. NO se puede aplicar gasto.
    CONFIRMADA,   // Solo lectura sobre items (ni agregar, ni borrar, ni cambiar monto). SÍ se puede aplicar gasto.
    CERRADA       // Período terminado, todos los items aplicados o decididos. Solo lectura total. Permite des-aplicar (con permiso).
}
```

**Transiciones permitidas:**
- `BORRADOR → CONFIRMADA` — manual (botón "Confirmar previsión"). Requiere al menos 1 item.
- `CONFIRMADA → BORRADOR` — manual con permiso `BUDGET_FORECAST_REOPEN`. Solo si **NO hay items aplicados** (sino habría que revertir todos primero).
- `CONFIRMADA → CERRADA` — manual (botón "Cerrar previsión") O automático cuando todos los items están en estado terminal (`APLICADO` / `OMITIDO`).
- `CERRADA → CONFIRMADA` — manual con permiso `BUDGET_FORECAST_REOPEN`.

### 1.3 Entidad hija: `BudgetForecastItem`

**Ubicación:** `model/entity/forecast/BudgetForecastItem.java`

```java
@Entity
@Table(name = "budget_forecast_items")
public class BudgetForecastItem extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_forecast_id", nullable = false)
    private BudgetForecast budgetForecast;

    @Column(name = "row_order", nullable = false)
    private Integer rowOrder;                           // orden visual (drag & drop futuro)

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private BudgetForecastItemType itemType;

    @Column(nullable = false, length = 500)
    private String description;                         // descripción humana — siempre requerida

    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;                     // dentro del rango [periodFrom, periodTo] (validado, salvo override admin)

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    // ── Referencias opcionales según itemType (para autocompletar al aplicar) ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;                          // requerido si itemType == SALARIO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;                          // requerido si itemType == SERVICIO/REPARACION/COMPRA_STOCK/PATENTE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id")
    private ServiceType serviceType;                    // requerido si itemType == SERVICIO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;                            // requerido si itemType == REPARACION/PATENTE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;                                // requerido si itemType == COMPRA_STOCK

    @Column(precision = 19, scale = 4)
    private BigDecimal stockQuantity;                   // requerido si itemType == COMPRA_STOCK

    // ── Estado de aplicación ──
    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 30)
    @Builder.Default
    private BudgetForecastItemApplicationStatus applicationStatus = BudgetForecastItemApplicationStatus.PENDIENTE;

    @Column(name = "applied_entity_type", length = 30)
    private String appliedEntityType;                   // "SALARY_PAYMENT" | "SERVICE_PAYMENT" | etc.

    @Column(name = "applied_entity_id")
    private Long appliedEntityId;                       // ID del registro real generado al aplicar

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "applied_by_user_id")
    private Long appliedByUserId;

    @Column(name = "skip_reason", length = 500)
    private String skipReason;                          // razón si applicationStatus == OMITIDO
}
```

### 1.4 Enum: `BudgetForecastItemType`

```java
public enum BudgetForecastItemType {
    SALARIO,           // → genera SalaryPayment
    SERVICIO,          // → genera ServicePayment
    REPARACION,        // → genera Repair
    COMPRA_STOCK,      // → genera StockPurchase
    PATENTE,           // → genera LicencePlatePayment (tratado como pago de servicio en flujo)
    OTRO               // gasto libre — NO se puede aplicar (solo informativo)
}
```

**Tabla de campos requeridos por tipo:**

| `itemType`     | description | expectedDate | expectedAmount | employee | supplier | serviceType | vehicle | stock | stockQuantity |
|----------------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| SALARIO        | ✓ | ✓ | ✓ | ✓ |   |   |   |   |   |
| SERVICIO       | ✓ | ✓ | ✓ |   | ✓ | ✓ |   |   |   |
| REPARACION     | ✓ | ✓ | ✓ |   | ✓ |   | ✓ |   |   |
| COMPRA_STOCK   | ✓ | ✓ | ✓ |   | ✓ |   |   | ✓ | ✓ |
| PATENTE        | ✓ | ✓ | ✓ |   | ✓ |   | ✓ |   |   |
| OTRO           | ✓ | ✓ | ✓ |   |   |   |   |   |   |

Validación implementada en `BudgetForecastItemValidator` invocado desde el service.

### 1.5 Enum: `BudgetForecastItemApplicationStatus`

```java
public enum BudgetForecastItemApplicationStatus {
    PENDIENTE,         // Default — aún no se aplicó
    APLICADO,          // Ya generó su registro real (appliedEntityId != null)
    OMITIDO            // Decidido no aplicar (con motivo en skipReason)
}
```

> El estado `OTRO` siempre queda en `PENDIENTE` lógicamente — no hay nada que aplicar. Visualmente se muestra como "Solo informativo".

### 1.6 Entidad: `BudgetForecastTemplate` (Plantilla Frecuente)

**Ubicación:** `model/entity/forecast/BudgetForecastTemplate.java`

```java
@Entity
@Table(name = "budget_forecast_templates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
public class BudgetForecastTemplate extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;                                // "Plantilla Semanal Operativa"

    @Column(length = 1000)
    private String description;

    @Column(name = "default_period_days", nullable = false)
    @Builder.Default
    private Integer defaultPeriodDays = 7;              // se usa como sugerencia al crear desde plantilla

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetForecastTemplateItem> items = new ArrayList<>();
}
```

### 1.7 Entidad hija: `BudgetForecastTemplateItem`

Espejo simplificado de `BudgetForecastItem`, sin estado de aplicación ni `expectedDate` absoluta:

```java
@Entity
@Table(name = "budget_forecast_template_items")
public class BudgetForecastTemplateItem extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private BudgetForecastTemplate template;

    @Column(name = "row_order", nullable = false)
    private Integer rowOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private BudgetForecastItemType itemType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "day_offset", nullable = false)
    private Integer dayOffset;                          // días desde periodFrom para calcular expectedDate al instanciar

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;                  // monto sugerido (editable al instanciar)

    // Mismas FKs opcionales que BudgetForecastItem (employee, supplier, serviceType, vehicle, stock)
    // ... omitidas por brevedad
    @Column(precision = 19, scale = 4)
    private BigDecimal stockQuantity;
}
```

**Uso:** Al crear una previsión "desde plantilla":
1. Usuario elige plantilla y `periodFrom` (default: hoy).
2. Backend calcula `periodTo = periodFrom + template.defaultPeriodDays - 1`.
3. Backend clona cada `BudgetForecastTemplateItem` → `BudgetForecastItem`:
   - `expectedDate = periodFrom + dayOffset`.
   - Resto de campos copiados tal cual.
4. La nueva previsión queda en `BORRADOR` para que el usuario la edite antes de confirmar.

---

## 2. Migración SQL

**Archivo:** `db/changelog/db.changelog-{nro}.xml`

```sql
CREATE TABLE budget_forecasts (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                BIGINT       NOT NULL,
    name                     VARCHAR(200) NOT NULL,
    description              VARCHAR(1000),
    period_from              DATE         NOT NULL,
    period_to                DATE         NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    total_amount             DECIMAL(19,2) NOT NULL DEFAULT 0,
    applied_amount           DECIMAL(19,2) NOT NULL DEFAULT 0,
    created_from_template_id BIGINT,
    confirmed_at             DATETIME,
    confirmed_by_user_id     BIGINT,
    closed_at                DATETIME,
    closed_by_user_id        BIGINT,
    deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               DATETIME     NOT NULL,
    updated_at               DATETIME     NOT NULL
);
CREATE INDEX idx_budget_forecasts_tenant_period ON budget_forecasts(tenant_id, period_from, period_to);
CREATE INDEX idx_budget_forecasts_status        ON budget_forecasts(tenant_id, status);

CREATE TABLE budget_forecast_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    budget_forecast_id  BIGINT       NOT NULL REFERENCES budget_forecasts(id) ON DELETE CASCADE,
    row_order           INT          NOT NULL,
    item_type           VARCHAR(30)  NOT NULL,
    description         VARCHAR(500) NOT NULL,
    expected_date       DATE         NOT NULL,
    expected_amount     DECIMAL(19,2) NOT NULL,
    employee_id         BIGINT       REFERENCES employees(id),
    supplier_id         BIGINT       REFERENCES suppliers(id),
    service_type_id     BIGINT       REFERENCES service_types(id),
    vehicle_id          BIGINT       REFERENCES vehicles(id),
    stock_id            BIGINT       REFERENCES stocks(id),
    stock_quantity      DECIMAL(19,4),
    application_status  VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    applied_entity_type VARCHAR(30),
    applied_entity_id   BIGINT,
    applied_at          DATETIME,
    applied_by_user_id  BIGINT,
    skip_reason         VARCHAR(500),
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL
);
CREATE INDEX idx_budget_forecast_items_forecast ON budget_forecast_items(budget_forecast_id, row_order);
CREATE INDEX idx_budget_forecast_items_status   ON budget_forecast_items(application_status);

CREATE TABLE budget_forecast_templates (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    name                 VARCHAR(200) NOT NULL,
    description          VARCHAR(1000),
    default_period_days  INT          NOT NULL DEFAULT 7,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    UNIQUE (tenant_id, name)
);

CREATE TABLE budget_forecast_template_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    template_id     BIGINT       NOT NULL REFERENCES budget_forecast_templates(id) ON DELETE CASCADE,
    row_order       INT          NOT NULL,
    item_type       VARCHAR(30)  NOT NULL,
    description     VARCHAR(500) NOT NULL,
    day_offset      INT          NOT NULL,
    expected_amount DECIMAL(19,2) NOT NULL,
    employee_id     BIGINT       REFERENCES employees(id),
    supplier_id     BIGINT       REFERENCES suppliers(id),
    service_type_id BIGINT       REFERENCES service_types(id),
    vehicle_id      BIGINT       REFERENCES vehicles(id),
    stock_id        BIGINT       REFERENCES stocks(id),
    stock_quantity  DECIMAL(19,4),
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL
);
```

---

## 3. DTOs

### 3.1 `BudgetForecastDTO` (request — create / update header)

```java
public record BudgetForecastDTO(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 1000) String description,
    @NotNull LocalDate periodFrom,
    @NotNull LocalDate periodTo
) {}
```

### 3.2 `BudgetForecastItemDTO` (request — create / update item)

```java
public record BudgetForecastItemDTO(
    @NotNull BudgetForecastItemType itemType,
    @NotBlank @Size(max = 500) String description,
    @NotNull LocalDate expectedDate,
    @NotNull @DecimalMin("0.01") BigDecimal expectedAmount,
    Integer rowOrder,
    Long employeeId,
    Long supplierId,
    Long serviceTypeId,
    Long vehicleId,
    Long stockId,
    BigDecimal stockQuantity
) {}
```

### 3.3 `BudgetForecastResponseDTO`

```java
public record BudgetForecastResponseDTO(
    Long id,
    String name,
    String description,
    LocalDate periodFrom,
    LocalDate periodTo,
    BudgetForecastStatus status,
    BigDecimal totalAmount,
    BigDecimal appliedAmount,
    BigDecimal pendingAmount,                          // totalAmount - appliedAmount (excluyendo OMITIDO)
    int itemCount,
    int appliedItemCount,
    int pendingItemCount,
    int skippedItemCount,
    Long createdFromTemplateId,
    String createdFromTemplateName,
    LocalDateTime confirmedAt,
    LocalDateTime closedAt,
    List<BudgetForecastItemResponseDTO> items
) {}
```

### 3.4 `BudgetForecastItemResponseDTO`

```java
public record BudgetForecastItemResponseDTO(
    Long id,
    Integer rowOrder,
    BudgetForecastItemType itemType,
    String description,
    LocalDate expectedDate,
    BigDecimal expectedAmount,

    Long employeeId,    String employeeFullName,
    Long supplierId,    String supplierName,
    Long serviceTypeId, String serviceTypeName,
    Long vehicleId,     String vehicleDescription,
    Long stockId,       String stockName,
    BigDecimal stockQuantity,

    BudgetForecastItemApplicationStatus applicationStatus,
    String appliedEntityType,
    Long appliedEntityId,
    String appliedEntityReference,                     // "Pago de Salario #123" / "Servicio #45" — descriptivo
    LocalDateTime appliedAt,
    String skipReason,
    Boolean canApply,                                  // calculado: status==CONFIRMADA && applicationStatus==PENDIENTE && itemType != OTRO
    Boolean canRevert                                  // calculado: applicationStatus==APLICADO && permiso del usuario
) {}
```

### 3.5 DTOs de plantilla

`BudgetForecastTemplateDTO`, `BudgetForecastTemplateItemDTO`, `BudgetForecastTemplateResponseDTO` — análogos a los anteriores, sin `expectedDate`/`applicationStatus` y reemplazando con `dayOffset`.

### 3.6 DTOs de operaciones especiales

```java
public record CreateForecastFromTemplateRequest(
    @NotNull Long templateId,
    @NotNull LocalDate periodFrom,
    String name                                        // opcional, default: template.name + " — " + periodFrom
) {}

public record ApplyItemRequest(
    Long itemId                                        // path param
    // Body opcional con overrides: si el usuario quiere ajustar el monto al aplicar
    // BigDecimal overrideAmount, LocalDate overrideDate, ...
) {}

public record SkipItemRequest(
    @NotBlank @Size(max = 500) String reason
) {}

public record BulkApplyRequest(
    Long forecastId,                                   // path param
    List<Long> itemIds                                 // null o vacío = todos los pendientes
) {}

public record BulkApplyResponse(
    int totalRequested,
    int successCount,
    int failureCount,
    List<ApplyItemResult> results
) {}

public record ApplyItemResult(
    Long itemId,
    boolean success,
    String appliedEntityType,
    Long appliedEntityId,
    String errorMessage
) {}
```

---

## 4. Endpoints REST

### 4.1 Controller: `BudgetForecastController` (`/api/v1/budget-forecasts`)

| Método | Path | Permiso | Descripción |
|---|---|---|---|
| `GET`    | `/`                          | `BUDGET_FORECAST_VIEW`        | Listado paginado + filtros |
| `GET`    | `/{id}`                      | `BUDGET_FORECAST_VIEW`        | Detalle con todos los items |
| `POST`   | `/`                          | `BUDGET_FORECAST_CREATE`      | Crear previsión vacía |
| `PUT`    | `/{id}`                      | `BUDGET_FORECAST_UPDATE`      | Actualizar header (solo si BORRADOR) |
| `DELETE` | `/{id}`                      | `BUDGET_FORECAST_DELETE`      | Soft delete |
| `POST`   | `/{id}/items`                | `BUDGET_FORECAST_UPDATE`      | Agregar item (solo BORRADOR) |
| `PUT`    | `/{id}/items/{itemId}`       | `BUDGET_FORECAST_UPDATE`      | Editar item (solo BORRADOR) |
| `DELETE` | `/{id}/items/{itemId}`       | `BUDGET_FORECAST_UPDATE`      | Eliminar item (solo BORRADOR) |
| `POST`   | `/{id}/confirm`              | `BUDGET_FORECAST_CONFIRM`     | Transición BORRADOR → CONFIRMADA |
| `POST`   | `/{id}/reopen`               | `BUDGET_FORECAST_REOPEN`      | CONFIRMADA → BORRADOR (solo si no hay items aplicados) |
| `POST`   | `/{id}/close`                | `BUDGET_FORECAST_CONFIRM`     | CONFIRMADA → CERRADA |
| `POST`   | `/{id}/items/{itemId}/apply` | `BUDGET_FORECAST_APPLY`       | Aplicar gasto: genera registro real |
| `POST`   | `/{id}/items/{itemId}/revert`| `BUDGET_FORECAST_APPLY`       | Revertir aplicación: borra registro generado |
| `POST`   | `/{id}/items/{itemId}/skip`  | `BUDGET_FORECAST_UPDATE`      | Marcar item como OMITIDO con razón |
| `POST`   | `/{id}/items/bulk-apply`     | `BUDGET_FORECAST_APPLY`       | Aplicar batch (todos o selección) |
| `POST`   | `/from-template`             | `BUDGET_FORECAST_CREATE`      | Crear desde `BudgetForecastTemplate` |
| `POST`   | `/{id}/duplicate`            | `BUDGET_FORECAST_CREATE`      | Duplicar previsión existente como nuevo BORRADOR |
| `GET`    | `/{id}/download`             | `BUDGET_FORECAST_VIEW`        | Descargar Excel/PDF (`?format=xlsx\|pdf`) |
| `POST`   | `/import`                    | `BUDGET_FORECAST_CREATE`      | Importar desde Excel (multipart) |
| `GET`    | `/import/template`           | `BUDGET_FORECAST_VIEW`        | Descargar plantilla Excel vacía |

**Filtros del listado:**
```java
public record BudgetForecastFilterDTO(
    LocalDate periodFromGte,
    LocalDate periodToLte,
    List<BudgetForecastStatus> statuses,
    String search,                                     // por name/description
    Boolean hasAppliedItems,
    int page, int size, String sortBy, Sort.Direction sortDir
) {}
```

### 4.2 Controller: `BudgetForecastTemplateController` (`/api/v1/budget-forecast-templates`)

CRUD estándar (`GET`, `POST`, `PUT`, `DELETE`, `GET /{id}`) más `GET /{id}/items` y endpoints de items individuales. Permisos `BUDGET_FORECAST_TEMPLATE_*`.

---

## 5. Service: lógica de "Aplicar Gasto" (núcleo)

### 5.1 Patrón Strategy: `BudgetForecastItemApplier`

```java
public interface BudgetForecastItemApplier {
    BudgetForecastItemType supportedType();
    AppliedEntityRef apply(BudgetForecastItem item, ApplyItemRequest request);
    void revert(BudgetForecastItem item);
}

public record AppliedEntityRef(String entityType, Long entityId, String reference) {}
```

**Implementaciones (una por `BudgetForecastItemType` aplicable):**

| Implementación                          | Tipo soportado | Acción                                                                                       |
|-----------------------------------------|---------------|----------------------------------------------------------------------------------------------|
| `SalaryPaymentApplier`                  | `SALARIO`     | Crea `SalaryPayment` con employee, amount, date.                                            |
| `ServicePaymentApplier`                 | `SERVICIO`    | Crea `ServicePayment` con supplier, serviceType, amount, date.                              |
| `RepairApplier`                         | `REPARACION`  | Crea `Repair` con vehicle, supplier, amount, date.                                          |
| `StockPurchaseApplier`                  | `COMPRA_STOCK`| Crea `StockPurchase` con stock, supplier, quantity, unitPrice (= amount/quantity), date.    |
| `LicencePlatePaymentApplier`            | `PATENTE`     | Crea `LicencePlatePayment` con vehicle, supplier, amount, date — usa el mismo flujo del formulario de pago de servicio. |

> El tipo `OTRO` no tiene applier — `BudgetForecastService.apply()` lanza `BudgetForecastApplyException` si se intenta aplicar.

### 5.2 Flujo de `apply(forecastId, itemId)`

```text
1. Validar: forecast.status == CONFIRMADA
2. Validar: item.applicationStatus == PENDIENTE
3. Validar: item.itemType != OTRO
4. Resolver applier por item.itemType (Map<Type, Applier> inyectado por Spring)
5. Llamar applier.apply(item, request) → retorna AppliedEntityRef
6. Persistir cambios en item:
       applicationStatus = APLICADO
       appliedEntityType = ref.entityType()
       appliedEntityId   = ref.entityId()
       appliedAt         = now()
       appliedByUserId   = currentUser.id
7. Recalcular forecast.appliedAmount
8. Si todos los items terminaron en APLICADO/OMITIDO → auto-transición a CERRADA
9. Retornar BudgetForecastItemResponseDTO actualizado
```

### 5.3 Flujo de `revert(forecastId, itemId)`

```text
1. Validar: item.applicationStatus == APLICADO
2. Validar: forecast.status IN (CONFIRMADA, CERRADA)
3. Resolver applier
4. Llamar applier.revert(item) → borra el registro de la entidad real (hard delete del SalaryPayment/ServicePayment/etc.)
   Si la entidad real fue modificada por el usuario después de aplicar (ej. monto cambió),
     mostrar warning y requerir confirmación explícita (?force=true).
5. Limpiar item: applicationStatus = PENDIENTE, appliedEntityType/Id/At/By = null
6. Recalcular forecast.appliedAmount
7. Si forecast estaba CERRADA por auto-transición → volver a CONFIRMADA automáticamente
```

**Decisión:** Revert hace hard delete del registro generado (no soft) porque desde la perspectiva del usuario "deshacer aplicar" debe ser limpio. Para evitar pérdida de datos accidental, el frontend muestra modal de confirmación con detalle de lo que se borrará.

### 5.4 Bulk apply

```text
1. Resolver lista de items (forecastId + ids opcional, o todos los PENDIENTE no-OTRO)
2. Por cada item: try apply() — captar excepción individual
3. Construir BulkApplyResponse con success/failure por item
4. NO hacer rollback de los exitosos si alguno falla — el usuario ve el detalle y decide
```

---

## 6. Importación / Exportación / Plantilla Excel

Se sigue el mismo patrón que **Feature 9 (Asistencia)**.

### 6.1 Formato de la planilla Excel

**Hoja: "Items de Previsión"**

| Columna | Header                    | Tipo / Validación                                                          | Obligatorio                |
|--------:|---------------------------|----------------------------------------------------------------------------|----------------------------|
| A       | Tipo                      | Texto: SALARIO / SERVICIO / REPARACION / COMPRA_STOCK / PATENTE / OTRO     | Sí                         |
| B       | Descripción               | Texto (max 500)                                                            | Sí                         |
| C       | Fecha Esperada            | Fecha (`YYYY-MM-DD`) dentro del rango de la previsión                      | Sí                         |
| D       | Monto Esperado            | Numérico decimal > 0                                                       | Sí                         |
| E       | Empleado (CUIL)           | CUIL del empleado                                                          | Si Tipo=SALARIO            |
| F       | Proveedor (CUIT)          | CUIT del proveedor                                                         | Si Tipo ∈ {SERVICIO, REPARACION, COMPRA_STOCK, PATENTE} |
| G       | Tipo de Servicio (Nombre) | Nombre exacto del ServiceType                                              | Si Tipo=SERVICIO           |
| H       | Vehículo (Patente)        | Patente del vehículo                                                       | Si Tipo ∈ {REPARACION, PATENTE} |
| I       | Stock (Código)            | Código del stock                                                           | Si Tipo=COMPRA_STOCK       |
| J       | Cantidad                  | Numérico decimal > 0                                                       | Si Tipo=COMPRA_STOCK       |

**Hoja: "Cabecera"** (solo en exportación e importación que crea previsión nueva):

| Campo        | Valor                       |
|--------------|-----------------------------|
| Nombre       | (texto)                     |
| Descripción  | (texto)                     |
| Período Desde| (fecha)                     |
| Período Hasta| (fecha)                     |

**Hoja: "Instrucciones"** — texto explicativo, valores válidos del enum, ejemplos.

### 6.2 Endpoints de import/export

- **`GET /import/template`** — Descarga plantilla vacía (header sin valores, hoja Items con 1 fila de ejemplo por tipo, hoja Instrucciones).
- **`GET /{id}/download?format=xlsx`** — Exporta una previsión existente con todos los items (formato compatible con import).
- **`GET /{id}/download?format=pdf`** — Exporta como PDF (estilo similar a los reportes F12*: cabecera azul `DeviceRgb(41,128,185)`, totales naranja `DeviceRgb(243,156,18)`, columna Estado con badge coloreado).
- **`POST /import`** — Multipart con `file` y `mode`:
  - `mode=NEW` → crea nueva previsión usando hoja "Cabecera" + items.
  - `mode=APPEND_TO=<forecastId>` → agrega items a previsión existente (debe estar BORRADOR).

### 6.3 Servicio: `BudgetForecastExcelService`

- `generateTemplate()` — Plantilla vacía con ejemplos.
- `exportToExcel(forecastId)` — Exporta previsión existente.
- `importFromExcel(MultipartFile, ImportMode, Long targetForecastId)` — Parsea, valida exhaustivamente (mismo patrón que `AttendanceImportValidator`), retorna `BudgetForecastImportResultDTO` con éxito/fallos por fila.

### 6.4 Servicio: `BudgetForecastPdfService`

Sigue patrón `MonetaryReportPdfService` de F12. Estructura:
1. Cabecera con logo + nombre tenant + título "Previsión: {name}" + período.
2. Tabla de items agrupados por `itemType` (subtotal por tipo, totales finales).
3. Footer con totales: previsto / aplicado / pendiente / omitido.

### 6.5 Validaciones de importación (resumen)

- Tipo válido (en enum).
- Campos requeridos según tipo (tabla de §1.4).
- CUIL de empleado, CUIT de proveedor, patente de vehículo, código de stock → existen y no eliminados (en el tenant).
- ServiceType existe.
- `expectedDate` dentro del rango `[periodFrom, periodTo]` de la previsión.
- Montos > 0.

Ante cualquier error: NO se persiste nada. Se devuelve detalle por fila con número de fila + columna + mensaje.

---

## 7. Permisos

```java
public class AppPermissions {
    // ... existentes
    public static final String BUDGET_FORECAST_VIEW            = "BUDGET_FORECAST_VIEW";
    public static final String BUDGET_FORECAST_CREATE          = "BUDGET_FORECAST_CREATE";
    public static final String BUDGET_FORECAST_UPDATE          = "BUDGET_FORECAST_UPDATE";
    public static final String BUDGET_FORECAST_DELETE          = "BUDGET_FORECAST_DELETE";
    public static final String BUDGET_FORECAST_CONFIRM         = "BUDGET_FORECAST_CONFIRM";   // confirmar y cerrar
    public static final String BUDGET_FORECAST_REOPEN          = "BUDGET_FORECAST_REOPEN";    // re-abrir confirmadas/cerradas
    public static final String BUDGET_FORECAST_APPLY           = "BUDGET_FORECAST_APPLY";     // aplicar y revertir gasto

    public static final String BUDGET_FORECAST_TEMPLATE_VIEW   = "BUDGET_FORECAST_TEMPLATE_VIEW";
    public static final String BUDGET_FORECAST_TEMPLATE_CREATE = "BUDGET_FORECAST_TEMPLATE_CREATE";
    public static final String BUDGET_FORECAST_TEMPLATE_UPDATE = "BUDGET_FORECAST_TEMPLATE_UPDATE";
    public static final String BUDGET_FORECAST_TEMPLATE_DELETE = "BUDGET_FORECAST_TEMPLATE_DELETE";
}
```

---

## 8. Frontend

### 8.1 Estructura

**Módulo:** `domains/budget-forecasts/` (rotulado **"Previsiones"** en navegación).

```
domains/budget-forecasts/
├── budget-forecasts.routes.ts
├── forecast-list/                    — listado paginado con filtros
│   ├── forecast-list.ts
│   └── forecast-list.html|.scss
├── forecast-detail/                  — detalle tipo planilla (núcleo de la feature)
│   ├── forecast-detail.ts
│   ├── forecast-items-grid/          — grid editable estilo Excel
│   ├── forecast-toolbar/             — botones: Confirmar/Cerrar/Aplicar todo/Importar/Descargar/Duplicar
│   └── forecast-status-badge/
├── forecast-form-modal/              — crear/editar header
├── forecast-item-form-modal/         — agregar/editar item con campos condicionales según tipo
├── forecast-from-template-modal/     — wizard "Crear desde plantilla"
├── forecast-import-modal/            — drag&drop Excel + descarga plantilla (idéntico a F9)
├── apply-item-modal/                 — confirmación de aplicación con preview del registro a generar
├── revert-item-modal/                — confirmación de reversión
└── templates/
    ├── template-list/
    ├── template-detail/
    └── template-form-modal/
```

Prefijo SCSS BEM: `bf-` (budget-forecast).

### 8.2 Navegación

En `core/config/navigation.config.ts`:

```typescript
{
  id: 'previsiones',
  label: 'Previsiones',
  icon: 'event_note',                 // o 'request_quote'
  permissions: ['BUDGET_FORECAST_VIEW'],
  children: [
    { id: 'previsiones-list',     label: 'Previsiones',  route: '/previsiones' },
    { id: 'previsiones-templates',label: 'Plantillas',   route: '/previsiones/plantillas',
      permissions: ['BUDGET_FORECAST_TEMPLATE_VIEW'] },
  ],
}
```

### 8.3 UX clave: vista de detalle como planilla

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ ← Previsión Semana 17 — Abril 2026          [Estado: BORRADOR] [⚙ Acciones] │
│ Período: 18/04/2026 → 24/04/2026                                             │
│ Total previsto: $1.245.000  |  Aplicado: $0  |  Pendiente: $1.245.000       │
│                                                                              │
│ [+ Agregar fila]  [Importar Excel]  [Descargar ▼]  [Duplicar]  [Confirmar]  │
├──────────────────────────────────────────────────────────────────────────────┤
│ # │ Tipo       │ Descripción         │ Fecha    │ Monto    │ Estado │ Acc.  │
│ 1 │ SALARIO    │ Juan Pérez          │ 19/04    │ $180.000 │ ⚪ Pend │ ⋮    │
│ 2 │ SALARIO    │ María Gómez         │ 19/04    │ $175.000 │ ⚪ Pend │ ⋮    │
│ 3 │ SERVICIO   │ Edenor — Luz oficina│ 22/04    │ $45.000  │ ⚪ Pend │ ⋮    │
│ 4 │ REPARACION │ Camión 123 — embrag.│ 23/04    │ $250.000 │ ⚪ Pend │ ⋮    │
│ 5 │ OTRO       │ Refrigerios obra    │ 24/04    │ $15.000  │ — Info │ ⋮    │
│ + Agregar fila…                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Cuando la previsión está CONFIRMADA:**
- Items dejan de ser editables (filas no clicables).
- Aparece columna "Acción" con botón **[Aplicar]** por fila pendiente (oculto en OTRO).
- Botón global **[Aplicar todos los pendientes]** en toolbar.
- Estados visuales por fila:
  - ⚪ PENDIENTE — gris
  - ✅ APLICADO — verde con link al registro real (ej. "→ SalaryPayment #432")
  - ⏭️ OMITIDO — amarillo con tooltip mostrando razón
  - — Info — solo en OTRO (sin acción)

**Modal "Aplicar gasto":** muestra preview del registro que se va a crear con todos los campos resueltos. Permite override de monto/fecha. Confirmación → llamada `POST /apply`.

**Modal "Revertir aplicación":** muestra warning con detalle "Esto eliminará el registro X #Y creado el ZZ. ¿Continuar?". Si el registro fue modificado tras aplicar, muestra detalle de cambios y requiere checkbox de confirmación.

### 8.4 Form de item con campos condicionales

Cuando el usuario abre el modal de item (o agrega fila inline), el primer campo es **Tipo**. Al cambiarlo, los campos siguientes se muestran/ocultan según la tabla de §1.4. Validación reactiva con Angular Forms (`updateValueAndValidity` al cambiar tipo).

### 8.5 Plantillas frecuentes

Vista similar a previsión pero sin `expectedDate` (usa `dayOffset`) ni estado. CRUD simple. Botón "Usar plantilla" abre el modal `forecast-from-template-modal` con preview de cómo quedará la previsión instanciada.

---

## 9. Mensajes i18n (`messages_es.properties`)

```properties
budgetForecast.notFound=Previsión no encontrada con id {0}
budgetForecast.invalidPeriod=La fecha de fin debe ser igual o posterior a la fecha de inicio
budgetForecast.notEditable=La previsión no se puede editar en estado {0}
budgetForecast.cannotConfirm.empty=No se puede confirmar una previsión sin items
budgetForecast.cannotReopen.hasApplied=No se puede reabrir una previsión con items ya aplicados — primero revertí las aplicaciones
budgetForecast.item.dateOutOfRange=La fecha del item ({0}) está fuera del período de la previsión ({1} - {2})
budgetForecast.item.requiredField=El campo {0} es obligatorio para items de tipo {1}

budgetForecast.apply.notConfirmed=Solo se pueden aplicar items de previsiones en estado CONFIRMADA
budgetForecast.apply.alreadyApplied=El item ya fue aplicado el {0}
budgetForecast.apply.notApplicable=Los items de tipo OTRO no se pueden aplicar (son solo informativos)
budgetForecast.apply.success=Item aplicado: se creó {0} #{1}

budgetForecast.revert.notApplied=El item no está aplicado, no hay nada que revertir
budgetForecast.revert.entityModified=El registro generado fue modificado tras aplicar — confirmá la reversión

budgetForecast.template.notFound=Plantilla de previsión no encontrada con id {0}
budgetForecast.template.duplicateName=Ya existe una plantilla con ese nombre

budgetForecast.import.invalidType=Tipo de item inválido en fila {0}: {1}. Valores permitidos: {2}
budgetForecast.import.entityNotFound=En fila {0}: {1} no encontrado para valor {2}
budgetForecast.import.dateOutOfRange=En fila {0}: la fecha {1} está fuera del período
```

---

## 10. Checklist de Implementación

### Backend
- [ ] Enums: `BudgetForecastStatus`, `BudgetForecastItemType`, `BudgetForecastItemApplicationStatus`
- [ ] Entidades: `BudgetForecast`, `BudgetForecastItem`, `BudgetForecastTemplate`, `BudgetForecastTemplateItem`
- [ ] Migración Liquibase: 4 tablas + índices
- [ ] DTOs request/response
- [ ] MapStruct mappers
- [ ] Repositorios con queries de filtro paginado
- [ ] Validador `BudgetForecastItemValidator` (campos requeridos por tipo)
- [ ] `BudgetForecastService` con todas las transiciones de estado y recalculadoras de totales
- [ ] Strategy `BudgetForecastItemApplier` + 5 implementaciones (SalaryPayment, ServicePayment, Repair, StockPurchase, LicencePlatePayment)
- [ ] `BudgetForecastTemplateService` con `instantiate()` (clonado de items)
- [ ] `BudgetForecastExcelService` (template, export, import con validación exhaustiva)
- [ ] `BudgetForecastPdfService` (estilo F12)
- [ ] `BudgetForecastController` + `BudgetForecastTemplateController`
- [ ] Permisos en `AppPermissions` + asignación a roles default
- [ ] Mensajes i18n
- [ ] Tests: validación por tipo, transición de estados, apply/revert por cada applier, import success/failure, plantilla → instanciación

### Frontend
- [ ] Modelos TypeScript (espejo de DTOs + enums)
- [ ] Servicio `BudgetForecastService` y `BudgetForecastTemplateService` (HTTP)
- [ ] Componentes: list, detail, items-grid, toolbar, status-badge
- [ ] Modales: form header, form item (con campos condicionales por tipo), from-template, import, apply, revert
- [ ] Sección de plantillas (CRUD)
- [ ] Tab "Previsiones" en navegación con permiso
- [ ] Rutas lazy en `app.routes.ts`
- [ ] Estilos BEM con prefijo `bf-`

### Validación end-to-end
- [ ] Crear previsión BORRADOR → agregar items de cada tipo → confirmar
- [ ] Aplicar item de cada tipo → verificar registro real generado en su entidad
- [ ] Revertir aplicación → verificar borrado del registro
- [ ] Bulk apply con éxitos y fallos mezclados → respuesta detallada
- [ ] Import Excel: subir plantilla con datos válidos → previsión creada
- [ ] Import Excel: subir con errores → ningún dato persistido, detalle por fila
- [ ] Export Excel + PDF de previsión existente → formato correcto
- [ ] Crear plantilla con dayOffsets → instanciar → fechas calculadas correctamente
- [ ] Duplicar previsión existente → nuevo BORRADOR con items copiados, sin estado de aplicación
- [ ] Reabrir CONFIRMADA con items aplicados → bloqueado con mensaje claro
- [ ] Auto-cierre cuando todos los items quedan en estado terminal
```