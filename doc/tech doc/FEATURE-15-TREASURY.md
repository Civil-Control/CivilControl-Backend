# Feature 15 — Tesorería: Cajas, Cuentas Bancarias y Chequeras

## Resumen

Introduce la infraestructura financiera del sistema mediante 3 nuevas entidades:
- **`CashBox`** — Cajas en efectivo con saldo trazable e historial de movimientos.
- **`BankAccount`** — Cuentas bancarias para cheques y transferencias, reemplazando el string libre `bankName` actual de los pagos.
- **`Checkbook`** — Chequeras (talonarios) opcionales que restringen los números de cheque emitidos.

Esta feature es **prerequisito** del reporte de Pagos Emitidos (F16) y del flujo de cheques con estado (también F16). El módulo nuevo se llama **"Tesorería"**.

> **Nota de orden:** Esta feature DEBE implementarse antes de F16. La numeración refleja la dependencia: F15 introduce el modelo de tesorería, F16 lo consume y agrega gestión de estado del cheque + reporte.

---

## 1. Entidad: `CashBox` (Caja)

**Ubicación:** `model/entity/treasury/CashBox.java`

```java
@Entity
@Table(name = "cash_boxes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
public class CashBox extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;       // cacheado, recalculado con cada movimiento

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @OneToMany(mappedBy = "cashBox", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CashBoxMovement> movements = new ArrayList<>();
}
```

### Entidad hija: `CashBoxMovement`

```java
@Entity
@Table(name = "cash_box_movements")
public class CashBoxMovement extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_box_id", nullable = false)
    private CashBox cashBox;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CashBoxMovementType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                           // siempre positivo; el tipo define el signo

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;                     // saldo resultante tras este movimiento

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(length = 500)
    private String comment;                              // motivo, requerido si type == AJUSTE

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_payment_id")
    private CashPayment cashPayment;                     // origen del movimiento si fue un pago
}
```

### Enum: `CashBoxMovementType`

```java
public enum CashBoxMovementType {
    INCREMENTO_MANUAL,         // entrada manual (saldo +)
    DECREMENTO_MANUAL,         // salida manual (saldo −)
    AJUSTE,                    // corrección (puede ser + o −, comentario obligatorio)
    PAGO_EMITIDO,              // egreso por CashPayment (saldo −)
    TRANSFERENCIA_INTERNA      // transferencia entre cajas (futuro)
}
```

### Reglas de negocio

- **Múltiples cajas por tenant** — sin límite. Casos de uso: caja chica de obra, caja administración, caja por sector, etc.
- **Saldo puede quedar negativo** — pero se debe avisar al usuario claramente:
  - Backend: si una operación deja `balance < 0`, NO se rechaza pero se devuelve un flag `negativeBalance: true` en la respuesta.
  - Frontend: badge rojo persistente "Saldo negativo" en la tarjeta de caja + advertencia confirmable al hacer movimientos que dejen saldo negativo.
- **Asociación con `CashPayment`:**
  - Si existe **al menos una** `CashBox` activa en el tenant → al crear un `CashPayment` la selección de caja es **obligatoria**.
  - Si NO existe ninguna `CashBox` → el campo no se muestra en el formulario y los pagos en efectivo no se asocian a ninguna caja.
- **Soft delete** — solo permitido si la caja tiene `balance == 0` y no tiene movimientos no-eliminables del último período (fuera de scope: definir).
- **`balance` siempre derivable de movimientos** — el campo cacheado se recalcula como `SUM(movements.signedAmount)` ante cada cambio. Job de reconciliación opcional.

### DTOs

**`CashBoxDTO` (request)**
```java
public record CashBoxDTO(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull BigDecimal initialBalance,           // solo en create; ignorado en update
    Boolean active
) {}
```

**`CashBoxResponseDTO`**
```java
public record CashBoxResponseDTO(
    Long id,
    String name,
    String description,
    BigDecimal balance,
    Boolean negativeBalance,                      // derivado: balance.compareTo(ZERO) < 0
    Boolean active,
    int movementCount,
    LocalDateTime lastMovementAt
) {}
```

