● **Versión:** 1.5.0
● **Fecha:** 11/05/2026

-----19. Órdenes de Compra

**Problema Actual**
No existe en el sistema una forma de registrar formalmente las necesidades de compra del equipo. Cuando un encargado o un operario necesita materiales, herramientas, ropa de trabajo o insumos, la solicitud se hace de palabra, por mensaje de texto o en un anotador, sin ningún tipo de trazabilidad. El supervisor no tiene visibilidad de qué se pidió, quién lo pidió ni cuándo, y la administración tampoco puede asociar la compra efectivamente realizada con la necesidad original que la motivó. Esto genera duplicación de pedidos, compras sin respaldo y una desconexión total entre quien necesita y quien compra.

**Mejora Implementada**
Se incorpora un módulo completo de **Órdenes de Compra** que formaliza el flujo desde la solicitud hasta la ejecución de la compra. El módulo define roles claros: el operario o encargado crea la orden detallando lo que necesita, el supervisor la revisa y aprueba, y la administración la vincula al comprobante fiscal real una vez que la compra se realizó.

**Flujo de la Orden**

1. **Creación (operario/encargado):** El usuario completa un formulario indicando la categoría del pedido (Materiales, Herramientas, Ropa/EPP, Insumos u Otro), la prioridad (Baja, Media o Alta), el motivo o descripción de la necesidad, la lista de ítems solicitados y opcionalmente el monto estimado. La orden queda en estado **Pendiente**.

2. **Revisión y aprobación (supervisor):** El supervisor ve el listado de todas las órdenes del tenant ordenadas por prioridad. Puede pasar una orden a **En Revisión** (para estudiarla o pedir correcciones al solicitante), **Aprobarla** directamente, o devolverla a Pendiente si requiere ajustes.

3. **Ejecución (administración/comprador):** Una vez aprobada, el comprador marca la orden como **Comprada** y puede vincularla al comprobante de factura correspondiente en el sistema. Este vínculo le da cierre formal al pedido y permite consultar, desde el comprobante, qué orden de compra lo originó.

**Estados de la Orden**

| Estado | Descripción |
|---|---|
| Pendiente | Recién creada, esperando revisión |
| En Revisión | El supervisor la está evaluando |
| Aprobada | Autorizada para proceder con la compra |
| Comprada | Ejecutada y opcionalmente vinculada a comprobante |

Las transiciones de retroceso están habilitadas: una orden puede volver de "En Revisión" a "Pendiente" o de "Aprobada" a "En Revisión" sin eliminarse, permitiendo al supervisor solicitar correcciones al solicitante sin perder el historial.

**Experiencia en Pantalla**
● Nuevo módulo **"Órdenes de Compra"** en el menú principal.
● Vista de listado con filtros por estado, categoría, prioridad y fecha, con indicador visual de prioridad (colores) en cada fila.
● Formulario de creación con campo de lista de ítems dinámico (agregar y quitar ítems individualmente).
● Panel de detalle con historial completo del estado de la orden y campo para vincular el comprobante al momento de marcarla como comprada.
● Acceso diferenciado por permiso: quien puede crear órdenes no necesariamente puede aprobarlas; quien puede aprobarlas no necesariamente puede ejecutarlas.

**Beneficio**
La empresa pasa de gestionar las compras de boca en boca a tener un registro centralizado, trazable y auditado de cada necesidad, desde que se origina hasta que se satisface. Se eliminan los pedidos duplicados, se puede priorizar y planificar el gasto de compras, y la administración puede conciliar cada factura con la orden que la justificó.

-----20. Incidentes Laborales

**Problema Actual**
El sistema ya permite registrar sanciones disciplinarias a empleados, pero no existe ningún módulo para registrar los **eventos operativos** que generan costos, multas o daños para la empresa. Cuando un empleado está involucrado en un accidente vehicular, recibe una multa de tránsito manejando un vehículo de la empresa, rompe una red de gas o agua durante una excavación, o daña material de trabajo, todo ese registro se hace fuera del sistema. No hay trazabilidad del impacto económico de estos eventos, no hay estado de resolución, y tampoco hay forma de relacionar el incidente con la sanción disciplinaria que eventualmente se emite.

