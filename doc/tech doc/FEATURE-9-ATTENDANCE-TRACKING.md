# Feature 9 — Asistencia Inteligente: Registro de Fichajes con Integración Biométrica

## 1. Resumen

Feature completa de registro de asistencia (fichaje) de empleados. Cada registro representa **un único movimiento**: una ENTRADA o una SALIDA. El sistema soporta cuatro vías de ingreso de datos:

1. **CRUD completo** — Idéntico a los demás módulos del sistema (tabla con filtros, formulario batch, detalle, paginación, sort).
2. **Importación masiva desde Excel** — El usuario sube un archivo `.xlsx` con un formato predefinido y el sistema lo valida exhaustivamente antes de persistir los datos.
3. **Exportación a Excel** — El usuario descarga los registros filtrados en un `.xlsx` con el mismo formato que la plantilla de importación (ida y vuelta compatible).
4. **Integración biométrica vía webhook** — Relojes biométricos (actualmente Hikvision) envían eventos HTTP al sistema, que los parsea y crea registros de asistencia automáticamente. El diseño utiliza un **sistema de adaptadores genéricos** (strategy pattern) para soportar múltiples marcas de dispositivos.

---

## 2. Modelo de Datos

### 2.1 Entidad: `AttendanceRecord`

Cada fila representa **un solo movimiento** (entrada o salida) de un empleado.

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `id` | `Long` (PK, auto) | No | Identificador único |
| `tenant_id` | `Long` | No | Heredado de `TenantEntity` |
| `employee` | `Employee` (FK) | No | Empleado que registra el movimiento |
| `date` | `LocalDate` | No | Fecha del movimiento |
| `time` | `LocalTime` | No | Hora del movimiento |
| `movementType` | `MovementType` (enum) | No | `ENTRADA` o `SALIDA` |
| `building` | `Building` (FK) | Sí | Edificio donde se registró (opcional) |
| `observation` | `String(500)` | Sí | Nota libre del usuario (opcional) |

**Restricciones:**
- Unique constraint compuesto por tenant: `(tenant_id, employee_id, date, time, movement_type)` — Evita duplicados exactos.
- La fecha no puede ser futura.
- El empleado referenciado no debe estar eliminado (soft delete).

### 2.2 Enum: `MovementType`

```java
public enum MovementType {
    ENTRADA("Entrada"),
    SALIDA("Salida");

    private final String displayName;
    // constructor + getter
}
```

Ubicación: `PSG.backEnd.model.enums.employee.MovementType`

### 2.3 Tabla SQL: `attendance_records`

```sql
CREATE TABLE attendance_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    employee_id     BIGINT       NOT NULL,
    date            DATE         NOT NULL,
    time            TIME         NOT NULL,
    movement_type   VARCHAR(10)  NOT NULL,  -- 'ENTRADA' | 'SALIDA'
    building_id     BIGINT       NULL,
    observation     VARCHAR(500) NULL,

    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_attendance_building FOREIGN KEY (building_id) REFERENCES buildings(id),
    CONSTRAINT uk_attendance_record   UNIQUE (tenant_id, employee_id, date, time, movement_type)
);
```

---

## 3. Backend — Capas

### 3.1 DTO: `AttendanceRecordDTO` (record, para CREATE y UPDATE)

```java
public record AttendanceRecordDTO(
    @NotNull(groups = OnCreate.class) Long employeeId,
    @NotNull(groups = OnCreate.class) LocalDate date,
    @NotNull(groups = OnCreate.class) LocalTime time,
    @NotNull(groups = OnCreate.class) MovementType movementType,
    Long buildingId,          // opcional
    @Size(max = 500) String observation  // opcional
) {}
```

### 3.2 DTO: `AttendanceRecordResponseDTO` (record)

```java
public record AttendanceRecordResponseDTO(
    Long id,
    Long employeeId,
    String employeeName,
    String employeeLastName,
    String employeeDni,
    LocalDate date,
    LocalTime time,
    MovementType movementType,
    Long buildingId,
    String buildingName,
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    String observation
) {}
```

### 3.3 DTO: `AttendanceRecordFilterDTO` (record)

```java
public record AttendanceRecordFilterDTO(
    Long employeeId,
    String firstName,
    String lastName,
    String dni,
    MovementType movementType,
    Long buildingId,
    Long projectAreaId,
    LocalDate dateFrom,
    LocalDate dateTo,
    LocalTime timeFrom,
    LocalTime timeTo,
    String search
) {}
```

### 3.4 DTO: `AttendanceRecordBatchDTO` (record)

```java
public record AttendanceRecordBatchDTO(
    @NotNull @Size(min = 1, max = 100) @Valid
    List<AttendanceRecordDTO> records
) {}
```

### 3.5 Controller: `AttendanceRecordController`