**`CashBoxMovementDTO` (request)**
```java
public record CashBoxMovementDTO(
    @NotNull Long cashBoxId,
    @NotNull CashBoxMovementType type,            // INCREMENTO_MANUAL | DECREMENTO_MANUAL | AJUSTE
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull LocalDate movementDate,
    @Size(max = 500) String comment               // requerido si type == AJUSTE
) {}
```

> No se permite crear movimientos `PAGO_EMITIDO` ni `TRANSFERENCIA_INTERNA` desde este endpoint — esos son generados internamente por `PaymentService`.

**`CashBoxMovementResponseDTO`**
```java
public record CashBoxMovementResponseDTO(
    Long id,
    Long cashBoxId,
    String cashBoxName,
    CashBoxMovementType type,
    BigDecimal amount,
    BigDecimal signedAmount,                      // amount con signo según type
    BigDecimal balanceAfter,
    LocalDate movementDate,
    String comment,
    LocalDateTime createdAt,
    Long createdByUserId,
    String createdByUserName,
    Long cashPaymentId
) {}
```

### Endpoints

```
GET    /api/v1/treasury/cash-boxes                  — paginado + filtros (active, search)
GET    /api/v1/treasury/cash-boxes/{id}
POST   /api/v1/treasury/cash-boxes                  — create
PUT    /api/v1/treasury/cash-boxes/{id}             — update name/description/active
DELETE /api/v1/treasury/cash-boxes/{id}             — soft delete

GET    /api/v1/treasury/cash-boxes/{id}/movements   — historial paginado
POST   /api/v1/treasury/cash-boxes/movements        — crear movimiento manual (INCREMENTO/DECREMENTO/AJUSTE)
```

**Permisos:**
- `TREASURY_VIEW` — listar y ver
- `TREASURY_CREATE` — crear caja y movimientos manuales
- `TREASURY_UPDATE` — actualizar caja
- `TREASURY_DELETE` — soft delete
- `TREASURY_BALANCE_ADJUST` — crear movimientos tipo `AJUSTE`

---

## 2. Entidad: `BankAccount` (Cuenta Bancaria)

**Ubicación:** `model/entity/treasury/BankAccount.java`

```java
@Entity
@Table(name = "bank_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "account_number", "bank_name"})
})
public class BankAccount extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                                // nombre interno descriptivo

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;                            // string libre del banco

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    @Builder.Default
    private BankAccountType accountType = BankAccountType.CUENTA_CORRIENTE;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(length = 22)
    private String cbu;                                 // opcional

    @Column(length = 30)
    private String alias;                               // opcional

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    @Builder.Default
    private Currency currency = Currency.ARS;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;       // cacheado

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BankAccountMovement> movements = new ArrayList<>();
}
```

### Enum: `BankAccountType`
```java
public enum BankAccountType {
    CUENTA_CORRIENTE,    // default
    CAJA_AHORRO,
    CUENTA_SUELDO,
    CUENTA_USD,
    OTRA
}
```

### Entidad hija: `BankAccountMovement`

Mismo patrón que `CashBoxMovement` con tipos específicos del flujo bancario:

```java
public enum BankAccountMovementType {
    INCREMENTO_MANUAL,
    DECREMENTO_MANUAL,
    AJUSTE,                    // comentario obligatorio
    TRANSFERENCIA_EMITIDA,     // egreso por TransferPayment (saldo −)
    CHEQUE_EMITIDO,            // reserva por CheckPayment al crear (saldo − pendiente)
    CHEQUE_COBRADO,            // confirmación al pasar el cheque a COBRADO (no afecta saldo si ya estaba reservado)
    CHEQUE_RECHAZADO,          // reversión: devuelve el monto al saldo (saldo +)
    CHEQUE_CANCELADO,          // reversión: devuelve el monto al saldo (saldo +)
    DEPOSITO,                  // ingreso (futuro: cobros de venta)
    TRANSFERENCIA_INTERNA      // entre cuentas (futuro)
}
```

### Reglas de flujo bancario (crítico)

**Saldo de `BankAccount` reflejante del dinero comprometido:**

