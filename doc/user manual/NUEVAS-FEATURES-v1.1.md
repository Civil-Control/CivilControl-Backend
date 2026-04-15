● **Versión:** 1.1.0
● **Fecha:** 21/03/2026

---

### 1. Órdenes de Reparación (Flujo de Trabajo en Taller)

**Problema Actual**

El operario que detecta una falla en una máquina o vehículo no tiene un medio para notificar al taller a través del sistema, y el personal de taller no tiene un lugar donde ver los trabajos pendientes.

**Mejora Implementada**

El sistema contará con una nueva sección llamada "Órdenes de Reparación" para establecer un canal formal y con trazabilidad completa entre el operario de campo y el taller.

**Flujo de Trabajo**

1. **Reporte de Falla (Operario):** El operario carga una orden indicando el vehículo que falló, la fecha y una breve descripción. La orden se registra automáticamente como pendiente.
2. **Gestión de la Orden (Personal de Taller):** El personal de taller accede al listado de órdenes pendientes y puede:
   - Marcar una orden como **"en proceso"** al iniciar la reparación.
   - Marcar una orden como **"finalizada"** al terminar el trabajo.

**Registro de Finalización**

Cuando una orden se marca como "finalizada", el sistema solicita completar los datos técnicos de la reparación, incluyendo:

- Tipo de reparación.
- Quién la realizó.
- Costo.
- Si intervino algún proveedor externo.

**Trazabilidad**

La reparación finalizada se añade al historial del vehículo, registrando también quién reportó la falla originalmente y la descripción de la misma.

---

### 2. Vinculación de Comprobantes a Gastos

**Problema Actual**

Los registros de gastos (carga de combustible, reparaciones, pagos de sueldo, compra de stock) se guardan separados de sus respectivas facturas o comprobantes de respaldo, sin una relación directa en el sistema.

**Mejora Implementada**

Cada factura o comprobante puede vincularse a los gastos que originó, estableciendo esta relación desde dos puntos:

- **Desde el Gasto hacia el Comprobante:** Al cargar un gasto, se habilita un nuevo campo para buscar y seleccionar el comprobante de respaldo (búsqueda por número de comprobante o nombre del proveedor).
- **Desde el Comprobante hacia el Gasto:** El detalle de la factura incluye una nueva sección que muestra todos los gastos ya asociados, organizados por tipo (cargas de combustible, reparaciones, pagos de haberes y compras de materiales).
   - Se puede crear un nuevo registro de gasto directamente desde esta sección, con la factura ya asociada automáticamente.

**Resultado**

El sistema permite ver, al consultar una factura, a qué gastos corresponde; o al consultar un gasto, qué comprobante lo respalda.

---

### 3. Facturación de Ventas y Gestión de Clientes

**Objetivo**

Agregar la capacidad de registrar lo que la empresa factura y cobra a sus clientes, complementando el registro existente de compras y pagos.

**Funcionalidades Principales**

1. **Gestión de Clientes:** Se añade una lista de clientes (similar a la de proveedores).
   - *Datos obligatorios:* CUIT, razón social, nombre comercial, condición frente al IVA (Responsable Inscripto, Monotributista, Exento o Consumidor Final), domicilio y datos de contacto.
2. **Emisión de Comprobantes de Venta:** Permite cargar los siguientes comprobantes por cada cliente:
   - Facturas A, B y C.
   - Notas de Débito y Crédito (variantes A, B y C).
   - *Detalle del Comprobante:* Incluye punto de venta, número, fecha, ítems facturados, totales desglosados, área o sector y un campo opcional para el número de pedido del cliente.
3. **Gestión de Cobro:** Marcar comprobantes como **"Cobrado"** o **"Pendiente"** con un solo click.
4. **Visualización del Cliente:** El detalle del cliente muestra tres números clave:
   - Total Facturado.
   - Total Cobrado.
   - Saldo Pendiente.

**Experiencia en Pantalla**

- Nueva sección **"Clientes"** en el menú principal.
- Listado de clientes con filtros disponibles.
- La ficha del cliente muestra sus datos y, debajo, todos sus comprobantes de venta.
- Se puede crear un nuevo comprobante de venta directamente desde esta pantalla con el cliente ya precargado.

---

### 4. Certificaciones de Obra y Seguimiento de Facturación por Contrato

**Objetivo**

Controlar las finanzas de cada contrato u orden de trabajo y seguir en tiempo real el monto total acordado.

**Seguimiento de Métricas Clave**

El sistema monitorea en tiempo real:

- **Cuánto se certificó:** El trabajo medido y presentado.
- **Cuánto de eso ya se facturó:** Los certificados con comprobante de venta emitido.
- **Cuánto ya se cobró:** Lo que ingresó efectivamente.

**Funcionalidades Principales**

1. **Registro de Contratos u Órdenes de Trabajo:**
   - *Datos:* Número, cliente, área o proyecto, descripción, fecha de firma, monto total contratado y moneda.
   - *Opción:* La fecha de vencimiento es opcional; si se carga y se incumple, el sistema lo marca en rojo automáticamente.
2. **Registro de Certificados de Avance:** Se añaden por contrato.
   - *Datos:* Número automático, fecha, monto certificado y estado.
   - *Estados Libres:* Presentado, Aprobado, Facturado o Cobrado (el usuario puede cambiar el estado en cualquier momento).
3. **Vinculación con Factura de Venta:** Permite vincular el certificado con el comprobante de venta emitido (opcional).

**Experiencia en Pantalla (Detalle del Contrato)**

- **Zona Superior — Panel Financiero:** Muestra tres barras de progreso apiladas (Certificado, Facturado, Cobrado) para una visión rápida.
   - *Ejemplo:* Se puede ver de un vistazo si hay trabajo certificado no facturado o facturado no cobrado.
- **Zona Inferior — Tabla de Certificaciones:** Lista los certificados con su monto, número de comprobante vinculado y estado codificado por colores.
   - Permite cambiar el estado o agregar un nuevo certificado desde la misma tabla.

**Beneficio**

El diseño resuelve el problema de no saber cuánto de lo ejecutado está cobrado, facturado pendiente o sin facturar, haciendo esta información disponible en 2 segundos.