● **Versión:** 1.3.0
● **Fecha:** 14/04/2026

---

### 10. Registro de Kilómetros en Parte Diario y Vehículos

**Problema Actual**

No existe un mecanismo para registrar el kilometraje de los vehículos a medida que operan día a día. La única forma de conocer los kilómetros de una máquina o vehículo es revisarlo presencialmente, y no queda constancia histórica de cuándo se registró cada valor.

**Mejora Implementada**

El sistema incorporará el seguimiento de kilómetros directamente desde el parte diario, convirtiendo la asignación de cuadrillas en una fuente automática de información de kilometraje para toda la flota.

**Flujo de Trabajo**

1. **Carga del Kilometraje (Operario/Encargado):** Al confeccionar el parte diario y asignar empleados a un vehículo, se habilitará un campo numérico para ingresar la lectura de odómetro actual del vehículo. El dato es por vehículo y por día, independiente de la cantidad de tripulantes asignados.

2. **Actualización Automática del Vehículo:** Al guardar el parte diario, el sistema actualizará automáticamente el kilometraje registrado en la ficha del vehículo, reflejando siempre la última lectura informada.

3. **Consulta Histórica:** Al revisar un parte diario de cualquier fecha pasada, se podrá ver el kilometraje que tenía cada vehículo en ese día, permitiendo reconstruir el uso de la unidad a lo largo del tiempo.

**Experiencia en Pantalla**

- **Parte Diario (armado de cuadrillas):** Cada tarjeta de vehículo incluirá un campo "Km" donde se ingresa la lectura del odómetro. El campo es opcional: si no se completa, no se modifica el registro del vehículo.
- **Parte Diario (vista de consulta):** Al revisar un día ya cargado, cada vehículo mostrará una etiqueta con el kilometraje registrado en esa fecha, visible de un vistazo junto al área de trabajo asignada.
- **Ficha del Vehículo:** En la sección de información básica, se mostrará el campo "Kilómetros" con el valor más reciente, sincronizado automáticamente desde el último parte diario.

**Beneficio**

Se elimina la necesidad de llevar planillas paralelas o registrar el kilometraje en sistemas externos. El dato se captura en el momento operativo natural (la confección del parte diario) y queda disponible tanto en la ficha del vehículo como en el historial de cada día, sin esfuerzo adicional para el usuario.

---

### 11. Sub-tareas dentro de Sectores y Áreas de Proyecto

**Problema Actual**

Todos los registros del sistema (cargas de combustible, contratos, pagos de servicio, comprobantes de venta, asignaciones de personal, vehículos y sueldos) se asocian a un sector o área, pero no es posible especificar a qué tarea concreta dentro de ese sector corresponde cada registro. Esto limita el nivel de detalle con el que se puede analizar la operación y dificulta la rendición de cuentas por actividad.

**Mejora Implementada**

El sistema permitirá definir sub-tareas dentro de cada sector o área, y vincular opcionalmente cualquier registro del sistema a una de estas sub-tareas. Esta funcionalidad estará disponible de forma transversal en todos los módulos que actualmente trabajan con sectores.

**Funcionalidades Principales**

1. **Definición de Sub-tareas:** Cada sector o área de proyecto podrá contener una lista de sub-tareas configurables por el usuario. Cada sub-tarea tendrá un nombre descriptivo y un estado (activa o inactiva).

2. **Selección Condicional en Formularios:** En todos los formularios donde se selecciona un sector (carga de vehículo, contrato de obra, carga de combustible, pago de servicio, comprobante de venta, entre otros), al elegir un sector que tenga sub-tareas definidas, se desplegará automáticamente un segundo campo con las opciones disponibles.
   - La selección de sub-tarea será **siempre opcional**: el usuario podrá dejar el campo sin especificar si no aplica o no desea detallar a ese nivel.
   - Si el sector seleccionado no tiene sub-tareas cargadas, el campo adicional no se mostrará, manteniendo la interfaz limpia.

3. **Visualización en Detalle:** En todas las pantallas de consulta y detalle (fichas de vehículo, contratos, empleados, proveedores, comprobantes, cuadrillas, cargas de combustible, pagos de servicio y sueldos), la sub-tarea se mostrará como complemento del sector, permitiendo identificar de un vistazo el nivel de detalle de cada registro.