**Mejora Implementada**
Se incorpora un módulo de **Incidentes Laborales** para registrar eventos operativos que generan consecuencias económicas o legales para la empresa, separado conceptualmente del módulo de acciones disciplinarias existente. La relación entre ambos es opcional: un incidente puede motivar una sanción, pero también puede existir sin que se tome ninguna acción disciplinaria, y viceversa.

**Tipos de Incidente**

● Accidente vehicular
● Multa de tránsito
● Daño a infraestructura
● Daño a redes de servicio (gas, agua, telefonía, electricidad)
● Daño a material o equipo
● Infracción normativa
● Otro

**Funcionalidades Principales**

1. **Registro del Incidente:** Cada incidente registra la fecha, el tipo, una descripción detallada del evento, los empleados involucrados (uno o más), el bien afectado (ej: "Camión F-350, patente AB123"), el impacto económico estimado (opcional) y el estado de resolución.

2. **Estados de Resolución:** Un incidente puede estar en tres estados: **Pendiente**, **En Investigación** y **Resuelto**. Al pasar a Resuelto, el sistema solicita la fecha de resolución (con auto-completado al día actual) y la guarda como parte del registro.

3. **Vínculo con Acción Disciplinaria:** Al cargar o editar una acción disciplinaria existente, el usuario puede asociarla opcionalmente a un incidente laboral del sistema. El detalle de la acción disciplinaria mostrará el incidente vinculado como referencia. Esta relación es siempre opcional en ambas direcciones.

**Filtros Disponibles**
● Empleado involucrado (búsqueda por nombre/apellido o selector directo).
● Tipo de incidente.
● Estado.
● Rango de fechas.
● Toggle "Solo con impacto económico" para analizar los incidentes que generaron costos.

**Experiencia en Pantalla**
● Nueva entrada **"Incidentes Laborales"** dentro del módulo de Recursos Humanos.
● Vista de listado con badge de estado por fila (colores: Pendiente = naranja, En Investigación = amarillo, Resuelto = verde) e indicación del impacto económico cuando existe.
● Formulario con selector múltiple de empleados: se agregan como chips individuales removibles con búsqueda en tiempo real.
● En el detalle de una acción disciplinaria existente, chip visible del incidente asociado con tipo y fecha, clickeable para navegar al incidente desde el módulo correspondiente.

**Beneficio**
La empresa puede por primera vez cuantificar y auditar el impacto económico de los eventos operativos negativos. Queda registro de qué ocurrió, cuánto costó, quiénes estuvieron involucrados y cómo se resolvió. Cuando se emite una sanción disciplinaria, el incidente que la motivó queda documentado y trazable, evitando la desconexión entre el hecho y la consecuencia formal.

-----21. Sistema de Notificaciones

**Problema Actual**
El sistema almacena fechas de vencimiento en varios módulos críticos — la VTV de los vehículos, los cheques emitidos que requieren seguimiento de estado, las fechas de pago de pólizas de seguro, los vencimientos de servicios y el cierre de contratos de obra — pero no existe ningún mecanismo que avise al usuario cuando alguna de estas fechas se aproxima. El usuario debe ingresar periódicamente a cada módulo, revisar uno por uno los registros y verificar si algo está por vencer. En la práctica, esto no ocurre con la frecuencia necesaria, y los vencimientos se detectan tarde o, en el peor caso, cuando ya ocurrieron.

**Mejora Implementada**
Se incorpora un **Sistema de Notificaciones** que envía avisos automáticos al usuario cuando una fecha de vencimiento se aproxima, con la anticipación y a través de los canales que él mismo configure. El sistema es completamente opt-in: cada usuario (o un usuario con permisos de administración) decide a qué quiere suscribirse, con cuánta antelación y por qué medio quiere recibir los avisos.