Ruta base: `/api/v1/attendance-records`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/` | `ATTENDANCE_RECORD_WRITE` | Crear un registro |
| `POST` | `/batch` | `ATTENDANCE_RECORD_WRITE` | Crear múltiples registros (max 100) |
| `POST` | `/import` | `ATTENDANCE_RECORD_WRITE` | Importar desde Excel (multipart) |
| `GET` | `/` | `ATTENDANCE_RECORD_READ` | Listar con filtros + paginación |
| `GET` | `/{id}` | `ATTENDANCE_RECORD_READ` | Obtener por ID |
| `GET` | `/export` | `ATTENDANCE_RECORD_READ` | Exportar registros filtrados a Excel |
| `GET` | `/import/template` | `ATTENDANCE_RECORD_READ` | Descargar plantilla Excel vacía |
| `PATCH` | `/{id}` | `ATTENDANCE_RECORD_WRITE` | Actualizar parcial |
| `DELETE` | `/{id}` | `ATTENDANCE_RECORD_DELETE` | Eliminar |

**Sort field mapping:**
- `employeeName` → `employee.name`
- `employeeLastName` → `employee.lastName`
- `employeeDni` → `employee.dni`
- `buildingName` → `building.name`

### 3.6 Service: `AttendanceRecordService`

Implementa `IAttendanceRecordService`. Métodos principales:

- `createAttendanceRecord(dto)` — Valida empleado, building, duplicados, fecha no futura. Persiste.
- `createBatchAttendanceRecords(batchDTO)` — Itera items delegando a la creación individual.
- `importFromExcel(MultipartFile)` — Parsea, valida, retorna resultado detallado (ver sección 5).
- `getAllAttendanceRecords(filterDTO, pageable)` — Consulta paginada con filtros.
- `getAttendanceRecordById(id)` — Lookup.
- `updateAttendanceRecord(id, dto)` — Actualización parcial.
- `deleteAttendanceRecord(id)` — Eliminación.
- `generateTemplate()` — Genera archivo `.xlsx` de plantilla vacía con headers y ejemplo.
- `exportToExcel(filterDTO)` — Genera archivo `.xlsx` con los registros que coincidan con los filtros aplicados (ver sección 6).

**Validaciones de negocio:**
1. **Empleado existe** y no está eliminado.
2. **Edificio existe** (si se provee) y no está eliminado.
3. **Fecha no futura** — `date` no puede ser posterior a hoy.
4. **Duplicado exacto** — No puede existir otro registro con mismo (employee, date, time, movementType).
5. **Coherencia temporal** — WARNING (no bloquea): si hay una SALIDA sin ENTRADA previa el mismo día, o una ENTRADA duplicada sin SALIDA intermedia. Esto se informa pero no impide guardar.

### 3.7 Repository: `AttendanceRecordRepository`

```java
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByIdAndEmployeeDeletedFalse(Long id);

    // Filtro paginado con JPQL dinámico (mismo patrón que SalaryPaymentRepository)
    @Query("SELECT ar FROM AttendanceRecord ar " +
           "WHERE ar.employee.deleted = false " +
           "AND (CAST(:employeeId AS long) IS NULL OR ar.employee.id = :employeeId) " +
           "AND (:firstName IS NULL OR LOWER(CAST(ar.employee.name AS string)) LIKE ...) " +
           // ... demás filtros con patrón IS NULL OR ...
           )
    Page<AttendanceRecord> findAllWithFilters(..., Pageable pageable);

    // Detección de duplicados
    boolean existsByEmployeeIdAndDateAndTimeAndMovementType(
        Long employeeId, LocalDate date, LocalTime time, MovementType movementType);

    // Para validación de coherencia (entradas/salidas del día)
    List<AttendanceRecord> findByEmployeeIdAndDateOrderByTimeAsc(Long employeeId, LocalDate date);
}
```

### 3.8 Excepciones

| Excepción | Caso |
|---|---|
| `AttendanceRecordNotFoundException` | ID no encontrado |
| `DuplicateAttendanceRecordException` | Registro duplicado (mismo empleado, fecha, hora, tipo) |
| `AttendanceImportException` | Error general de importación |
| `AttendanceImportValidationException` | Errores de validación en filas del Excel (contiene lista de errores por fila) |

### 3.9 Permisos

Agregar en `AppPermissions.java`:

```java
// ATTENDANCE RECORDS MODULE
public static final String ATTENDANCE_RECORD_READ   = "ATTENDANCE_RECORD_READ";
public static final String ATTENDANCE_RECORD_WRITE  = "ATTENDANCE_RECORD_WRITE";
public static final String ATTENDANCE_RECORD_DELETE  = "ATTENDANCE_RECORD_DELETE";
```

---

## 4. Frontend — Estructura

### 4.1 Ubicación

```
domains/human-resources/attendance/
├── attendance-detail/
│   ├── attendance-detail.ts
│   ├── attendance-detail.html
│   └── attendance-detail.scss
├── attendance-form/
│   ├── attendance-form.ts
│   ├── attendance-form.html
│   └── attendance-form.scss
├── attendance-export/              ← NUEVO (panel de exportación)
│   ├── attendance-export.ts
│   ├── attendance-export.html
│   └── attendance-export.scss
├── attendance-import/              ← NUEVO (componente de importación)
│   ├── attendance-import.ts
│   ├── attendance-import.html
│   └── attendance-import.scss
├── attendance-page/
│   ├── attendance-page.ts
│   ├── attendance-page.html
│   └── attendance-page.scss
├── attendance-table/
│   ├── attendance-table.ts
│   ├── attendance-table.html
│   └── attendance-table.scss
└── services/
    ├── attendance.service.ts
    ├── attendance-form.service.ts
    └── attendance-import.service.ts  ← NUEVO
```

### 4.2 Modelo: `AttendanceRecord`

```typescript
export interface AttendanceRecord {
  id?: number;
  employeeId: number;
  employeeName: string;
  employeeLastName: string;
  employeeDni: string;
  date: string;           // 'YYYY-MM-DD'
  time: string;           // 'HH:mm'
  movementType: MovementType;
  buildingId?: number | null;
  buildingName?: string;
  projectAreaId?: number | null;
  projectAreaName?: string;
  projectAreaColor?: string;
  observation?: string | null;
}

export enum MovementType {
  ENTRADA = 'ENTRADA',
  SALIDA = 'SALIDA'
}

