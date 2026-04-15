● **Versión:** 1.2.0
● **Fecha:** 14/04/2026

---

### 5. Órdenes de Reparación — Flujo de Trabajo en Taller

**Problema Actual**

El sistema registra reparaciones de vehículos con información técnica y de costo, pero no contempla un flujo que diferencie una rotura reportada de una reparación ya ejecutada. Cuando un operario de campo detecta una falla mecánica, la comunicación con el taller se realiza de forma verbal o mediante anotaciones en papel. Esto provoca que los trabajos pendientes se pierdan, no exista trazabilidad de quién reportó cada falla, y el personal de taller no cuente con un tablero actualizado de trabajos por ejecutar.

**Mejora Implementada**

El sistema incorporará un módulo de órdenes de reparación que conectará al operario de campo con el personal de taller mediante un flujo de trabajo de tres estados. El operario podrá reportar fallas desde cualquier dispositivo, y el personal de taller las visualizará como trabajos pendientes que podrá iniciar y completar, generando automáticamente el registro de reparación al finalizar.

**Flujo de Trabajo**

1. **Reporte de Falla (Operario de Campo):** El operario detecta un problema en un vehículo y crea una orden de reparación indicando el vehículo afectado, la fecha y una descripción de la falla observada. Opcionalmente puede registrar su nombre como informante. La orden ingresa al sistema en estado "Pendiente".

2. **Inicio de Reparación (Personal de Taller):** El encargado de taller accede al listado de órdenes pendientes y, al asignar recursos para atender un trabajo, cambia su estado a "En Proceso". Esto indica que la reparación ya está siendo ejecutada.

3. **Finalización y Registro (Personal de Taller):** Una vez completado el trabajo, el técnico finaliza la orden completando los datos de la reparación: costo, descripción del trabajo realizado, materiales utilizados y proveedor si la reparación fue externa. Al confirmar, el sistema creará automáticamente el registro de reparación vinculado a la orden original, y esta pasará al estado "Completada".

**Experiencia en Pantalla**

- **Operario de Campo:** Accede a un formulario simplificado para reportar fallas. Puede ver únicamente sus propias órdenes pendientes, editarlas o eliminarlas mientras estén en estado "Pendiente". No tiene visibilidad sobre órdenes de otros usuarios ni sobre reparaciones completadas.
- **Personal de Taller:** Dispone de un tablero con todas las órdenes del tenant, filtrable por estado, vehículo y fecha. Desde el listado puede iniciar órdenes pendientes y completar órdenes en proceso, accediendo al formulario de cierre que solicita los datos técnicos y económicos de la reparación.
- **Reparaciones:** Al consultar una reparación creada desde una orden, se mostrará la referencia a la orden original, permitiendo trazar el origen de cada intervención.

**Beneficio**

Se formaliza la comunicación entre campo y taller, eliminando la pérdida de reportes de fallas. El taller obtiene un panel de trabajos pendientes que facilita la planificación de mano de obra y materiales, y la empresa gana trazabilidad completa desde el momento en que se detecta una falla hasta que se registra la reparación con sus costos.

---

### 6. Vinculación de Comprobantes de Compra a Registros del Sistema

**Problema Actual**

El sistema gestiona comprobantes de compra (facturas de proveedor) y registros de gastos (cargas de combustible, reparaciones, pagos de haberes y compras de stock) de forma independiente. No existe un mecanismo para vincular un comprobante al gasto que lo originó. Cuando se necesita verificar qué factura respalda una carga de combustible o una reparación, el usuario debe buscar manualmente en el módulo de comprobantes y cruzar los datos de forma visual.

**Mejora Implementada**

El sistema permitirá vincular opcionalmente un comprobante de compra a cualquier registro de carga de combustible, reparación, pago de haberes o compra de stock. La vinculación funcionará en ambos sentidos: desde el registro hacia el comprobante, y desde el comprobante hacia los registros que lo referencian.

**Funcionalidades Principales**

1. **Vinculación desde el Registro:** Al crear o editar una carga de combustible, reparación, pago de haberes o compra de stock, se habilitará un campo de búsqueda para seleccionar el comprobante de compra que lo respalda. La selección es siempre opcional: el usuario puede dejar el campo vacío si no aplica o si el comprobante aún no fue cargado.