**Módulos y Vencimientos Cubiertos**

| Módulo | Criterio de Notificación |
|---|---|
| Vehículos | Vencimiento de la VTV |
| Pagos (Cheques) | Fecha de vencimiento de cheques en estado Pendiente |
| Pólizas de Seguro | Próxima fecha de pago de la cuota |
| Afectaciones de Servicios | Próxima fecha de vencimiento del servicio |
| Contratos de Obra | Fecha de cierre del contrato |

**Canales de Notificación**

Cada suscripción puede combinar libremente los tres canales disponibles:

● **Notificación en sistema:** Aparece en tiempo real dentro de la aplicación a través de una campana de notificaciones en la barra superior. El aviso llega instantáneamente mientras el usuario tiene el sistema abierto, y queda disponible en la bandeja de entrada si no estaba conectado al momento del envío.

● **WhatsApp:** El aviso se envía como mensaje al número de WhatsApp configurado en el perfil del usuario.

● **Email:** El aviso se envía al correo electrónico registrado en el perfil del usuario, con un formato profesional que incluye el nombre del recurso, la fecha exacta de vencimiento y los días restantes.

**Configuración de Suscripciones**

Cada suscripción es configurable en detalle:

1. **A qué suscribirse:** El usuario puede suscribirse a un recurso concreto (ej: el vehículo con patente AB123CDE) o a todos los recursos de un tipo (ej: todos los vehículos del tenant). En el segundo caso, recibirá un aviso por cada vehículo que esté próximo a vencer su VTV.

2. **Cuántos avisos:** Se pueden configurar múltiples avisos dentro de una misma suscripción. Ejemplo: un aviso 30 días antes, otro a 7 días y otro a 1 día del vencimiento.

3. **Canales:** Selección independiente de uno, dos o los tres canales disponibles. Los mismos canales aplican a todos los avisos de esa suscripción.

**Gestión de Permisos**

El sistema introduce dos permisos específicos:

● **Auto-suscripción:** Permite al usuario configurar sus propias suscripciones.
● **Asignación a terceros:** Permite a un usuario (administrador o responsable) configurar suscripciones en nombre de otro usuario, útil para garantizar que ciertos vencimientos críticos siempre lleguen a la persona correcta aunque ella no lo haya configurado por su cuenta.

**Experiencia en Pantalla**

● **Campana de notificaciones:** Ícono en la barra superior de la aplicación con un contador rojo de avisos no leídos. Al hacer click se despliega un panel con los avisos recientes, mostrando el recurso afectado, los días restantes y la fecha exacta. Incluye un acceso directo a la pantalla de gestión de suscripciones.

● **Bandeja de entrada:** Los avisos del canal "sistema" quedan almacenados y son consultables aunque el usuario no haya estado conectado cuando llegaron. Los avisos de WhatsApp y email no requieren que el usuario esté en el sistema.

● **Gestión de suscripciones:** Pantalla dedicada accesible desde el menú de configuración o desde la campana. Muestra todas las suscripciones activas del usuario con sus canales y avisos configurados. Desde aquí se crean, modifican o eliminan suscripciones con un formulario que incluye selector de módulo y recurso, checkboxes de canales y lista dinámica de días de antelación.

**Beneficio**
La empresa elimina por completo el riesgo de que un vencimiento importante pase desapercibido. El sistema toma la responsabilidad de recordar, con la anticipación suficiente para tomar acción, y lo hace a través del canal más conveniente para cada usuario. La configuración es flexible y no invasiva: quien no quiera recibir avisos sobre un módulo simplemente no se suscribe. Quien quiera máxima cobertura puede recibir el mismo aviso por tres canales distintos con 30, 15, 7 y 1 día de anticipación.