export interface AttendanceRecordFilters {
  employeeId?: number | null;
  firstName?: string;
  lastName?: string;
  dni?: string;
  movementType?: MovementType | null;
  buildingId?: number | null;
  projectAreaId?: number | null;
  dateFrom?: string | null;
  dateTo?: string | null;
  timeFrom?: string | null;
  timeTo?: string | null;
  search?: string;
}
```

### 4.3 Tabla — Columnas

| Columna | Tipo | Sorteable | Filtrable |
|---|---|---|---|
| Empleado (nombre + apellido) | text | ✅ | ✅ (employee-search) |
| DNI | text | ✅ | ✅ (text) |
| Fecha | date | ✅ (default desc) | ✅ (date range) |
| Hora | time | ✅ | ✅ (time range) |
| Tipo de Movimiento | badge | ✅ | ✅ (select: Entrada/Salida) |
| Edificio | text | ✅ | ✅ (select: lista de buildings) |
| Área | badge color | — | ✅ (select: lista de áreas) |
| Observación | text (truncado) | — | — |

### 4.4 Formulario — Campos (Batch)

Formulario batch idéntico al patrón de SalaryPayment:
- **Header con defaults**: Fecha por defecto, Tipo de Movimiento por defecto, Edificio por defecto.
- **Items individuales**: Cada fila tiene Empleado (searchable), Fecha, Hora, Tipo, Edificio, Observación.
- Los defaults del header se aplican a todos los items nuevos.
- El usuario puede agregar/quitar filas dinámicamente.

### 4.5 Detalle

Vista de solo lectura mostrando todos los campos del registro con formato legible.

### 4.6 Ruta

Agregar en `human-resources.routes.ts`:

```typescript
{
  path: 'attendance',
  loadComponent: async () =>
    (await import('./attendance/attendance-page/attendance-page')).AttendancePage,
  canMatch: [hasPermissionMatch('ATTENDANCE_RECORD_READ')],
}
```

---

## 5. Importación desde Excel — Diseño Detallado

### 5.1 Formato de la Plantilla Excel

El sistema ofrece una plantilla `.xlsx` descargable. La hoja se llama **"Registros de Asistencia"** y tiene estas columnas:

| Columna | Header | Tipo | Obligatorio | Formato / Ejemplo |
|---|---|---|---|---|
| A | `DNI` | Texto/Número | ✅ | `12345678` (7-8 dígitos) |
| B | `Fecha` | Fecha | ✅ | `DD/MM/YYYY` (ej: `15/03/2026`) |
| C | `Hora` | Hora | ✅ | `HH:mm` (ej: `08:30`) |
| D | `Tipo de Movimiento` | Texto | ✅ | `Entrada` o `Salida` (case-insensitive) |
| E | `Edificio` | Texto | ❌ | Nombre o código del edificio tal como está en el sistema |
| F | `Observación` | Texto | ❌ | Texto libre (max 500 chars) |

**La plantilla incluye:**
- Fila 1: Headers en negrita con color de fondo.
- Fila 2: Ejemplo de datos válidos (en gris claro, para que el usuario entienda el formato).
- Fila 3: Segundo ejemplo con tipo "Salida" y sin edificio (mostrando que es opcional).
- Una segunda hoja oculta **"Instrucciones"** con reglas claras y tips.

### 5.2 Flujo de Importación — Frontend

```
[Botón "Importar Excel" en toolbar de tabla]
        │
        ▼
[Modal de importación se abre]
        │
        ├─ Paso 1: Subida de archivo
        │   • Drag & drop o file picker (.xlsx solamente)
        │   • Botón "Descargar plantilla" visible y prominente
        │   • Texto explicativo: "Descargue la plantilla, complete los datos y súbala aquí"
        │   • Validación de extensión en frontend antes de enviar
        │
        ├─ Paso 2: Vista previa y validación
        │   • Se envía el archivo al backend (POST /import con dry-run=true)
        │   • El backend parsea y valida TODO, retorna resultado sin persistir
        │   • Se muestra una tabla de previsualización:
        │       ✅ Filas válidas (en verde)
        │       ⚠️ Filas con warnings (en amarillo) — ej: SALIDA sin ENTRADA previa
        │       ❌ Filas con errores (en rojo) — ej: DNI no encontrado, formato inválido
        │   • Cada fila con error muestra el mensaje específico en lenguaje claro
        │   • Se muestra resumen: "X registros válidos, Y con advertencias, Z con errores"
        │   • Si hay errores: botón de confirmación deshabilitado. Debe corregir el Excel.
        │   • Si solo hay warnings: botón habilitado con confirmación extra
        │   • Si todo OK: botón de confirmación habilitado directamente
        │
        └─ Paso 3: Confirmación e importación
            • El usuario confirma → se envía POST /import (sin dry-run)
            • El backend vuelve a validar (por seguridad) y persiste
            • Se muestra resultado final: "Se importaron X registros exitosamente"
            • Se refresca la tabla automáticamente
```

### 5.3 Endpoint de Importación — Backend

#### `POST /api/v1/attendance-records/import`

**Request:**
- Content-Type: `multipart/form-data`
- Param `file`: archivo `.xlsx`
- Param `dryRun`: `boolean` (default `true`) — Si true, valida sin persistir.

**Response:** `AttendanceImportResultDTO`

```java
public record AttendanceImportResultDTO(
    int totalRows,
    int validRows,
    int warningRows,
    int errorRows,
    boolean imported,   // true si se persistieron los datos (dryRun=false y sin errores)
    List<AttendanceImportRowResultDTO> rows
) {}

public record AttendanceImportRowResultDTO(
    int rowNumber,
    RowStatus status,          // VALID, WARNING, ERROR
    String dni,
    String employeeName,       // nombre resuelto (null si DNI no encontrado)
    LocalDate date,
    LocalTime time,
    String movementType,
    String buildingName,
    String observation,
    List<String> messages      // lista de mensajes de error o warning
) {}

