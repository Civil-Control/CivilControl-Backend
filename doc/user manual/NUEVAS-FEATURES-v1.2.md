● **Versión:** 1.2.0
● **Fecha:** 14/04/2026

-----5. Panel de Trabajo Interactivo y Semáforo de Órdenes de Reparación

**Problema Actual**

El personal de taller tiene que consultar la tabla de órdenes de reparación y filtrar manualmente para saber qué trabajos hay pendientes o en curso. No existe una pantalla operativa que muestre de un vistazo el estado de todos los trabajos activos, ni un indicador visual rápido en la tabla que permita distinguir el estado de cada orden sin leer la columna de texto.

**Mejora Implementada**

El sistema contará con dos mejoras visuales sobre el módulo de órdenes de reparación:

1. **Panel de Trabajo (Panel de Taller):** Pantalla tipo tablero con dos columnas: la primera muestra las órdenes **pendientes** y la segunda las órdenes **en proceso**. Se actualiza automáticamente cada 30 segundos y muestra un reloj en vivo. Está diseñada para usarse en un monitor de taller a pantalla completa, sin barra de navegación.
2. **Semáforo en la tabla:** Cada fila de la tabla de órdenes se colorea según su estado: **rojo** para pendientes, **amarillo** para en proceso y **verde** para completadas. El usuario puede alternar entre un modo suave y un modo intenso de coloreo.

**Funcionalidades del Panel de Taller**

- Dos columnas con tarjetas: cada tarjeta muestra la patente del vehículo, los ítems reportados, la descripción, la fecha y quién reportó la falla.
- Botón **"Iniciar"** en las tarjetas pendientes para pasar la orden a "en proceso".
- Botón **"Finalizar"** en las tarjetas en proceso para abrir el formulario de completar la reparación.
- Actualización automática cada 30 segundos sin intervención del usuario.

**Acceso**

- Desde la tabla de órdenes de reparación, un botón **"Panel de Trabajos"** lleva directamente al tablero.
- La pantalla se muestra sin barra de navegación ni menú, a pantalla completa, ideal para un monitor fijo en el taller.

**Beneficio**

El taller obtiene una vista operativa en tiempo real que permite planificar y ejecutar trabajos sin necesidad de filtrar tablas. El semáforo en la tabla facilita la identificación inmediata del estado de cada orden al recorrer el listado.

-----6. Parte Diario: Asignación de Cuadrillas a Vehículos

**Problema Actual**

Cada mañana se decide qué empleados viajan en qué vehículo y a qué zona de obra se dirigen. Esta asignación se realiza de forma verbal o en papel, sin que quede registro digital. No se puede consultar quién fue a qué obra un día determinado, y no existe un historial de asignaciones.

**Mejora Implementada**

El sistema contará con un módulo de **parte diario** que permite armar cuadrillas diarias asignando empleados a vehículos, indicando la zona de proyecto, la sub-tarea y quién conduce.

**Funcionalidades Principales**

1. **Constructor de Cuadrillas:** Interfaz visual donde se arman las cuadrillas del día arrastrando empleados hacia tarjetas de vehículos.
   ○ Se selecciona la fecha y la zona de proyecto.
   ○ Se arrastra cada empleado desde una lista hacia el vehículo correspondiente.
   ○ Se marca quién es el conductor de cada vehículo.
   ○ Al guardar, el sistema envía todas las asignaciones del día en un solo paso.
2. **Advertencias Inteligentes:** Al guardar, el sistema alerta (sin bloquear) sobre situaciones inusuales:
   ○ Un empleado marcado como conductor que no tiene rol de chofer.
   ○ Más de 2 personas asignadas al mismo vehículo.
   ○ Un empleado duplicado en el mismo día.
3. **Resumen Diario:** Vista de solo lectura que muestra la conformación de cuadrillas de cualquier fecha pasada, con los vehículos, sus tripulantes, la zona de proyecto y el kilometraje registrado.
4. **Calendario Mensual:** Vista de calendario que muestra por cada día la cantidad de vehículos y empleados asignados, permitiendo identificar de un vistazo los días sin parte diario.
5. **Historial con Filtros:** Tabla convencional con filtros por empleado, vehículo, zona de proyecto, fecha y conductor, con paginación y ordenamiento.

**Experiencia en Pantalla**

- Nueva sección **"Parte Diario"** en el menú, dentro del módulo de Flota.
- La pantalla principal muestra tres vistas: el constructor de cuadrillas, el calendario mensual y el historial de asignaciones.
- El constructor permite armar todo el parte diario de forma visual y guardarlo con un solo clic.

**Beneficio**

Se digitaliza la asignación diaria de personal a vehículos, generando un historial consultable que antes no existía. El constructor visual simplifica una tarea que antes requería coordinación verbal, y el calendario permite supervisar rápidamente qué días tienen parte cargado.

-----7. Generación de PDF de Órdenes de Pago

**Problema Actual**

Cuando la empresa realiza un pago a un proveedor, necesita generar un documento formal de "Orden de Pago" para archivo y respaldo administrativo. Actualmente este documento se confecciona manualmente fuera del sistema, transcribiendo los datos del pago, los comprobantes imputados y el medio de pago.

**Mejora Implementada**

El sistema generará automáticamente el PDF de la orden de pago a partir de los datos ya registrados en el pago. El documento replica el formulario oficial de la empresa e incluye toda la información necesaria.

**Contenido del Documento**

El PDF generado incluye las siguientes secciones:

1. **Encabezado:** Datos de la empresa (razón social, CUIT, dirección), número de orden de pago y fecha.
2. **Beneficiario:** Nombre del proveedor, CUIT, dirección y condición frente al IVA.
3. **Concepto:** Descripción del motivo del pago.
4. **Comprobantes Imputados:** Tabla con todos los comprobantes de compra vinculados al pago (tipo, número, fecha y monto de cada uno).
5. **Medio de Pago:** Detalle del medio de pago utilizado:
   ○ Si es cheque: banco, número, fecha de emisión y fecha de cobro.
   ○ Si es transferencia: banco, CBU destino y número de operación.
   ○ Si es efectivo: se indica como tal.
6. **Totales:** Monto neto, bruto, retenciones y saldo no imputado.

**Funcionamiento**

- Desde el detalle de cualquier pago, un botón **"Generar Orden de Pago"** genera y descarga el PDF automáticamente.
- El archivo se descarga con el nombre `orden-de-pago-{número}.pdf`.
- No requiere configuración adicional.

**Beneficio**

Se elimina la confección manual del documento de orden de pago, evitando errores de transcripción y ahorrando tiempo administrativo. La información del PDF siempre coincide con lo registrado en el sistema, garantizando consistencia documental.

-----8. Mejoras en Proveedores y Clientes: Alias y Contactos Múltiples

**Problema Actual**

Los proveedores frecuentemente se conocen internamente por un nombre informal distinto de su razón social (por ejemplo, "El Corralón de Juan" en lugar de "Corralón del Norte SRL"), pero el sistema no contempla este dato. Además, tanto proveedores como clientes almacenan un único contacto (un email y un teléfono), cuando en la práctica cada empresa tiene múltiples referentes (compras, administración, facturación).

**Mejora Implementada**

Se implementan dos mejoras en las fichas de proveedores y clientes:

1. **Alias en Proveedores:** Se agrega un campo opcional de "alias" o nombre informal, buscable y filtrable desde el listado.
   ○ El alias aparece en las búsquedas, por lo que escribir el nombre informal del proveedor es suficiente para encontrarlo.
2. **Contactos Múltiples:** Se reemplaza el contacto único por una lista de contactos, tanto en proveedores como en clientes.
   ○ Cada contacto tiene un nombre de referencia (por ejemplo, "Administración", "Ventas", "Facturación").
   ○ Cada contacto puede tener múltiples emails y múltiples teléfonos.
   ○ Se pueden agregar y quitar contactos desde el formulario de edición.

**Migración de Datos**

Los contactos existentes se migran automáticamente como "Contacto principal" para que no se pierda ningún dato previo.

**Beneficio**

El alias permite encontrar proveedores por el nombre con el que realmente se los conoce en la empresa. Los contactos múltiples reflejan la realidad de que cada proveedor o cliente tiene distintas personas de referencia según el área, evitando depender de un solo dato de contacto.

-----9. Asistencia Inteligente: Registro de Fichajes con Integración Biométrica

**Problema Actual**

No existe un módulo para registrar la asistencia de los empleados. El control de entradas y salidas se realiza en planillas de papel o en archivos de Excel externos, lo que dificulta la consulta histórica, impide filtrar por empleado, obra o período, y no permite integrar esta información con el resto del sistema. Las empresas que tienen relojes biométricos (como Hikvision) no pueden volcar automáticamente los fichajes al sistema.

**Mejora Implementada**

El sistema contará con un módulo completo de registro de asistencia con cuatro vías de ingreso de datos: carga manual, carga por lotes, importación desde Excel e **integración automática con relojes biométricos**.

**Funcionalidades Principales**

1. **Registro de Movimientos:** Cada registro representa una entrada o salida de un empleado, con fecha, hora, tipo de movimiento, edificio y observación. El sistema valida que no existan duplicados y que la fecha no sea futura.
2. **Carga Masiva por Lotes:** Permite registrar hasta 100 movimientos simultáneamente desde un formulario de carga rápida.
3. **Importación desde Excel:** El usuario descarga una plantilla, la completa con los datos de asistencia y la sube al sistema.
   ○ El importador valida cada fila: verifica que el DNI exista, detecta duplicados, controla fechas futuras y alerta sobre inconsistencias como una salida sin entrada previa.
   ○ Muestra un informe detallado fila por fila antes de confirmar la importación.
4. **Exportación a Excel:** Los registros filtrados se descargan en un archivo Excel con formato profesional, compatible con la plantilla de importación para permitir la ida y vuelta.
5. **Integración con Relojes Biométricos (Hikvision):** Los relojes biométricos se configuran para enviar los fichajes directamente al sistema, de forma automática y en tiempo real.
   ○ Cada vez que un empleado ficha en el reloj, el sistema recibe el evento, identifica al empleado por DNI y registra automáticamente la entrada o salida.
   ○ El sistema detecta si el siguiente fichaje debe ser una entrada o una salida, alternando automáticamente.
   ○ La configuración se realiza desde la pantalla de ajustes de la empresa, donde se muestra la URL que debe cargarse en el reloj.
   ○ El diseño permite agregar soporte para otras marcas de relojes en el futuro sin modificar lo existente.

**Experiencia en Pantalla**

- Nueva sección **"Asistencia"** en el menú, dentro del módulo de Recursos Humanos.
- Tabla paginada con filtros por empleado, DNI, tipo de movimiento, edificio, sector, rango de fechas y rango de horarios.
- Botones de importación y exportación Excel en la barra de herramientas de la tabla.
- En la configuración de la empresa, sección dedicada para copiar la URL del webhook del reloj biométrico con instrucciones de configuración.

**Beneficio**

Se digitaliza completamente el control de asistencia. La importación desde Excel facilita la transición para empresas que ya llevan datos en planillas. La integración con relojes biométricos elimina la carga manual: los fichajes se registran automáticamente al pasar la tarjeta o la huella, sin intervención humana. El sistema queda preparado para incorporar nuevas marcas de dispositivos a medida que sea necesario.