| Operación | Tipo de movimiento | Efecto en saldo |
|---|---|---|
| Crear `TransferPayment` | `TRANSFERENCIA_EMITIDA` | `−amount` |
| Crear `CheckPayment` (cualquier estado) | `CHEQUE_EMITIDO` | `−amount` (reserva inmediata) |
| Cambiar cheque → `COBRADO` | `CHEQUE_COBRADO` | `0` (ya reservado) |
| Cambiar cheque → `RECHAZADO` | `CHEQUE_RECHAZADO` | `+amount` (libera reserva) |
| Cambiar cheque → `CANCELADO` | `CHEQUE_CANCELADO` | `+amount` (libera reserva) |
| Cheque en estado `VENCIDO` (derivado) | (ninguno) | `0` (sigue reservado) |

Esta lógica se ejecuta automáticamente en `PaymentService` al crear pagos y en el endpoint `PATCH /payments/check/{id}/status` (F16).

### DTOs

**`BankAccountDTO` (request)**
```java
public record BankAccountDTO(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100) String bankName,
    @NotNull BankAccountType accountType,
    @NotBlank @Size(max = 50) String accountNumber,
    @Size(max = 22) String cbu,
    @Size(max = 30) String alias,
    Currency currency,
    @NotNull BigDecimal initialBalance,             // solo en create
    Boolean active
) {}
```

**`BankAccountResponseDTO`** — incluye balance actual, currency, count de chequeras activas asociadas.

**`BankAccountMovementResponseDTO`** — análogo a `CashBoxMovementResponseDTO`, con FK opcional a `checkPaymentId` o `transferPaymentId`.

### Endpoints

```
GET    /api/v1/treasury/bank-accounts                — listado paginado + filtros
GET    /api/v1/treasury/bank-accounts/{id}
POST   /api/v1/treasury/bank-accounts
PUT    /api/v1/treasury/bank-accounts/{id}
DELETE /api/v1/treasury/bank-accounts/{id}

GET    /api/v1/treasury/bank-accounts/{id}/movements
POST   /api/v1/treasury/bank-accounts/movements      — solo INCREMENTO_MANUAL/DECREMENTO_MANUAL/AJUSTE
```

Permisos: mismo grupo `TREASURY_*` que cajas.

---

## 3. Entidad: `Checkbook` (Chequera)

**Ubicación:** `model/entity/treasury/Checkbook.java`

```java
@Entity
@Table(name = "checkbooks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "checkbook_number", "bank_account_id"})
})
public class Checkbook extends TenantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "checkbook_number", nullable = false, length = 50)
    private String checkbookNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 15)
    private CheckType checkType;                       // INMEDIATO | DIFERIDO

    @Column(name = "range_from", nullable = false)
    private Long rangeFrom;                            // ej. 100

    @Column(name = "range_to", nullable = false)
    private Long rangeTo;                              // ej. 125

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;                     // false cuando todos los números fueron usados

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
```

### Enum: `CheckType`
```java
public enum CheckType {
    INMEDIATO,
    DIFERIDO
}
```

### Reglas de validación

- `rangeFrom <= rangeTo`, ambos > 0.
- `bankAccount` requerido.
- **Sin overlap dentro de la misma cuenta bancaria**: validar que `[rangeFrom, rangeTo]` no se superponga con otra `Checkbook` activa de la misma `bankAccount`.
- **Auto-desactivación**: cuando todos los números del rango fueron usados (cheques en estados NO terminales reversibles + cheques en estados consumidos), `active` pasa automáticamente a `false`.
- **Soft delete**: solo si no tiene cheques asociados con estado distinto de `CANCELADO`.

### Uso en `CheckPayment` (consumido por F16)