public enum RowStatus {
    VALID, WARNING, ERROR
}
```

#### `GET /api/v1/attendance-records/import/template`

Retorna un archivo `.xlsx` generado con Apache POI con los headers y ejemplos.
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="plantilla_asistencia.xlsx"`

### 5.4 Validaciones de Importación (por fila)

#### Errores (bloquean la importación)

| # | Validación | Mensaje para el usuario |
|---|---|---|
| E1 | DNI vacío o ausente | `"Fila X: El DNI es obligatorio"` |
| E2 | DNI no es numérico de 7-8 dígitos | `"Fila X: El DNI 'abc' no tiene un formato válido (debe ser 7-8 dígitos)"` |
| E3 | DNI no corresponde a ningún empleado activo | `"Fila X: No se encontró un empleado activo con DNI 12345678"` |
| E4 | Fecha vacía o formato inválido | `"Fila X: La fecha es obligatoria y debe tener formato DD/MM/YYYY"` |
| E5 | Fecha futura | `"Fila X: La fecha 20/12/2030 es posterior a hoy y no es válida"` |
| E6 | Hora vacía o formato inválido | `"Fila X: La hora es obligatoria y debe tener formato HH:mm (ej: 08:30)"` |
| E7 | Tipo de movimiento vacío o irreconocible | `"Fila X: El tipo de movimiento debe ser 'Entrada' o 'Salida'"` |
| E8 | Edificio indicado no existe en el sistema | `"Fila X: No se encontró el edificio 'Depósito Sur' en el sistema"` |
| E9 | Observación supera 500 caracteres | `"Fila X: La observación supera el máximo de 500 caracteres"` |
| E10 | Duplicado exacto dentro del mismo Excel | `"Fila X: Este registro es idéntico a la fila Y (mismo empleado, fecha, hora y tipo)"` |
| E11 | Duplicado contra la base de datos | `"Fila X: Ya existe un registro de ENTRADA para el empleado DNI 12345678 el 15/03/2026 a las 08:30"` |
| E12 | Fila completamente vacía (se ignora silenciosamente, no cuenta como error) | — |

#### Advertencias (no bloquean, pero se informan)

| # | Validación | Mensaje para el usuario |
|---|---|---|
| W1 | SALIDA sin ENTRADA previa ese día | `"Fila X: Se registra una SALIDA para DNI 12345678 el 15/03/2026 pero no hay una ENTRADA previa ese día"` |
| W2 | ENTRADA cuando ya hay una ENTRADA sin SALIDA ese día | `"Fila X: Se registra otra ENTRADA para DNI 12345678 el 15/03/2026 pero la entrada anterior (08:30) no tiene salida registrada"` |
| W3 | Hora de SALIDA anterior a hora de ENTRADA del mismo día | `"Fila X: La hora de SALIDA (07:00) es anterior a la última ENTRADA (08:30) del mismo día"` |
| W4 | Registro en día no laborable (sábado/domingo) | `"Fila X: El 15/03/2026 es un día sábado. Verifique si el registro es correcto"` |

### 5.5 Proceso de Validación — Lógica Interna

```
1. Validar archivo
   ├─ ¿Es .xlsx? → Si no, rechazar con mensaje claro
   ├─ ¿Tiene la hoja "Registros de Asistencia"? → Si no, buscar primera hoja
   ├─ ¿Supera el límite de filas (1000)? → Si sí, rechazar
   └─ ¿Tiene los headers correctos en fila 1? → Si no, rechazar con detalle

2. Precargar datos de referencia (una sola consulta cada uno)
   ├─ Mapa de DNI → Employee (solo activos, no eliminados del tenant)
   ├─ Mapa de nombre/código → Building (activos del tenant)
   └─ Registros existentes del rango de fechas del Excel (para check de duplicados)

3. Iterar fila por fila (desde fila 2)
   ├─ Saltar filas completamente vacías
   ├─ Validar formato de cada celda
   ├─ Resolver referencias (DNI → empleado, nombre → edificio)
   ├─ Verificar duplicados intra-excel (set de claves ya vistas)
   ├─ Verificar duplicados contra BD (set precargado)
   ├─ Verificar coherencia temporal (warnings)
   └─ Acumular resultado por fila

4. Si dryRun=false Y errorRows == 0
   └─ Persistir todos los registros válidos en una transacción
```

### 5.6 Componente de Importación — Frontend

El componente `attendance-import` es un **modal/panel lateral** que se abrirá desde un botón en la toolbar de la tabla.

**Estados del componente:**
1. `UPLOAD` — Estado inicial, muestra drag-drop zone y botón de plantilla.
2. `VALIDATING` — Spinner mientras el backend procesa el archivo.
3. `PREVIEW` — Muestra tabla de previsualización con colores por estado de fila.
4. `IMPORTING` — Spinner durante la persistencia final.
5. `DONE` — Mensaje de éxito con resumen.
6. `ERROR` — Error general (archivo corrupto, error de red, etc.).

**UX clave:**
- El botón "Descargar plantilla" siempre visible en el paso UPLOAD.
- En la previsualización, las filas con error tienen un ícono ❌ y tooltip con el mensaje.
- Las filas con warning tienen un ícono ⚠️ y tooltip explicativo.
- Si hay errores, un banner claro dice: *"Corrija los errores en el Excel y vuelva a subirlo"*.
- Botón "Confirmar importación" deshabilitado si hay errores, habilitado si solo warnings.

---

## 6. Exportación a Excel — Diseño Detallado