4. **Integración con Reportes:** Tanto los reportes generales como los reportes de pago de sueldos incluirán la sub-tarea como información complementaria al sector, ampliando la capacidad de análisis sin modificar la estructura de los reportes existentes.

**Módulos Alcanzados**

La funcionalidad abarcará simultáneamente los siguientes módulos:

| Módulo | Alcance |
|---|---|
| Vehículos | Formulario, detalle y reporte |
| Contratos de Obra | Formulario y detalle |
| Cargas de Combustible | Formulario por ítem y detalle |
| Pagos de Servicio | Formulario y detalle |
| Comprobantes de Venta | Formulario y detalle |
| Comprobantes de Compra | Detalle |
| Empleados | Detalle (sueldo y cuadrilla) |
| Sueldos | Detalle y reporte de pago |
| Cuadrillas / Parte Diario | Detalle |
| Proveedores | Detalle de comprobantes |
| Reportes | General y de sueldos |

**Experiencia en Pantalla**

- **Formularios:** Al seleccionar un sector, si este tiene sub-tareas configuradas, aparecerá un selector adicional con la leyenda "Sub-tarea" y la primera opción será "Sin especificar", permitiendo al usuario omitir la selección.
- **Vistas de Detalle:** La sub-tarea se mostrará junto al nombre del sector con el formato **"Sector — Sub-tarea"**, visible sin necesidad de abrir pantallas adicionales.
- **Reportes:** La columna de sector incluirá la sub-tarea como información complementaria, y en el reporte de sueldos se añadirá una columna dedicada.

**Beneficio**

Se obtiene un nivel de granularidad superior en el seguimiento de costos y actividades sin alterar el flujo de trabajo existente. El usuario que no necesite este nivel de detalle puede simplemente ignorar las sub-tareas, mientras que el que lo requiera podrá rastrear exactamente a qué actividad dentro de cada sector se destinó cada gasto, asignación o recurso.

---

### 12. Reportes Independientes por Módulo de Egresos

**Problema Actual**

El sistema cuenta con un reporte general de egresos que consolida todas las salidas de dinero en una única vista (facturas, salarios, servicios, combustible, seguros, reparaciones, compras de stock y patentes). Si bien este reporte es muy útil para tener una panorámica global, no permite analizar en profundidad el comportamiento de cada categoría de gasto de forma independiente. Cuando un responsable necesita, por ejemplo, revisar cuánto se gastó en combustible por vehículo o cuánto se pagó de servicios por edificio, debe recurrir a exportaciones manuales, filtrar datos en planillas externas o recorrer registro por registro en las pantallas del módulo correspondiente.

El único módulo de egreso que ya dispone de un reporte independiente con análisis jerárquico es el de pago de salarios, que permite navegar desde el resumen por sector hasta el detalle de cada pago individual. Los restantes cuatro módulos de egreso recurrente —facturación, pagos de servicios, cargas de combustible y reparaciones— carecen de esta capacidad.

**Mejora Implementada**

El sistema incorporará reportes independientes para cada uno de los principales módulos de egreso, siguiendo la misma arquitectura de tres capas de profundidad ya probada en el reporte de salarios. Cada reporte permitirá navegar desde un resumen general por sector, pasando por un agrupamiento intermedio por la entidad más relevante del módulo (proveedor, edificio o vehículo según corresponda), hasta llegar al detalle de cada transacción individual. Todos los reportes ofrecerán vista previa en pantalla, exportación a Excel y exportación a PDF.

**Estructura General de los Reportes**

Cada uno de los cinco reportes comparte la misma filosofía de presentación:

1. **Capa 1 — Resumen por Sector:** Muestra los totales agrupados por sector o área de proyecto, permitiendo identificar de un vistazo cómo se distribuye el gasto entre las distintas áreas de la empresa. Incluye subtotales por una dimensión secundaria relevante al módulo.

2. **Capa 2 — Resumen por Entidad:** Dentro de cada sector, agrupa los registros por la entidad más significativa del módulo (empleado, proveedor, edificio o vehículo). Permite evaluar el gasto individual de cada entidad con subtotales y cantidades.