`CheckPayment` recibe FK opcional a `Checkbook`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "checkbook_id")
private Checkbook checkbook;                          // OPCIONAL
```

**Validación al crear/actualizar `CheckPayment`:**
- Si `checkbook != null`:
  - `checkNumber` (numérico) debe estar dentro de `[checkbook.rangeFrom, checkbook.rangeTo]`.
  - El `checkNumber` no debe haber sido usado por otro `CheckPayment` de la misma `Checkbook` (sin importar el estado del cheque previo — los números **quedan consumidos**).
  - El `checkbook.bankAccount` se utiliza también como `bankAccount` del `CheckPayment` (consistencia obligatoria — el sistema lo asigna automáticamente, no lo elige el usuario).
- Si `checkbook == null`:
  - El usuario ingresa libremente `checkNumber` con la validación actual (regex / length).
  - Igualmente debe seleccionar una `BankAccount` (obligatorio — F16).

**Política de número consumido:** Una vez asignado, el número de cheque queda permanentemente consumido aun si el cheque pasa a `CANCELADO` o `RECHAZADO`. El papel físico ya fue inutilizado.

### DTOs

**`CheckbookDTO` (request)**
```java
public record CheckbookDTO(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String checkbookNumber,
    @NotNull Long bankAccountId,
    @NotNull CheckType checkType,
    @NotNull @Positive Long rangeFrom,
    @NotNull @Positive Long rangeTo
) {}
```

**`CheckbookResponseDTO`**
```java
public record CheckbookResponseDTO(
    Long id,
    String name,
    String checkbookNumber,
    Long bankAccountId,
    String bankAccountName,
    String bankName,
    CheckType checkType,
    Long rangeFrom,
    Long rangeTo,
    int totalChecks,
    int usedChecks,
    int availableChecks,
    Long nextAvailableNumber,                          // primer número no usado, o null si agotada
    Boolean active
) {}
```

### Endpoints

```
GET    /api/v1/treasury/checkbooks
GET    /api/v1/treasury/checkbooks/{id}
GET    /api/v1/treasury/checkbooks/{id}/used-numbers   — lista de números ya consumidos
POST   /api/v1/treasury/checkbooks
PUT    /api/v1/treasury/checkbooks/{id}
DELETE /api/v1/treasury/checkbooks/{id}
```

---

## 4. Migración de Base de Datos

**Archivo:** `db/changelog/db.changelog-{nro}.xml`

Orden de operaciones (ejecutado en una sola transacción de Liquibase por changeset):

### 4.1 Crear tablas

```sql
CREATE TABLE cash_boxes (...);
CREATE TABLE cash_box_movements (...);
CREATE TABLE bank_accounts (...);
CREATE TABLE bank_account_movements (...);
CREATE TABLE checkbooks (...);

CREATE INDEX idx_cash_box_movements_box ON cash_box_movements(cash_box_id, movement_date);
CREATE INDEX idx_bank_account_movements_acc ON bank_account_movements(bank_account_id, movement_date);
CREATE INDEX idx_checkbooks_account ON checkbooks(bank_account_id, active);
```

### 4.2 Backfill inicial — `BankAccount` "Banco Provincia" por tenant

```sql
INSERT INTO bank_accounts (tenant_id, name, bank_name, account_type, account_number, balance, active, deleted, created_at, updated_at)
SELECT
    t.id,
    'Banco Provincia',
    'Banco Provincia',
    'CUENTA_CORRIENTE',
    'MIGRADA-PROVINCIA',
    0,
    true,
    false,
    NOW(),
    NOW()
FROM tenants t;
```

### 4.3 Migración del campo `bankName` en `check_payments` y `transfer_payments`

```sql
-- Paso 1: agregar columna nullable
ALTER TABLE check_payments ADD COLUMN bank_account_id BIGINT NULL REFERENCES bank_accounts(id);
ALTER TABLE check_payments ADD COLUMN checkbook_id BIGINT NULL REFERENCES checkbooks(id);
ALTER TABLE transfer_payments ADD COLUMN bank_account_id BIGINT NULL REFERENCES bank_accounts(id);

-- Paso 2: asociar todos los pagos existentes a la BankAccount "Banco Provincia" del mismo tenant
UPDATE check_payments cp
SET bank_account_id = (
    SELECT ba.id FROM bank_accounts ba
    WHERE ba.tenant_id = cp.tenant_id
      AND ba.name = 'Banco Provincia'
    LIMIT 1
);

UPDATE transfer_payments tp
SET bank_account_id = (
    SELECT ba.id FROM bank_accounts ba
    WHERE ba.tenant_id = tp.tenant_id
      AND ba.name = 'Banco Provincia'
    LIMIT 1
);

-- Paso 3: hacer la columna NOT NULL ahora que está backfilleada
ALTER TABLE check_payments ALTER COLUMN bank_account_id SET NOT NULL;
ALTER TABLE transfer_payments ALTER COLUMN bank_account_id SET NOT NULL;