### 6.1 Concepto General

El usuario puede descargar los registros de asistencia en un archivo `.xlsx` que usa **el mismo formato de columnas que la plantilla de importación**. Esto permite un flujo de ida y vuelta: exportar → revisar/corregir en Excel → reimportar.

El Excel exportado aplica los mismos filtros que el usuario tiene activos en la tabla, y tiene valores por defecto inteligentes para que una exportación sin configurar ya sea útil.

### 6.2 Endpoint

#### `GET /api/v1/attendance-records/export`

**Query Params** (todos opcionales, mismos que el GET de listado):

| Param | Tipo | Valor por defecto | Descripción |
|---|---|---|---|
| `employeeId` | Long | — | Filtrar por empleado específico |
| `firstName` | String | — | Búsqueda parcial por nombre |
| `lastName` | String | — | Búsqueda parcial por apellido |
| `dni` | String | — | Filtrar por DNI |
| `movementType` | MovementType | — | `ENTRADA` o `SALIDA` |
| `buildingId` | Long | — | Filtrar por edificio |
| `projectAreaId` | Long | — | Filtrar por área de proyecto |
| `dateFrom` | LocalDate | **Primer día del mes actual** | Inicio del rango de fechas |
| `dateTo` | LocalDate | **Hoy** | Fin del rango de fechas |
| `timeFrom` | LocalTime | `00:00` | Inicio del rango horario |
| `timeTo` | LocalTime | `23:59` | Fin del rango horario |
| `search` | String | — | Búsqueda genérica (nombre, apellido) |

**Response:**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="asistencia_YYYY-MM-DD.xlsx"`
- El nombre del archivo incluye la fecha de exportación para fácil identificación.

**Límite:** Máximo 5000 registros por exportación. Si la consulta supera ese límite, se retorna HTTP 400 con mensaje: *"La exportación supera el límite de 5000 registros. Aplique más filtros para reducir los resultados."*

### 6.3 Formato del Excel Exportado

El archivo tiene una **única hoja** llamada **"Registros de Asistencia"** (mismo nombre que la plantilla de importación).

#### Estructura del archivo

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fila 1: ENCABEZADO DEL REPORTE (merge A1:F1)                     │
│  "Registros de Asistencia — 01/04/2026 al 02/04/2026"             │
│  Fuente 14pt, negrita, color de fondo corporativo                  │
├─────────────────────────────────────────────────────────────────────┤
│  Fila 2: FILTROS APLICADOS (merge A2:F2)                           │
│  "Filtros: Edificio: Planta Central | Tipo: Entrada"              │
│  Fuente 10pt, itálica, gris                                       │
│  (Si no hay filtros: "Sin filtros adicionales aplicados")          │
├─────────────────────────────────────────────────────────────────────┤
│  Fila 3: VACÍA (separador visual)                                  │
├─────────────────────────────────────────────────────────────────────┤
│  Fila 4: HEADERS DE COLUMNAS                                      │
│  DNI | Fecha | Hora | Tipo de Movimiento | Edificio | Observación  │
│  Negrita, fondo gris claro, bordes                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Fila 5+: DATOS                                                    │
│  12345678 | 01/04/2026 | 08:30 | Entrada | Planta Central | ...   │
│  12345678 | 01/04/2026 | 17:00 | Salida  | Planta Central | ...   │
│  Filas alternadas con fondo blanco / gris muy claro (zebra)        │
├─────────────────────────────────────────────────────────────────────┤
│  Última fila + 2: RESUMEN (merge A:F)                              │
│  "Total: 152 registros (89 entradas, 63 salidas)"                 │
│  Fuente 10pt, negrita                                              │
│  "Exportado el 02/04/2026 a las 14:35"                            │
└─────────────────────────────────────────────────────────────────────┘
```

#### Detalle de columnas de datos

| Columna | Header | Formato de celda | Ejemplo |
|---|---|---|---|
| A | `DNI` | Texto (para preservar ceros a la izquierda) | `12345678` |
| B | `Fecha` | Fecha `DD/MM/YYYY` | `01/04/2026` |
| C | `Hora` | Hora `HH:mm` | `08:30` |
| D | `Tipo de Movimiento` | Texto | `Entrada` / `Salida` |
| E | `Edificio` | Texto (vacío si no tiene) | `Planta Central` |
| F | `Observación` | Texto (vacío si no tiene) | `Llegó tarde por lluvia` |

**Los headers de columna (fila 4) son idénticos a los de la plantilla de importación**, de modo que si el usuario elimina las filas de encabezado y resumen, el archivo es directamente reimportable.

#### Formato visual

- **Ancho de columnas**: Auto-ajustado al contenido, con mínimos razonables (DNI: 12, Fecha: 14, Hora: 8, Tipo: 20, Edificio: 25, Observación: 40).
- **Bordes**: Bordes finos en toda la zona de datos (filas 4 en adelante).
- **Zebra**: Filas de datos alternan fondo blanco y `#F2F2F2`.
- **Encabezado de reporte** (fila 1): Fondo azul oscuro `#1F4E79`, texto blanco, fuente 14pt.
- **Filtros** (fila 2): Texto gris `#666666`, itálica.
- **Headers de columna** (fila 4): Fondo gris `#D9E2F3`, negrita, bordes.
- **Resumen** (última fila): Negrita, sin fondo, separado por una fila vacía.

### 6.4 Ordenamiento

Los datos se exportan ordenados por:
1. **Fecha** ascendente (los más antiguos primero)
2. **Hora** ascendente
3. **Nombre del empleado** ascendente (dentro de misma fecha/hora)

Este orden hace que la lectura del Excel sea cronológica y fácil de seguir.