3. **Capa 3 — Detalle de Transacciones:** Muestra cada registro individual con sus atributos completos: fecha, monto, método de pago y datos específicos del módulo. Incluye subtotales por entidad y por sector.

**Formatos de Salida**

Cada reporte está disponible en tres formatos:

- **Vista Previa en Pantalla:** Presentación interactiva con tarjetas de resumen (KPI), grupos expandibles y colapsables, y navegación jerárquica entre las tres capas. Incluye barra de herramientas con acciones de expandir/colapsar todo y botones de descarga.
- **Excel (.xlsx):** Libro de trabajo con tres hojas correspondientes a las tres capas del reporte, con formato profesional, estilos de encabezado, formato de moneda, filas de subtotales y totales generales.
- **PDF:** Documento con tres secciones separadas por salto de página, tablas formateadas con encabezados coloreados, filas de subtotales y totales generales destacados, e información de metadatos (período, fecha de generación, cantidad de registros).

**Panel de Filtros**

Todos los reportes comparten un panel lateral de filtros con los siguientes criterios comunes:

- **Período:** Rango de fechas (desde/hasta) para acotar el análisis temporal.
- **Sectores/Áreas:** Selección múltiple mediante chips interactivos, con opciones de seleccionar todos o ninguno. Si no se selecciona ninguno, se incluyen todos.
- **Rango de Montos:** Monto mínimo y máximo para filtrar transacciones.
- **Filtros específicos por módulo:** Cada reporte incorpora filtros adicionales propios de su naturaleza (frecuencia de pago, tipo de combustible, tipo de comprobante, etc.).

---

#### 12.1. Reporte de Salarios

Este reporte ya se encuentra implementado y funcionando. Se documenta a continuación como referencia y estándar de calidad para los cuatro reportes nuevos.

**Capas del Reporte**

| Capa | Agrupación | Descripción |
|------|-----------|-------------|
| 1 | Sector / Área | Totales de salarios pagados por cada sector de obra |
| 2 | Empleado | Totales por empleado dentro de cada sector |
| 3 | Pago individual | Cada pago de salario con fecha, frecuencia, método y monto |

**Dimensión Secundaria:** Frecuencia de pago (Mensual, Quincenal, Semanal). Se muestran columnas de subtotales por cada frecuencia que tenga registros en el período consultado.

**Filtros Específicos**

- Frecuencia de pago (Mensual / Quincenal / Semanal)
- Método de pago (Efectivo / Transferencia / Cheque)

**Experiencia en Pantalla**

- **Vista Previa:** Tarjetas KPI con total general, cantidad de pagos y totales por frecuencia. Grupos de sectores expandibles que revelan los empleados, y al expandir cada empleado se despliega la tabla con cada pago individual mostrando fecha, frecuencia (con etiqueta de color), método de pago, sub-tarea y monto.
- **Excel:** Hoja 1 "Resumen por Área" con columnas de sector, cantidad, totales por frecuencia y total general. Hoja 2 "Resumen por Empleado" agrupado por sector con filas de subtotales. Hoja 3 "Detalle de Pagos" con cada pago individual y subtotales por empleado y por sector.
- **PDF:** Tres secciones con la misma estructura, tablas formateadas con encabezados azules, filas de subtotales en gris y totales generales en naranja.

---

#### 12.2. Reporte de Facturación

**Capas del Reporte**

| Capa | Agrupación | Descripción |
|------|-----------|-------------|
| 1 | Sector / Área | Totales de comprobantes de compra por cada sector de obra |
| 2 | Proveedor | Totales por proveedor dentro de cada sector |
| 3 | Comprobante individual | Cada factura o nota de crédito con todos sus datos |

**Dimensión Secundaria:** Tipo de comprobante (Factura A, Factura B, Factura C, Nota de Crédito A, Nota de Crédito B, Nota de Crédito C). Se muestran columnas de subtotales únicamente por los tipos que tengan registros en el período consultado.

**Filtros Específicos**

- Tipo de comprobante
- Método de pago (Efectivo / Transferencia / Cheque)
- Estado de pago (Todos / Pagado / No pagado)

**Experiencia en Pantalla**