-- Paso 4: eliminar el viejo campo bank_name
ALTER TABLE check_payments DROP COLUMN bank_name;
ALTER TABLE transfer_payments DROP COLUMN bank_name;
```

### 4.4 Asociación opcional de `CashPayment` con `CashBox`

```sql
ALTER TABLE cash_payments ADD COLUMN cash_box_id BIGINT NULL REFERENCES cash_boxes(id);
```

(NO se hace backfill — los `CashPayment` históricos quedan sin caja asociada. La obligatoriedad se aplica en validación a partir del momento en que existe al menos una `CashBox` activa.)

---

## 5. Cambios en entidades existentes

### 5.1 `CheckPayment` — eliminar `bankName`, agregar FKs

```java
// ELIMINAR:
// @Column(name = "bank_name", length = 100, columnDefinition = "VARCHAR(100)")
// private String bankName;

// AGREGAR:
@ManyToOne(optional = false, fetch = FetchType.LAZY)
@JoinColumn(name = "bank_account_id", nullable = false)
private BankAccount bankAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "checkbook_id")
private Checkbook checkbook;                        // OPCIONAL
```

Actualizar `CheckPaymentDTO`:
- Eliminar `String bankName`.
- Agregar `@NotNull Long bankAccountId`, `Long checkbookId` (opcional).

Actualizar `CheckPaymentResponseDTO`: incluir datos derivados (`bankAccountName`, `bankName` desde la cuenta, `checkbookId`, `checkbookName`, `checkbookNumber`).

### 5.2 `TransferPayment` — eliminar `bankName`, agregar FK

```java
// ELIMINAR:
// @Column(name = "bank_name", length = 100, columnDefinition = "VARCHAR(100)")
// private String bankName;

// AGREGAR:
@ManyToOne(optional = false, fetch = FetchType.LAZY)
@JoinColumn(name = "bank_account_id", nullable = false)
private BankAccount bankAccount;
```

Actualizar `TransferPaymentDTO` y `TransferPaymentResponseDTO` análogo.

### 5.3 `CashPayment` — agregar FK opcional a `CashBox`

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cash_box_id")
private CashBox cashBox;                            // condicionalmente requerido (ver regla)
```

Actualizar `CashPaymentDTO`: agregar `Long cashBoxId` (validación condicional en `PaymentService`).

### 5.4 `PaymentService` — orquestación con tesorería

Al crear un pago, ejecutar lógica de tesorería:

```java
// Pseudo-código de createCheck:
public CheckPaymentResponseDTO createCheck(CheckPaymentDTO dto) {
    // ... validaciones existentes
    validateCheckbookConsistency(dto);                  // checkNumber dentro del rango, no usado
    BankAccount account = resolveBankAccount(dto);      // del checkbook si aplica, o del bankAccountId

    CheckPayment check = ...;                           // crear y persistir
    bankAccountMovementService.registerCheckIssued(account, check);  // -amount, tipo CHEQUE_EMITIDO
    autoDeactivateCheckbookIfExhausted(dto.checkbookId());

    return checkPaymentMapper.toResponse(check);
}
```

Y en `updateCheckStatus` (F16):
- Cambio a `RECHAZADO` o `CANCELADO` → `bankAccountMovementService.registerCheckReversal(account, check)` (`+amount`).
- Cambio a `COBRADO` → registrar `CHEQUE_COBRADO` (informativo, no altera saldo).

---

## 6. Frontend

### 6.1 Módulo nuevo: "Tesorería"

**Ubicación:** `domains/treasury/`
- Subsecciones:
  - `cash-boxes/` — listado + detalle con tabs Resumen/Movimientos
  - `bank-accounts/` — listado + detalle con tabs Resumen/Movimientos/Chequeras
  - `checkbooks/` — listado + detalle con números usados/disponibles

### 6.2 Tab de navegación

En `navigation.config.ts`:
```typescript
{
  id: 'tesoreria',
  label: 'Tesorería',
  icon: 'account_balance',
  permissions: ['TREASURY_VIEW'],
  children: [
    { id: 'cajas',             label: 'Cajas',             route: '/tesoreria/cajas' },
    { id: 'cuentas-bancarias', label: 'Cuentas Bancarias', route: '/tesoreria/cuentas-bancarias' },
    { id: 'chequeras',         label: 'Chequeras',         route: '/tesoreria/chequeras' },
  ],
}
```