2. **Vinculación desde el Comprobante:** Al visualizar el detalle de un comprobante de compra, se mostrarán todos los registros que ya están vinculados a él, organizados por tipo (combustible, reparaciones, salarios, stock). Desde esta misma pantalla se podrá crear un nuevo registro con el comprobante pre-seleccionado, agilizando la carga cruzada.

3. **Resumen en las Vistas de Detalle:** En la ficha de cada registro vinculado se mostrará un resumen del comprobante asociado (tipo, número, proveedor, monto y fecha), permitiendo identificar el respaldo documental sin navegar a otra pantalla.

**Módulos Alcanzados**

| Módulo | Alcance |
|---|---|
| Cargas de Combustible | Formulario de creación/edición y detalle |
| Reparaciones | Formulario de creación/edición y detalle |
| Pagos de Haberes | Formulario de creación/edición y detalle |
| Compras de Stock | Formulario de creación/edición y detalle |
| Comprobantes de Compra | Vista de detalle con registros vinculados |

**Experiencia en Pantalla**

- **Formularios:** Al hacer clic en el campo de comprobante se desplegará un buscador con filtro por tipo de documento, número y proveedor. El comprobante seleccionado se mostrará como una tarjeta resumen con los datos principales.
- **Detalle de Registro:** El comprobante vinculado se presentará como una sección adicional en la ficha del registro, con un enlace para navegar directamente al detalle del comprobante.
- **Detalle de Comprobante:** Se añadirá una sección que lista todos los registros vinculados, agrupados por categoría, con accesos directos para ver cada registro o crear nuevos vínculos.

**Beneficio**

Se cierra la brecha entre los comprobantes de compra y los gastos operativos, permitiendo auditar en segundos qué factura respalda cada gasto. La vinculación bidireccional elimina la necesidad de cruzar datos manualmente y facilita la conciliación contable al tener toda la información enlazada dentro del propio sistema.

---

### 7. Facturación de Ventas y Gestión de Clientes

**Problema Actual**

La empresa emite comprobantes de venta a sus clientes (comitentes, municipios, organismos y empresas privadas), pero el sistema solo gestiona el circuito de compras. La facturación de ventas se lleva de forma externa, lo que impide tener una visión integral del flujo financiero: no se puede consultar desde el sistema cuánto se facturó a un cliente, cuánto se cobró ni cuánto está pendiente de cobro.

**Mejora Implementada**

El sistema incorporará un módulo completo de gestión de clientes y facturación de ventas. Se podrán registrar clientes con su condición ante el IVA, emitir comprobantes de venta (facturas y notas de crédito/débito de tipo A, B y C según la normativa argentina), y hacer seguimiento del estado de cobro de cada comprobante.

**Funcionalidades Principales**

1. **Gestión de Clientes:** Se dispondrá de un módulo dedicado para registrar clientes con sus datos fiscales (CUIT, razón social, nombre comercial, condición ante el IVA), datos de contacto y dirección. Cada cliente tendrá una ficha de detalle con el historial de facturación y las métricas financieras: total facturado, total cobrado y total pendiente de cobro.

2. **Comprobantes de Venta:** Se podrán registrar facturas y notas de crédito/débito de tipo A (para Responsables Inscriptos), tipo B (para Consumidores Finales y Exentos) y tipo C (para Monotributistas). Cada comprobante incluirá punto de venta, número, fecha de emisión, detalle de ítems con cantidades y porcentaje de IVA, descuentos y totales discriminados (neto, IVA, exento, otros impuestos).

3. **Seguimiento de Cobro:** Cada comprobante de venta podrá marcarse como cobrado o pendiente. La ficha del cliente mostrará los totales actualizados en tiempo real, permitiendo conocer de un vistazo la deuda de cada cliente.

4. **Asociación a Sectores:** Los comprobantes de venta podrán asociarse opcionalmente a un sector o área de proyecto, permitiendo analizar los ingresos por área de la misma forma en que se analizan los egresos.

**Experiencia en Pantalla**