- **Vista Previa:** Tarjetas KPI con total general, cantidad de comprobantes y totales por tipo de comprobante. Grupos de sectores expandibles que revelan los proveedores, y al expandir cada proveedor se despliega la tabla con cada comprobante mostrando fecha, tipo, punto de venta y número, monto neto, IVA, total, método de pago y estado de pago.
- **Excel:** Hoja 1 "Resumen por Área" con columnas de sector, cantidad, totales por tipo de documento y total general. Hoja 2 "Resumen por Proveedor" con razón social, CUIT, agrupado por sector con subtotales. Hoja 3 "Detalle de Comprobantes" con cada comprobante individual y subtotales por proveedor y por sector.
- **PDF:** Tres secciones con la misma estructura jerárquica y formato visual consistente con el reporte de salarios.

**Beneficio Específico**

Permite identificar rápidamente cuánto se facturó por proveedor y por sector, detectar concentración de gastos en proveedores específicos, y verificar el estado de pago de los comprobantes pendientes, todo sin necesidad de recurrir a planillas externas.

---

#### 12.3. Reporte de Pagos de Servicios

**Capas del Reporte**

| Capa | Agrupación | Descripción |
|------|-----------|-------------|
| 1 | Sector / Área | Totales de pagos de servicios por cada sector de obra |
| 2 | Edificio | Totales por edificio dentro de cada sector |
| 3 | Pago individual | Cada pago de servicio con sus datos completos |

**Dimensión Secundaria:** Tipo de servicio (Electricidad, Gas, Agua, Internet, entre otros). Se muestran columnas de subtotales por cada tipo de servicio que tenga registros en el período consultado.

**Filtros Específicos**

- Tipo de servicio
- Método de pago
- Año
- Período / Mes (1 a 12)

**Experiencia en Pantalla**

- **Vista Previa:** Tarjetas KPI con total general, cantidad de pagos y totales por tipo de servicio. Grupos de sectores expandibles que revelan los edificios, y al expandir cada edificio se despliega la tabla con cada pago mostrando fecha, proveedor del servicio, tipo de servicio, año y período, número de referencia, método de pago y monto. Los pagos de servicios que no estén asociados a un edificio se agruparán bajo la categoría "Sin Edificio asignado".
- **Excel:** Hoja 1 "Resumen por Área" con columnas de sector, cantidad, totales por tipo de servicio y total general. Hoja 2 "Resumen por Edificio" agrupado por sector con subtotales. Hoja 3 "Detalle de Pagos" con cada pago individual y subtotales por edificio y por sector.
- **PDF:** Tres secciones con la misma estructura jerárquica y formato visual.

**Beneficio Específico**

Permite analizar el gasto en servicios e impuestos por edificio y por tipo, facilitando la comparación de costos operativos entre distintas locaciones y la detección de variaciones anómalas en consumos de servicios de un período a otro.

---

#### 12.4. Reporte de Cargas de Combustible

**Capas del Reporte**

| Capa | Agrupación | Descripción |
|------|-----------|-------------|
| 1 | Sector / Área | Totales de cargas de combustible por cada sector de obra |
| 2 | Vehículo | Totales por vehículo dentro de cada sector |
| 3 | Carga individual | Cada carga de combustible con todos sus datos |

**Dimensión Secundaria:** Tipo de combustible (Nafta Súper, Nafta Premium, Gasoil, Gasoil Premium, GNC). Se muestran columnas de subtotales por cada tipo de combustible que tenga registros en el período consultado.

**Métrica Adicional:** Además del monto en pesos, este reporte incorpora el total de litros cargados como métrica complementaria en todos los niveles de agrupación, permitiendo un análisis tanto económico como de consumo volumétrico.

**Filtros Específicos**

- Tipo de combustible
- Vehículo (búsqueda por patente)
- Estación de servicio

**Experiencia en Pantalla**