### 6.3 UX de la Caja — clave del requerimiento

**No se permite editar el saldo directamente como un input.** En vez de eso:

**Vista de detalle de Caja:**
```
┌──────────────────────────────────────────────────────────┐
│  Caja: Caja Chica Obra Norte                             │
│  ┌──────────────────────────────────┐                    │
│  │   Saldo Actual                   │                    │
│  │   $ 142.350,00                   │  [⚠ Negativo]     │ ← badge si < 0
│  │   Última operación: 22/04/2026   │                    │
│  └──────────────────────────────────┘                    │
│                                                          │
│  [+ Agregar dinero]   [− Quitar dinero]   [⚙ Ajustar]   │
│                                                          │
│  ── Historial de Movimientos ──                          │
│  Fecha       Tipo               Monto      Saldo  Usuario│
│  22/04/2026  Pago Emitido      −$5.000   $142.350  ...  │
│  20/04/2026  Incremento Manual +$50.000  $147.350  ...  │
│  18/04/2026  Ajuste            −$1.200   $97.350   ...  │
│  ...                                                     │
└──────────────────────────────────────────────────────────┘
```

**Modales:**
- "Agregar dinero" → form con `amount`, `movementDate`, `comment`. Tipo = `INCREMENTO_MANUAL`.
- "Quitar dinero" → form con `amount`, `movementDate`, `comment`. Tipo = `DECREMENTO_MANUAL`. Si dejara saldo negativo → confirmación con texto rojo "Esta operación dejará la caja con saldo negativo: $-X. ¿Continuar?".
- "Ajustar" → permiso `TREASURY_BALANCE_ADJUST`. Form con `targetAmount` (saldo objetivo) que el sistema convierte en un movimiento de tipo `AJUSTE` con la diferencia. `comment` obligatorio.

### 6.4 UX de Cuenta Bancaria

Misma filosofía que Caja: saldo no editable, historial visible, modales de movimientos manuales. Tab adicional "Chequeras" lista las chequeras asociadas con su estado (números disponibles/usados).

### 6.5 UX de Chequera

Vista de detalle:
- Datos: nombre, número, banco/cuenta, tipo (Inmediato/Diferido), rango.
- **Grid visual** de números: tarjetas con cada número del rango, coloreadas:
  - Disponible — gris claro
  - Usado (cheque vigente) — naranja con N° de pago
  - Usado (cheque cobrado) — verde
  - Usado (cheque rechazado/cancelado) — rojo tachado

### 6.6 Cambios en formulario de Pagos (cheque/transferencia)

- Eliminar input texto `Banco`.
- Agregar **autocomplete de `BankAccount`** (obligatorio) — muestra "Nombre — Banco — N° Cuenta".
- Para cheques: agregar **autocomplete opcional de `Checkbook`** filtrado por la `BankAccount` seleccionada.
  - Al seleccionar chequera: input `checkNumber` se valida contra el rango y números usados (validación inline + en submit).
  - Helper visible: "Números disponibles: 105, 107, 110-125".
- Para pagos en efectivo: autocomplete obligatorio de `CashBox` (solo si existe alguna activa).

---

## 7. Permisos nuevos

```java
public class AppPermissions {
    // ... existentes
    public static final String TREASURY_VIEW = "TREASURY_VIEW";
    public static final String TREASURY_CREATE = "TREASURY_CREATE";
    public static final String TREASURY_UPDATE = "TREASURY_UPDATE";
    public static final String TREASURY_DELETE = "TREASURY_DELETE";
    public static final String TREASURY_BALANCE_ADJUST = "TREASURY_BALANCE_ADJUST";
}
```

Asignar al rol Admin por defecto. El rol Operador puede recibir `TREASURY_VIEW` + `TREASURY_CREATE` + `TREASURY_UPDATE` (sin `BALANCE_ADJUST` ni `DELETE`).

---

## 8. Mensajes i18n (en `messages_es.properties`)