### 6.5 Flujo de Exportación — Frontend

```
[Botón "Exportar Excel" en toolbar de tabla]
        │
        ▼
[Modal/Panel de exportación]
        │
        ├─ Muestra los filtros actuales de la tabla como valores precargados
        │   • Rango de fechas: Desde [01/MM/YYYY] hasta [hoy] (editables)
        │   • Rango horario: [00:00] a [23:59] (editables)
        │   • Empleado, DNI, Edificio, Tipo, Área: heredados de la tabla
        │   • El usuario puede ajustar cualquier filtro antes de exportar
        │
        ├─ Muestra preview: "Se exportarán aproximadamente X registros"
        │   (Consulta rápida de COUNT al backend para dar estimación)
        │
        └─ Botón "Descargar Excel"
            • Dispara GET /export con los filtros seleccionados
            • Muestra spinner durante la descarga
            • El navegador inicia la descarga del archivo automáticamente
            • Si supera 5000 registros: muestra error con sugerencia de filtrar más
```

**Valores por defecto inteligentes del panel de exportación:**

| Filtro | Valor por defecto | Lógica |
|---|---|---|
| Fecha desde | Día 1 del mes actual | Exportar el mes en curso es el caso más común |
| Fecha hasta | Hoy | No incluir días futuros |
| Hora desde | `00:00` | Capturar todo el día desde el inicio |
| Hora hasta | `23:59` | Capturar todo el día hasta el final |
| Tipo de movimiento | Todos | No restringir por defecto |
| Empleado / DNI | — (vacío) | Todos los empleados |
| Edificio | — (vacío) | Todos los edificios |
| Área | — (vacío) | Todas las áreas |

Si el usuario ya tiene filtros activos en la tabla, esos filtros se propagan como valores iniciales del panel de exportación (sobreescribiendo los defaults donde corresponda).

### 6.6 Clase Backend: `AttendanceExcelExporter`

```java
@Component
@RequiredArgsConstructor
public class AttendanceExcelExporter {

    // Genera el byte[] del .xlsx a partir de una lista de registros y los filtros aplicados
    public byte[] export(List<AttendanceRecordResponseDTO> records, 
                         AttendanceRecordFilterDTO appliedFilters) {
        // 1. Crear workbook con SXSSFWorkbook (streaming, eficiente en memoria)
        // 2. Crear hoja "Registros de Asistencia"
        // 3. Escribir encabezado del reporte con rango de fechas
        // 4. Escribir resumen de filtros aplicados
        // 5. Escribir headers de columna
        // 6. Iterar registros y escribir datos con formato zebra
        // 7. Escribir fila de resumen con totales
        // 8. Auto-ajustar anchos de columna
        // 9. Retornar como byte[]
    }

    // Construye el texto descriptivo de filtros aplicados
    private String buildFilterSummary(AttendanceRecordFilterDTO filters) { ... }
}
```

### 6.7 DTO adicional: `AttendanceExportFilterDTO`

Se reutiliza `AttendanceRecordFilterDTO` ya definido en la sección 3.3 — no hace falta un DTO nuevo. El controller mapea los query params al mismo FilterDTO.

---

## 7. Seguridad y Performance

### 7.1 Seguridad
- **Tamaño máximo del archivo**: 2MB (configurar en `application.properties`: `spring.servlet.multipart.max-file-size=2MB`).
- **Límite de filas**: 1000 registros por importación.
- **Validación server-side obligatoria**: Aunque el frontend haga dry-run, el backend SIEMPRE revalida en la persistencia real.
- **Permisos**: El endpoint de importación usa el mismo permiso `ATTENDANCE_RECORD_WRITE`.
- **Sanitización**: Los textos del Excel se sanitizan (trim, eliminar caracteres de control) antes de cualquier procesamiento.

### 7.2 Performance
- **Carga en memoria**: Apache POI streaming mode (`SXSSFWorkbook` para escribir, `XSSFWorkbook` para leer — archivos ≤1000 filas no necesitan streaming de lectura).
- **Precarga batch**: Todos los lookups (empleados, edificios, registros existentes) se hacen en 3 queries antes de iterar filas, no N+1.
- **Persistencia batch**: `saveAll()` en una sola transacción.
- **Exportación streaming**: Usa `SXSSFWorkbook` para no cargar todo el Excel en memoria. Límite de 5000 filas evita generar archivos excesivamente grandes.

---

## 8. Archivos a Crear/Modificar

### 8.1 Backend — Archivos NUEVOS

| Archivo | Ruta |
|---|---|
| `MovementType.java` | `model/enums/employee/MovementType.java` |
| `AttendanceRecord.java` | `model/entity/employee/AttendanceRecord.java` |
| `AttendanceRecordDTO.java` | `model/dto/employee/AttendanceRecordDTO.java` |
| `AttendanceRecordResponseDTO.java` | `model/dto/employee/AttendanceRecordResponseDTO.java` |
| `AttendanceRecordFilterDTO.java` | `model/dto/employee/AttendanceRecordFilterDTO.java` |
| `AttendanceRecordBatchDTO.java` | `model/dto/employee/AttendanceRecordBatchDTO.java` |
| `AttendanceImportResultDTO.java` | `model/dto/employee/AttendanceImportResultDTO.java` |
| `AttendanceImportRowResultDTO.java` | `model/dto/employee/AttendanceImportRowResultDTO.java` |
| `RowStatus.java` | `model/enums/employee/RowStatus.java` |
| `AttendanceRecordController.java` | `controller/AttendanceRecordController.java` |
| `IAttendanceRecordService.java` | `service/port/IAttendanceRecordService.java` |
| `AttendanceRecordService.java` | `service/implementation/AttendanceRecordService.java` |
| `AttendanceExcelImporter.java` | `service/importer/AttendanceExcelImporter.java` |
| `AttendanceExcelExporter.java` | `service/export/AttendanceExcelExporter.java` |
| `AttendanceRecordRepository.java` | `repository/AttendanceRecordRepository.java` |
| `AttendanceRecordNotFoundException.java` | `exception/attendanceRecord/AttendanceRecordNotFoundException.java` |
| `DuplicateAttendanceRecordException.java` | `exception/attendanceRecord/DuplicateAttendanceRecordException.java` |
| `AttendanceImportException.java` | `exception/attendanceRecord/AttendanceImportException.java` |
| Flyway migration `V*__create_attendance_records.sql` | `resources/db/migration/` |

