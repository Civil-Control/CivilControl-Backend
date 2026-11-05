# Feature 8 — Mejoras en Clientes y Proveedores: Alias y Contactos Múltiples

## Contexto

Los módulos de Clientes y Proveedores originalmente almacenaban un único contacto embebido (email + teléfono) por entidad. En la práctica, una empresa proveedora o un comitente tiene múltiples personas de contacto (compras, administración, facturación, etc.).

Adicionalmente, los proveedores frecuentemente se conocen por un nombre informal ("El Corralón de Juan") distinto de su razón social ("Corralón del Norte SRL"). Era necesario agregar un campo de alias.

Esta feature introduce:

1. **Alias en proveedores** — Campo `alias` (50 chars) opcional, buscable y filtrable.
2. **Contactos múltiples** — Migración de contacto embebido a relación OneToMany con `ContactInfo`, tanto en `Supplier` como en `Client`. Cada contacto tiene un `referenceName` y listas de emails y teléfonos.

---

## 1. Modelo de Datos

### 1.1 Entidad: `Supplier` — Campos modificados

| Campo | Tipo | Cambio | Descripción |
|---|---|---|---|
| `alias` | `String(50)` | **NUEVO** — nullable | Nombre informal/alias del proveedor |
| `contacts` | `List<ContactInfo>` (OneToMany) | **MODIFICADO** — era contacto embebido | Lista de contactos con cascade ALL y orphanRemoval |

### 1.2 Entidad: `Client` — Campos modificados

| Campo | Tipo | Cambio | Descripción |
|---|---|---|---|
| `contacts` | `List<ContactInfo>` (OneToMany) | **MODIFICADO** — era contacto embebido | Lista de contactos (mappedBy="client"), cascade ALL, orphanRemoval |

### 1.3 Entidad: `ContactInfo`

Entidad independiente que extiende `TenantEntity`. Permite múltiples contactos por proveedor o cliente.

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `id` | `Long` (PK, auto) | No | Identificador |
| `referenceName` | `String(100)` | No | Nombre de referencia del contacto (ej. "Administración", "Ventas") |
| `email` | `List<String>` (ElementCollection) | Sí | Lista de emails. Tabla: `contact_info_emails` |
| `phoneNumber` | `List<String>` (ElementCollection) | Sí | Lista de teléfonos. Tabla: `contact_info_phones` |
| `client` | FK → `Client` (ManyToOne) | Sí | Referencia al cliente (bidireccional) |
| `supplier` | FK → `Supplier` (ManyToOne) | Sí | Referencia al proveedor (bidireccional) |

> **Invariante:** Un ContactInfo siempre pertenece a exactamente un `Client` O un `Supplier` (nunca a ambos, nunca a ninguno).

---

## 2. Backend — Capas

### 2.1 DTOs de Contacto

#### `ContactInfoDTO` (record, CREATE/UPDATE)

```java
public record ContactInfoDTO(
    @NotBlank @Size(max = 100) String referenceName,
    List<@Email String> email,
    List<@Pattern(regexp = "...") String> phoneNumber
) {}
```

#### `ContactInfoResponseDTO` (record)

```java
public record ContactInfoResponseDTO(
    Long id,
    String referenceName,
    List<String> email,
    List<String> phoneNumber
) {}
```

### 2.2 DTOs de Proveedor (campos modificados)

#### `SupplierDTO` — Campos agregados

```java
public record SupplierDTO(
    // ... campos existentes ...
    @Size(max = 50) String alias,                // NUEVO
    @Valid List<ContactInfoDTO> contacts          // MODIFICADO (era contacto único)
) {}
```

#### `SupplierResponseDTO` — Campos agregados

```java
public record SupplierResponseDTO(
    // ... campos existentes ...
    String alias,                                 // NUEVO
    List<ContactInfoResponseDTO> contacts         // MODIFICADO
) {}
```

#### `SupplierFilterDTO` — Campos agregados

```java
public record SupplierFilterDTO(
    // ... filtros existentes ...
    String alias,                                 // NUEVO — filtro por alias
    // search también busca en alias
) {}
```

### 2.3 Repository: `SupplierRepository`

El filtro JPQL `findAllWithFilters` fue ampliado para incluir `alias`:

```sql
AND (:alias IS NULL OR LOWER(CAST(s.alias AS string)) LIKE LOWER(CONCAT('%', :alias, '%')))
```

El campo `search` también busca en alias:

```sql
OR LOWER(CAST(s.alias AS string)) LIKE LOWER(CONCAT('%', :search, '%'))
```

### 2.4 Controller: `SupplierController`

El endpoint `GET /api/v1/suppliers` acepta el query parameter `alias` para filtrar.

### 2.5 Cascada y orphanRemoval

Al actualizar un proveedor o cliente mediante PATCH:
- Los contactos que ya no aparecen en la lista son **eliminados automáticamente** (orphanRemoval).
- Los contactos nuevos son **creados automáticamente** (cascade PERSIST).
- Los contactos existentes (con `id`) son **actualizados** (cascade MERGE).

---

## 3. Frontend

### 3.1 Modelos

**Archivo:** `shared/models/supplier.model.ts`

```typescript
export interface ContactInfo {
    id?: number;
    referenceName: string;
    email: string[] | null;
    phoneNumber: string[] | null;
}

export interface Supplier {
    // ... campos existentes ...
    alias?: string;
    contacts?: ContactInfo[];
}

export interface SupplierFilters {
    // ... filtros existentes ...
    alias?: string;
}
```

**Archivo:** `shared/models/client.model.ts`

```typescript
export interface Client {
    // ... campos existentes ...
    contacts?: ContactInfo[];  // importado desde supplier.model
}
```

> **Nota:** `ContactInfo` y `Address` están definidas en `supplier.model.ts` y son reutilizadas por los modelos de Client y Building.

### 3.2 Formularios

Los formularios de proveedor y cliente fueron actualizados para:
- Mostrar el campo **Alias** (input de texto, max 50 chars) en el formulario de proveedor.
- Reemplazar los campos de contacto único por una **lista dinámica de contactos**, donde cada contacto tiene: nombre de referencia, lista de emails y lista de teléfonos.
- Botón **"Agregar contacto"** para añadir entradas a la lista.
- Botón **"Eliminar"** en cada contacto para removerlo.

---

## 4. Permisos

No se agregan permisos nuevos. Reutiliza los existentes:

| Constante | Uso |
|---|---|
| `SUPPLIER_READ` | Ver proveedores (incluye alias y contactos) |
| `SUPPLIER_WRITE` | Crear/editar proveedores (incluye alias y contactos) |
| `CLIENT_READ` | Ver clientes (incluye contactos) |
| `CLIENT_WRITE` | Crear/editar clientes (incluye contactos) |

---

## 5. Migraciones

### V37 — Contactos a lista con nombre de referencia

```sql
-- V37__contact_info_to_list_with_reference_name.sql

-- 1. Agrega columnas nuevas a contact_info
ALTER TABLE contact_info ADD COLUMN reference_name VARCHAR(100);
ALTER TABLE contact_info ADD COLUMN client_id BIGINT REFERENCES clients(id);
ALTER TABLE contact_info ADD COLUMN supplier_id BIGINT REFERENCES suppliers(id);

-- 2. Migra datos existentes: vincula cada contacto a su proveedor/cliente
UPDATE contact_info ci SET supplier_id = s.id
FROM suppliers s WHERE s.contact_info_id = ci.id;

UPDATE contact_info ci SET client_id = c.id
FROM clients c WHERE c.contact_info_id = ci.id;

-- 3. Asigna nombre de referencia por defecto a registros existentes
UPDATE contact_info SET reference_name = 'Contacto principal'
WHERE reference_name IS NULL;

ALTER TABLE contact_info ALTER COLUMN reference_name SET NOT NULL;

-- 4. Elimina FKs antiguas de clients y suppliers
ALTER TABLE clients DROP COLUMN contact_info_id;
ALTER TABLE suppliers DROP COLUMN contact_info_id;
```

### V39 — Alias en proveedores

```sql
-- V39__add_alias_to_suppliers.sql
ALTER TABLE suppliers ADD COLUMN alias VARCHAR(50);
```

---

## 6. Internacionalización

**Archivo:** `messages_es.properties`

```properties
supplier.alias.size=El alias no puede exceder 50 caracteres
```

---