- **Listado de Clientes:** Tabla con filtros por CUIT, razón social, condición IVA y estado (activo/inactivo). Desde el listado se accede al formulario de creación y al detalle de cada cliente.
- **Detalle del Cliente:** Ficha con los datos del cliente, tarjetas de resumen financiero (total facturado, cobrado y pendiente) y tabla de comprobantes emitidos con filtros por tipo, fecha, monto y estado de cobro.
- **Comprobantes de Venta:** Formulario con carga de ítems (producto, cantidad, precio unitario, porcentaje de IVA), cálculo automático de totales y validación de formato AFIP para punto de venta y número de comprobante.

**Beneficio**

Se unifica la gestión de compras y ventas dentro del mismo sistema, proporcionando una visión completa del flujo financiero de la empresa. El seguimiento de cobro por cliente elimina la dependencia de planillas externas y permite identificar rápidamente las cuentas por cobrar pendientes.

---

### 8. Certificaciones de Obra y Seguimiento de Facturación por Contrato

**Problema Actual**

En empresas constructoras, la relación financiera con un comitente se estructura a través de contratos u órdenes de trabajo que fijan un monto total. A medida que avanza la obra se emiten certificados de avance, y cada certificado puede vincularse a una factura de venta. Actualmente no existe en el sistema un mecanismo para registrar contratos, emitir certificaciones ni hacer seguimiento de cuánto se certificó, facturó y cobró respecto del total contratado. Esta información se gestiona manualmente, lo que dificulta responder tres preguntas fundamentales: ¿cuánto de lo contratado ya se certificó?, ¿cuánto de lo certificado ya se facturó? y ¿cuánto de lo facturado ya se cobró?

**Mejora Implementada**

El sistema incorporará un módulo de contratos de obra y certificaciones que permitirá registrar cada contrato con su monto total, emitir certificados de avance vinculados a ese contrato, asociar opcionalmente cada certificación a un comprobante de venta, y visualizar en tiempo real el estado de avance financiero en tres niveles: certificación, facturación y cobro.

**Funcionalidades Principales**

1. **Contratos de Obra:** Se podrán registrar contratos con número, cliente, monto contratado, moneda (pesos o dólares), fecha de inicio, fecha de vencimiento opcional, sector asignado y estado (Activo, Suspendido, Finalizado). Cada contrato tendrá una ficha de detalle con la tabla de certificaciones y las barras de progreso financiero.

2. **Certificaciones de Avance:** Dentro de cada contrato se podrán crear certificados numerados automáticamente. Cada certificación registrará la fecha, el monto certificado, un estado de gestión (Presentado, Aprobado, Facturado, Cobrado) y opcionalmente el comprobante de venta que la respalda. Los estados no tendrán restricciones de transición: el operador podrá asignar cualquier estado en cualquier momento, reflejando la realidad del trabajo en obra donde la secuencia administrativa no siempre sigue el orden formal.

3. **Panel de Seguimiento Financiero:** La ficha de cada contrato presentará tres indicadores de progreso calculados en tiempo real:
   - **Certificación:** Porcentaje del monto contratado que ya fue certificado, con detalle de cuánto falta por certificar.
   - **Facturación:** Porcentaje del monto certificado que ya tiene comprobante de venta asociado, con detalle de cuánto falta por facturar.
   - **Cobro:** Porcentaje del monto facturado que ya fue efectivamente cobrado, con detalle de cuánto está pendiente de cobro.

4. **Vinculación con Comprobantes de Venta:** Al crear o editar una certificación, el usuario podrá buscar y seleccionar un comprobante de venta del mismo cliente. El sistema validará que el comprobante pertenezca al cliente del contrato para evitar inconsistencias.

**Experiencia en Pantalla**

- **Listado de Contratos:** Tabla con filtros por cliente, número de contrato, sector, estado, moneda y rango de montos. Las columnas mostrarán el porcentaje de certificación a modo de indicador rápido.
- **Detalle del Contrato:** Ficha completa con los datos del contrato, tres barras de progreso con colores diferenciados (certificación, facturación, cobro), montos desglosados y tabla de certificaciones con filtros por estado y fecha.
- **Tabla de Certificaciones:** Cada fila mostrará el número de certificación, fecha, monto, estado (con etiqueta de color) y comprobante vinculado. El estado y el comprobante serán editables directamente desde la tabla sin necesidad de abrir un formulario.