- **Vista Previa:** Tarjetas KPI con total general en pesos, cantidad de cargas, total de litros y totales por tipo de combustible. Grupos de sectores expandibles que revelan los vehículos, mostrando patente, marca y modelo. Al expandir cada vehículo se despliega la tabla con cada carga mostrando fecha, tipo de combustible, litros, precio por litro, total, estación de servicio y número de ticket. Las cargas que no estén asociadas a un vehículo (bidones o tambores) se agruparán bajo "Sin Vehículo asignado".
- **Excel:** Hoja 1 "Resumen por Área" con columnas de sector, cantidad, litros, totales por tipo de combustible y total general. Hoja 2 "Resumen por Vehículo" con patente, marca/modelo, litros, agrupado por sector con subtotales. Hoja 3 "Detalle de Cargas" con cada carga individual y subtotales por vehículo y por sector.
- **PDF:** Tres secciones con la misma estructura jerárquica y formato visual.

**Beneficio Específico**

Permite controlar el consumo de combustible por vehículo y por sector, identificar unidades con consumos elevados, comparar rendimientos entre vehículos similares y verificar la coherencia entre litros cargados y kilómetros recorridos cuando se utiliza en conjunto con el registro de kilometraje del parte diario.

---

#### 12.5. Reporte de Reparaciones

**Capas del Reporte**

| Capa | Agrupación | Descripción |
|------|-----------|-------------|
| 1 | Sector / Área | Totales de reparaciones por cada sector de obra |
| 2 | Vehículo | Totales por vehículo dentro de cada sector |
| 3 | Reparación individual | Cada reparación con su desglose de costos |

**Dimensión Secundaria:** Tipo de costo (Materiales y Mano de Obra). A diferencia de los demás reportes donde la dimensión varía según los datos, este reporte siempre presenta dos columnas fijas: el subtotal de materiales y el subtotal de mano de obra, permitiendo analizar la composición del gasto en reparaciones.

**Filtros Específicos**

- Vehículo (búsqueda por patente)
- Proveedor (para filtrar reparaciones realizadas por talleres externos)

**Experiencia en Pantalla**

- **Vista Previa:** Tarjetas KPI con total general, cantidad de reparaciones, total en materiales y total en mano de obra. Grupos de sectores expandibles que revelan los vehículos, mostrando patente, marca y modelo. Al expandir cada vehículo se despliega la tabla con cada reparación mostrando fecha, descripción, kilometraje al momento de la reparación, proveedor (si es reparación externa), costo de materiales, costo de mano de obra y total. El sector se determina a partir del área asignada al vehículo.
- **Excel:** Hoja 1 "Resumen por Área" con columnas de sector, cantidad de reparaciones, total materiales, total mano de obra y total general. Hoja 2 "Resumen por Vehículo" con patente, marca/modelo, cantidad, materiales, mano de obra y total, agrupado por sector con subtotales. Hoja 3 "Detalle de Reparaciones" con cada reparación individual y subtotales por vehículo y por sector.
- **PDF:** Tres secciones con la misma estructura jerárquica y formato visual.

**Beneficio Específico**

Permite evaluar el costo de mantenimiento de cada unidad de la flota, identificar vehículos con gastos recurrentes que podrían justificar su reemplazo, analizar la proporción entre costos de materiales y mano de obra, y comparar el rendimiento de proveedores de servicios mecánicos externos.

---

**Integración con Sub-tareas**

Todos los reportes incorporan la información de sub-tareas como dato complementario al sector. En la vista previa, la sub-tarea se muestra como una columna adicional en la tabla de detalle (Capa 3). En los archivos Excel y PDF, la sub-tarea aparece junto al nombre del sector con el formato "Sector — Sub-tarea" en la hoja de detalle, ampliando la capacidad de análisis sin alterar la estructura jerárquica del reporte.

**Permisos**

El acceso a los reportes se controla mediante los permisos existentes del módulo de informes. Cualquier usuario con permiso de generación de reportes financieros podrá acceder a los cinco reportes. La exportación a archivos requiere adicionalmente el permiso de exportación.

**Beneficio General**

Se transforma la capacidad analítica del sistema al llevar cada módulo de egreso al mismo nivel de detalle que ya ofrecía el reporte de salarios. El responsable financiero podrá auditar cualquier categoría de gasto con tres niveles de profundidad —sector, entidad y transacción individual— sin salir del sistema ni recurrir a herramientas externas. La consistencia en el formato y la navegación entre los cinco reportes reduce la curva de aprendizaje: quien sepa usar uno, sabrá usar todos.
