● **Versión:** 1.2  
● **Fecha:** 14/04/2026

---

### 5. Registro de Kilómetros en Parte Diario y Vehículos

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

### 6. Sub-tareas dentro de Sectores y Áreas de Proyecto

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