**Beneficio**

Se obtiene visibilidad inmediata sobre el avance financiero de cada contrato, respondiendo las tres preguntas clave del negocio sin salir del sistema. La numeración automática de certificaciones elimina errores de secuencia, la vinculación con comprobantes de venta cierra el circuito documental, y la flexibilidad en los estados de gestión respeta los flujos reales de trabajo en obra donde los procesos administrativos no siempre siguen un orden lineal.

---

### 9. Registro de Entradas y Salidas de Empleados

**Problema Actual**

No existe un módulo para registrar la asistencia de los empleados. El control de entradas y salidas se realiza en planillas de papel o en archivos de Excel externos, lo que dificulta la consulta histórica, impide filtrar por empleado, obra o período, y no permite integrar esta información con el resto del sistema.

**Mejora Implementada**

El sistema incorporará un módulo completo de registro de asistencia que permitirá registrar movimientos de entrada y salida de cada empleado, con fecha, hora, tipo de movimiento, edificio y observaciones. El módulo soportará carga individual, carga masiva por lotes, importación desde archivos Excel y exportación de registros filtrados.

**Funcionalidades Principales**

1. **Registro de Movimientos:** Cada registro representará un único movimiento (una entrada o una salida) de un empleado en una fecha y hora determinada. Opcionalmente se podrá indicar el edificio donde se produjo el movimiento y agregar una observación. El sistema validará que no existan registros duplicados (mismo empleado, fecha, hora y tipo de movimiento) y que la fecha no sea futura.

2. **Carga Masiva por Lotes:** Además de la carga individual, se podrán registrar hasta 100 movimientos simultáneamente desde un formulario de carga por lotes, agilizando la tarea cuando se deben cargar múltiples registros de un mismo día.

3. **Importación desde Excel:** El usuario podrá descargar una plantilla Excel predefinida, completarla con los datos de asistencia y subirla al sistema. El importador validará exhaustivamente cada fila antes de persistir los datos:
   - Verificará que el DNI del empleado exista en el sistema y que no esté dado de baja.
   - Detectará registros duplicados (misma combinación de empleado, fecha, hora y tipo).
   - Validará que las fechas no sean futuras y que el formato de hora sea correcto.
   - Alertará sobre inconsistencias lógicas, como una salida sin entrada previa el mismo día, sin bloquear la importación.
   - Retornará un informe detallado fila por fila indicando éxitos, errores y advertencias.

4. **Exportación a Excel:** Los registros filtrados podrán descargarse en un archivo Excel con formato profesional, compatible con la plantilla de importación. Esto permite la ida y vuelta: exportar datos, corregir en planilla y volver a importar.

5. **Filtros Avanzados:** El listado de asistencia ofrecerá filtros por empleado (nombre, apellido, DNI), tipo de movimiento (entrada/salida), edificio, sector, rango de fechas, rango de horarios y búsqueda general.

**Experiencia en Pantalla**

- **Listado de Asistencia:** Tabla paginada con todas las columnas relevantes (empleado, DNI, fecha, hora, tipo de movimiento, edificio, sector, observación) y filtros rápidos. Ordenable por cualquier columna.
- **Formulario de Carga:** Permite registrar uno o varios movimientos seleccionando el empleado, la fecha, la hora, el tipo de movimiento y opcionalmente el edificio y una observación.
- **Importador Excel:** Pantalla dedicada con zona de carga del archivo, barra de progreso durante la validación, y tabla de resultados que muestra el estado de cada fila (importada correctamente, rechazada por error, o importada con advertencia).
- **Exportador Excel:** Botón de descarga que genera el archivo aplicando los mismos filtros activos en la tabla, incluyendo un resumen con la cantidad de registros y el período.

**Beneficio**

Se digitaliza el control de asistencia, eliminando las planillas de papel y los archivos externos. La importación masiva desde Excel facilita la transición para empresas que ya tienen datos en planillas, y la exportación permite compartir información con áreas que aún no tienen acceso al sistema. La validación exhaustiva del importador previene errores de carga y garantiza la integridad de los datos desde el primer momento.