### 8.2 Backend — Archivos a MODIFICAR

| Archivo | Cambio |
|---|---|
| `AppPermissions.java` | Agregar 3 constantes de permisos |
| `GlobalExceptionHandler.java` | Agregar handlers para las nuevas excepciones |
| `application.properties` | Agregar `spring.servlet.multipart.max-file-size=2MB` (si no existe) |
| `messages_es.properties` | Agregar mensajes de validación para attendance |

### 8.3 Frontend — Archivos NUEVOS

| Archivo | Ruta |
|---|---|
| `attendance-record.model.ts` | `shared/models/attendance-record.model.ts` |
| `attendance.service.ts` | `domains/human-resources/attendance/services/attendance.service.ts` |
| `attendance-form.service.ts` | `domains/human-resources/attendance/services/attendance-form.service.ts` |
| `attendance-import.service.ts` | `domains/human-resources/attendance/services/attendance-import.service.ts` |
| `attendance-table.ts/html/scss` | `domains/human-resources/attendance/attendance-table/` |
| `attendance-form.ts/html/scss` | `domains/human-resources/attendance/attendance-form/` |
| `attendance-detail.ts/html/scss` | `domains/human-resources/attendance/attendance-detail/` |
| `attendance-page.ts/html/scss` | `domains/human-resources/attendance/attendance-page/` |
| `attendance-import.ts/html/scss` | `domains/human-resources/attendance/attendance-import/` |
| `attendance-export.ts/html/scss` | `domains/human-resources/attendance/attendance-export/` |

### 8.4 Frontend — Archivos a MODIFICAR

| Archivo | Cambio |
|---|---|
| `human-resources.routes.ts` | Agregar ruta `/attendance` |
| `human-resources-page.ts` | Agregar tab de navegación "Asistencia" |

---

## 9. Orden de Implementación Sugerido

### Fase 1 — Backend CRUD Base
1. Migration SQL + Enum `MovementType`
2. Entidad `AttendanceRecord`
3. DTOs (record, response, filter, batch)
4. Repository con query de filtros
5. Service con CRUD + validaciones de negocio
6. Controller con todos los endpoints CRUD
7. Excepciones + mensajes i18n
8. Permisos en `AppPermissions`

### Fase 2 — Frontend CRUD Base
1. Modelo TypeScript + enum
2. Service HTTP
3. Form service
4. Tabla con filtros
5. Formulario batch
6. Detalle
7. Página contenedora
8. Ruta + tab de navegación

### Fase 3 — Backend Importación Excel
1. Endpoint `GET /import/template` (generación de plantilla)
2. Clase `AttendanceExcelImporter` (parseo + validación)
3. Endpoint `POST /import` con dry-run
4. DTOs de resultado de importación
5. Tests

### Fase 4 — Frontend Importación Excel
1. Import service (upload, download template)
2. Componente de importación (modal con steps)
3. Tabla de previsualización con colores y mensajes
4. Integración con la página principal (botón en toolbar)

### Fase 5 — Backend Exportación Excel
1. Clase `AttendanceExcelExporter` (generación con formato profesional)
2. Endpoint `GET /export` con filtros y defaults inteligentes
3. Query de exportación en repository (sin paginación, con límite de 5000)

### Fase 6 — Frontend Exportación Excel
1. Panel/modal de exportación con filtros precargados desde la tabla
2. Lógica de defaults inteligentes (mes actual, 00:00-23:59)
3. Consulta de COUNT previa para mostrar estimación
4. Descarga del archivo vía blob download
5. Integración con toolbar (botón "Exportar Excel")

---

## 10. Integración Biométrica — Webhooks con Adaptadores Genéricos

### 10.1 Arquitectura

El sistema recibe eventos push de relojes biométricos a través de un endpoint público (sin autenticación JWT). La resolución del tenant se hace mediante un **token de webhook** único por empresa, incluido en la URL.

```
Reloj Hikvision                    CivilControl
      │                                 │
      │  POST /api/webhook/biometric    │
      │  /{webhookToken}/{brand}        │
      │  Body: JSON del evento          │
      ├────────────────────────────────→│
      │                                 ├─ 1. Resolver tenant por webhookToken
      │                                 ├─ 2. Buscar parser por brand ("hikvision")
      │                                 ├─ 3. Parsear payload → (DNI, timestamp)
      │                                 ├─ 4. Buscar empleado por DNI
      │                                 ├─ 5. Determinar tipo: ENTRADA o SALIDA (toggle)
      │                                 ├─ 6. Crear AttendanceRecord
      │  HTTP 200 OK                    │
      │←────────────────────────────────┤
```

> **Principio de diseño:** Siempre retorna HTTP 200 para evitar reintentos del dispositivo, incluso si el payload es inválido o el empleado no existe.

### 10.2 Strategy Pattern — Sistema de Adaptadores

#### Interface: `BiometricPayloadParser`