```properties
treasury.cashBox.name.required=El nombre de la caja es obligatorio
treasury.cashBox.notFound=Caja no encontrada con id {0}
treasury.cashBox.deleteWithBalance=No se puede eliminar una caja con saldo distinto de cero
treasury.cashBox.movement.adjust.commentRequired=El comentario es obligatorio para movimientos de ajuste
treasury.cashBox.required=Existe al menos una caja registrada — la selección de caja es obligatoria
treasury.cashBox.balance.negative=La operación dejará la caja con saldo negativo

treasury.bankAccount.required=La cuenta bancaria es obligatoria
treasury.bankAccount.notFound=Cuenta bancaria no encontrada con id {0}
treasury.bankAccount.duplicate=Ya existe una cuenta con ese número en el banco indicado

treasury.checkbook.notFound=Chequera no encontrada con id {0}
treasury.checkbook.range.invalid=El rango de números es inválido (desde debe ser <= hasta)
treasury.checkbook.range.overlap=El rango se solapa con otra chequera de la misma cuenta
treasury.checkbook.numberOutOfRange=El número de cheque está fuera del rango de la chequera ({0} - {1})
treasury.checkbook.numberAlreadyUsed=El número de cheque {0} ya fue usado en esta chequera
treasury.checkbook.exhausted=La chequera está agotada (todos los números fueron consumidos)
```

---

## 9. Checklist de Implementación

### Backend
- [ ] Enums: `CashBoxMovementType`, `BankAccountType`, `BankAccountMovementType`, `CheckType`, `Currency` (si no existe)
- [ ] Entidades: `CashBox`, `CashBoxMovement`, `BankAccount`, `BankAccountMovement`, `Checkbook`
- [ ] Modificar entidades: `CheckPayment` (FK BankAccount + Checkbook), `TransferPayment` (FK BankAccount), `CashPayment` (FK CashBox)
- [ ] Migración Liquibase: crear tablas + índices + backfill "Banco Provincia" por tenant + UPDATE de pagos + drop bank_name
- [ ] DTOs (request/response) para cada entidad nueva
- [ ] Mappers MapStruct
- [ ] Repositorios con queries de soporte (números usados por chequera, validaciones de overlap)
- [ ] Services: `CashBoxService`, `BankAccountService`, `CheckbookService`, `BankAccountMovementService`
- [ ] Modificar `PaymentService`: validaciones de chequera + registro de movimientos en BankAccount/CashBox al crear pago
- [ ] Controllers REST nuevos: `TreasuryController` (o uno por entidad)
- [ ] Permisos en `AppPermissions` + asignación al rol Admin
- [ ] Mensajes i18n
- [ ] Tests unitarios de validaciones (rango, overlap, saldo negativo, número consumido)

### Frontend
- [ ] Módulo `domains/treasury/` con 3 sub-features
- [ ] Modelos TypeScript espejo de DTOs
- [ ] Servicios HTTP (`CashBoxService`, `BankAccountService`, `CheckbookService`)
- [ ] Componentes: listado + detalle de cada entidad
- [ ] Modales de movimientos (Agregar / Quitar / Ajustar)
- [ ] Componente grid visual de números de chequera
- [ ] Modificar formularios de pagos (cheque/transferencia/efectivo) con autocomplete de BankAccount/Checkbook/CashBox
- [ ] Validación inline de número de cheque vs rango/usados
- [ ] Tab "Tesorería" en navegación
- [ ] Rutas en `app.routes.ts` lazy

### Validación end-to-end
- [ ] Crear caja → agregar dinero → quitar dinero → ajustar → ver historial
- [ ] Forzar saldo negativo de caja → confirmar advertencia visual
- [ ] Crear cuenta bancaria → registrar transferencia → verificar saldo decrementado
- [ ] Crear chequera con rango 100-125 → emitir cheque 105 → verificar que 105 deja de estar disponible
- [ ] Marcar cheque como CANCELADO → verificar que 105 NO se libera (queda consumido)
- [ ] Marcar cheque como RECHAZADO → verificar que el saldo de la cuenta vuelve a su valor anterior
- [ ] Pago en efectivo: con caja existente → caja obligatoria; sin cajas → campo no aparece
- [ ] Pago por cheque sin chequera → ingresar número manualmente → validación regex actual
- [ ] Pago por cheque con chequera → número fuera de rango → error claro
- [ ] Verificar migración: pagos históricos quedan asociados a "Banco Provincia"