```java
public interface BiometricPayloadParser {
    boolean supports(String brand);
    BiometricParsedEvent parse(String rawPayload);
}
```

**Archivo:** `service/webhook/BiometricPayloadParser.java`

- `supports(brand)` — Determina si este parser maneja la marca indicada.
- `parse(rawPayload)` — Retorna el evento parseado, `null` para eventos de sistema (ignorar), o lanza `BiometricParseException` para payloads malformados.

#### Record: `BiometricParsedEvent`

```java
public record BiometricParsedEvent(String employeeDni, LocalDateTime timestamp) {}
```

**Archivo:** `service/webhook/BiometricParsedEvent.java`

#### Implementación: `HikvisionPayloadParser`

```java
@Component
public class HikvisionPayloadParser implements BiometricPayloadParser {
    
    @Override
    public boolean supports(String brand) {
        return "hikvision".equalsIgnoreCase(brand);
    }
    
    @Override
    public BiometricParsedEvent parse(String rawPayload) {
        // Parsea JSON con formato ISAPI:
        // - AccessControllerEvent.employeeNoString → DNI
        // - root dateTime (ISO 8601 con offset) → timestamp
        // Retorna null si no tiene employeeNoString (evento de sistema)
    }
}
```

**Archivo:** `service/webhook/HikvisionPayloadParser.java`

> **Extensibilidad:** Para soportar una nueva marca de reloj, basta crear un nuevo `@Component` que implemente `BiometricPayloadParser` con su propio `supports("nueva-marca")` y lógica de parseo. No se modifica ningún código existente.

### 10.3 Dead-letter Queue — Eventos Huérfanos

#### Entidad: `OrphanBiometricLog`

Almacena payloads que no pudieron procesarse (DNI no encontrado, formato inválido, etc.) para auditoría.

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `id` | `Long` (PK) | No | Identificador |
| `clockBrand` | `String(50)` | No | Marca del dispositivo |
| `rawPayload` | `TEXT` | No | Payload completo recibido |
| `errorMessage` | `TEXT` | Sí | Mensaje de error descriptivo |
| `receivedAt` | `LocalDateTime` | No | Timestamp de recepción |

**Archivo:** `model/entity/OrphanBiometricLog.java`

> **Nota:** Esta entidad NO extiende `TenantEntity` ya que puede contener payloads cuyo tenant no se pudo resolver.

### 10.4 Servicio: `BiometricWebhookService`

**Archivo:** `service/implementation/BiometricWebhookService.java`

Implementa `IBiometricWebhookService`. Flujo del método `process()`:

1. **Resolver tenant** por `webhookToken` via `TenantRepository.findByWebhookTokenAndDeletedFalse()`.
2. **Buscar parser** — Itera los `BiometricPayloadParser` inyectados y selecciona el que `supports(brand)`.
3. **Parsear** — Invoca `parser.parse(rawPayload)`. Si retorna `null`, ignora (evento de sistema).
4. **Buscar empleado** por DNI dentro del tenant.
5. **Determinar MovementType** — Toggle: la última entrada/salida del empleado ese día determina si el nuevo evento es ENTRADA o SALIDA.
6. **Crear `AttendanceRecord`** — Persiste el registro.
7. **Idempotencia** — Captura `DataIntegrityViolationException` silenciosamente (unique constraint impide duplicados).
8. **Error handling** — Cualquier fallo guarda un `OrphanBiometricLog` para auditoría.

### 10.5 Controller: `BiometricWebhookController`

**Archivo:** `controller/BiometricWebhookController.java`

| Método | Ruta | Auth |
|---|---|---|
| `POST` / `PUT` | `/api/webhook/biometric/{webhookToken}/{brand}` | **Público** (sin JWT) |

- Acepta body como JSON raw o form param `event_log` (compatibilidad con dispositivos que envían formularios).
- **Siempre retorna HTTP 200** para evitar reintentos del dispositivo.
- Configurado como endpoint público en `SecurityConfig.java`:

```java
.requestMatchers("/api/webhook/**").permitAll()
```

### 10.6 Integración con Tenant

La entidad `Tenant` fue ampliada con un campo para el token de webhook:

| Campo | Tipo | Descripción |
|---|---|---|
| `webhookToken` | `UUID` (UNIQUE, NOT NULL) | Token único de webhook generado automáticamente al crear el tenant |

**Resolución:** `TenantRepository.findByWebhookTokenAndDeletedFalse(webhookToken)` mapea el token de la URL al tenant correspondiente.

### 10.7 Frontend — Configuración de Webhook

**Archivo:** `pages/company/company-settings-page/company-settings-page.ts`

En la página de configuración de la empresa se muestra:

- La **URL completa del webhook** lista para copiar:
  ```
  {baseUrl}/api/webhook/biometric/{webhookToken}/hikvision
  ```
- Botón **"Copiar URL"** que copia al portapapeles.
- **Instrucciones** para configurar el reloj Hikvision:
  - Ir a "Configuración → Red → HTTP Listening"
  - Pegar la URL
  - Activar el envío de eventos

### 10.8 Migraciones

| Migración | Descripción |
|---|---|
| `V46__create_biometric_webhook_tables.sql` | Crea tabla `orphan_biometric_logs` |
| `V47__add_webhook_token_to_tenants.sql` | Agrega columna `webhook_token` (UUID, NOT NULL, UNIQUE) a `tenants` con valor default random |

### 10.9 Permisos

No se agregan permisos nuevos para el webhook — el endpoint es público. Los registros de asistencia creados automáticamente son accesibles con los mismos permisos `ATTENDANCE_RECORD_*` del CRUD manual.

---
