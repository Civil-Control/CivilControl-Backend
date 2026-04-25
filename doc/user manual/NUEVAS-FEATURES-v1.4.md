● **Versión:** 1.4.0
● **Fecha:** 24/04/2026

-----13. Reporte de Comprobantes de Venta

**Problema Actual**
El sistema permite registrar facturas, notas de débito y notas de crédito emitidas a clientes, pero no ofrece una herramienta de análisis dedicada para revisar la facturación del período. Para conocer cuánto se le facturó a cada cliente, qué comprobantes están cobrados y cuáles pendientes, o cómo se distribuyeron las ventas por sector, hay que recorrer manualmente la pantalla de comprobantes y filtrar a mano. Tampoco existe un reporte que cruce los certificados de obra con los comprobantes emitidos para verificar la trazabilidad de la facturación contractual.

**Mejora Implementada**
Se incorpora un nuevo reporte de **Comprobantes de Venta**, siguiendo la misma arquitectura de tres capas y estilos visuales ya establecidos en los reportes de egresos. Permite analizar la facturación con tres niveles de profundidad y filtrarla por todas las dimensiones relevantes del módulo de ventas.

**Estructura del Reporte**

1. **Capa 1 — Resumen por Sector:** Totales facturados, cobrados y saldo pendiente agrupados por sector o área de proyecto.
2. **Capa 2 — Resumen por Cliente:** Dentro de cada sector, los comprobantes se agrupan por cliente, con los totales correspondientes y el saldo de cada uno.
3. **Capa 3 — Detalle de Comprobantes:** Cada comprobante individual con punto de venta, número, fecha, tipo, monto neto, IVA, total, estado de cobro, certificado de obra vinculado (si lo tiene) y número de pedido del cliente.

**Filtros Disponibles**

● Período (rango de fechas).
● Sectores/Áreas (selección múltiple).
● Clientes (autocompletado, selección múltiple).
● Tipo de comprobante (Factura A, B, C, Nota de Débito A/B/C, Nota de Crédito A/B/C).
● Estado de cobro (Cobrado, Pendiente).
● Rango de Montos.
● Certificado vinculado (solo comprobantes que correspondan a un certificado de obra, o solo los que no lo hacen).

**Formatos de Salida**

● **Vista Previa en Pantalla:** Navegación jerárquica con tarjetas KPI (Total Facturado, Total Cobrado, Saldo Pendiente, Cantidad de Comprobantes).
● **Excel (.xlsx):** Tres hojas correspondientes a cada capa.
● **PDF:** Tres secciones separadas con formato profesional, mismo estilo visual que el resto de los reportes.

**Experiencia en Pantalla**
● Nueva entrada **"Reporte de Ventas"** dentro del menú de Informes.
● Panel de filtros lateral con persistencia de la última configuración usada.
● Detalle de cada comprobante con link directo a la ficha del comprobante para abrir su detalle completo.

**Beneficio**
La empresa cuenta por primera vez con una vista analítica completa de la facturación, equivalente en profundidad a los reportes de egresos. Permite responder en segundos preguntas como "¿cuánto le facturé a este cliente este trimestre?", "¿qué comprobantes vencidos están sin cobrar?" o "¿qué porcentaje de la facturación corresponde a cada sector de obra?", sin necesidad de exportar datos ni armar planillas externas.

-----14. Reporte de Cuenta Corriente de Proveedores

**Problema Actual**
Cuando se necesita revisar la situación de un proveedor (qué facturas están pendientes de pago, qué notas de débito o crédito se aplicaron, qué pagos se le hicieron y cuál es el saldo final), no existe una vista consolidada. Hay que ingresar al detalle de cada comprobante y a cada pago por separado, sumando manualmente. Esto vuelve impráctico el control de cuentas a pagar y dificulta la conciliación periódica con los proveedores.

**Mejora Implementada**
Se incorpora un nuevo reporte de **Cuenta Corriente de Proveedores** que muestra, para cada proveedor seleccionado, el saldo previo al período consultado y el detalle cronológico de todos los movimientos: facturas emitidas, notas de débito, notas de crédito y pagos realizados. El sistema calcula el saldo acumulado movimiento a movimiento, mostrando el estado real de la deuda en cualquier punto del tiempo.

**Estructura del Reporte**

1. **Capa 1 — Resumen por Proveedor:** Para cada proveedor seleccionado, muestra el saldo inicial (anterior al período), el total de movimientos del período, el saldo final y la cantidad de movimientos.
2. **Capa 2 — Detalle de Movimientos:** Por cada proveedor, una tabla cronológica con todos los movimientos (factura, nota de débito, nota de crédito, pago) con su fecha, tipo, número de comprobante o pago, monto, signo (suma o resta), estado y saldo acumulado tras el movimiento.

**Filtros Disponibles**

● Período (rango de fechas).
● Proveedores (autocompletado, selección múltiple — si no se selecciona ninguno se incluyen todos).
● Estado del comprobante (Pendiente, Cancelado).
● Tipo de movimiento (Factura, Nota de Débito, Nota de Crédito, Pago).
● Solo proveedores con saldo pendiente.

**Formatos de Salida**

● **Vista Previa en Pantalla:** Cabecera con datos del proveedor, fila de saldo inicial, listado cronológico, fila de saldo final destacada.
● **Excel (.xlsx):** Una hoja por proveedor o consolidado, según selección.
● **PDF:** Formato apto para enviar al proveedor como conciliación, con encabezado de la empresa y cierre con saldo final destacado.

**Experiencia en Pantalla**
● Nueva entrada **"Cuenta Corriente Proveedores"** dentro del menú de Informes.
● Desde la ficha de un proveedor, botón directo "Ver cuenta corriente" que abre el reporte ya filtrado por ese proveedor.
● Color rojo para movimientos que aumentan deuda, verde para los que la disminuyen, saldos finales en negro destacado.

**Beneficio**
Se obtiene en una sola pantalla la posición real frente a cada proveedor, con todos los movimientos ordenados y el saldo acumulado calculado automáticamente. Facilita la conciliación periódica, permite detectar pagos no imputados o comprobantes mal cargados, y agiliza enormemente la respuesta a consultas del proveedor sobre cuánto se le debe y por qué.

-----15. Tesorería: Cajas, Cuentas Bancarias y Chequeras

**Problema Actual**
El sistema actualmente registra los pagos por cheque o transferencia con un campo de texto libre llamado "Banco", sin un control real sobre las cuentas bancarias propias de la empresa. No existe el concepto de **caja en efectivo** con saldo trazable, no hay forma de saber cuánto dinero hay disponible en cada caja en cualquier momento, y no se puede llevar el control de los talonarios de cheques (qué números están disponibles, cuáles ya fueron usados, cuáles pertenecen a qué cuenta). Esto deja al sistema sin la columna vertebral financiera que necesita una empresa con manejo intensivo de pagos.

**Mejora Implementada**
Se incorpora un módulo completo de **Tesorería** con tres entidades nuevas: **Cajas en Efectivo**, **Cuentas Bancarias** y **Chequeras**. Todas con saldo trazable, historial de movimientos y reglas que aseguran consistencia con el resto del sistema. Los pagos en efectivo, cheque y transferencia pasan a operar contra estas entidades, generando movimientos automáticos que reflejan en cada momento el estado financiero real.

**Funcionalidades Principales**

1. **Cajas en Efectivo:** Cada empresa puede crear cuantas cajas necesite (caja chica de obra, caja administración, caja por sector, etc.). Cada caja tiene saldo actualizado en tiempo real, historial completo de movimientos y operaciones manuales de "Agregar dinero", "Quitar dinero" y "Ajustar saldo" (esta última con permiso especial y comentario obligatorio).
   ○ Si una operación dejaría la caja con saldo negativo, el sistema **avisa con confirmación** (no bloquea), y la caja queda señalada con una advertencia visual hasta que se regularice.
   ○ Cuando se carga un pago en efectivo, si existe al menos una caja activa, la selección de caja se vuelve **obligatoria**, asegurando que todo egreso de efectivo quede asociado.

2. **Cuentas Bancarias:** Reemplazan al campo de texto libre actual. Cada cuenta tiene nombre interno, banco, tipo (corriente, caja de ahorro, USD, etc.), número de cuenta, CBU, alias y moneda. Su saldo se actualiza automáticamente con cada cheque emitido (reserva inmediata), transferencia o ajuste.
   ○ Cuando un cheque se emite, su monto queda **reservado** en el saldo de la cuenta. Cuando el cheque se cobra efectivamente, la reserva se confirma. Si el cheque se rechaza o se cancela, el monto vuelve al saldo disponible automáticamente.

3. **Chequeras:** Talonarios opcionales asociados a una cuenta bancaria. Cada chequera tiene un rango de números (ej. 100 a 125), un tipo (Inmediato o Diferido) y un nombre. Al cargar un pago por cheque, el usuario puede elegir desde qué chequera lo emite.
   ○ El sistema valida que el número esté **dentro del rango** y que **no haya sido usado** previamente.
   ○ Los números **quedan consumidos** aun si el cheque después se cancela o se rechaza, reflejando que el papel físico ya fue inutilizado.
   ○ La chequera se desactiva automáticamente cuando todos los números se consumieron.

**Migración de Datos**
Todos los pagos por cheque y transferencia ya cargados en el sistema se migran automáticamente a una cuenta bancaria llamada **"Banco Provincia"** que se crea de oficio para cada empresa. De esta forma no se pierde ningún dato histórico y la transición es transparente.

**Experiencia en Pantalla**

● Nuevo módulo **"Tesorería"** en el menú principal con tres secciones: Cajas, Cuentas Bancarias y Chequeras.
● Vista de detalle de cada caja y cuenta con tarjeta de saldo destacada, botones de operación (Agregar / Quitar / Ajustar) e historial completo paginado.
● Vista de detalle de chequera con grid visual de números coloreados según su estado (disponible, usado/cobrado, usado/pendiente, usado/cancelado).
● En los formularios de pago, los autocompletes de Caja, Cuenta Bancaria y Chequera reemplazan al input de texto libre, con validación inline y helper "Números disponibles" cuando aplica.

**Beneficio**
La empresa pasa a tener control real de sus disponibilidades financieras: sabe en cualquier momento cuánto efectivo tiene en cada caja, cuánto saldo bancario disponible (descontando cheques en circulación), y qué números de cheque le quedan en cada talonario. Las operaciones que antes requerían planillas Excel paralelas (control de chequera, conciliación de caja chica) quedan integradas al sistema. Es la base sobre la que se construyen el reporte de Pagos Emitidos y la funcionalidad de Recupero de Valores descriptos a continuación.

-----16. Reporte de Pagos Emitidos y Gestión de Estado de Cheques

**Problema Actual**
Los cheques emitidos se registran en el sistema, pero no existe forma de marcar si un cheque ya fue cobrado por el proveedor, si fue rechazado por el banco o si la empresa lo canceló antes de que se presentara. Tampoco hay un reporte que permita analizar todos los pagos emitidos en un período diferenciados por método (efectivo, transferencia, cheque) y por proveedor. Esto deja a tesorería sin información clave para conciliar los extractos bancarios y para planificar la liquidez.

**Mejora Implementada**
Se incorporan dos mejoras complementarias:

1. **Gestión de Estado del Cheque:** Cada cheque pasa a tener un estado operativo con cinco valores posibles: **Pendiente, Cobrado, Rechazado, Cancelado** y **Vencido** (este último derivado automáticamente cuando un cheque pendiente pasa su fecha de cobro). El estado se gestiona desde el detalle del cheque o desde el reporte mismo, registrando fecha de cambio y usuario responsable.
   ○ El cambio de estado se integra con Tesorería: si un cheque pasa a Rechazado o Cancelado, el monto se libera automáticamente en el saldo de la cuenta bancaria de origen.
   ○ Estados terminales (Cobrado, Cancelado) no se pueden modificar para evitar inconsistencias.

2. **Reporte de Pagos Emitidos:** Sigue la arquitectura de tres capas ya conocida y agrega una particularidad: el usuario puede **alternar entre dos modos de agrupación** (Método → Proveedor o Proveedor → Método) desde el toolbar, sin recargar filtros, según qué pregunta esté respondiendo.

**Estructura del Reporte**

● **Capa 1:** Totales por método de pago (Efectivo, Transferencia, Cheque) o por proveedor, según el modo elegido.
● **Capa 2:** Sub-agrupación cruzada (proveedores dentro de cada método, o métodos dentro de cada proveedor).
● **Capa 3:** Detalle de cada pago con todos los datos relevantes — fecha, monto, comprobantes imputados, cuenta bancaria o caja origen, número de cheque, chequera, estado, fecha de cobro, comentarios.

**Filtros Disponibles**

● Período, proveedores, sectores, métodos de pago.
● Estado del cheque (multiselect, incluye Vencido).
● Cuenta bancaria, caja, chequera (autocompletados de las entidades de Tesorería).
● Rango de montos.
● Toggle "Solo cheques vencidos no cobrados" — clave para detectar problemas operativos.

**Formatos de Salida**
Vista en pantalla, Excel y PDF, con tarjetas KPI específicas de cheques (Total Pendiente, Total Vencido, Total Rechazado).

**Experiencia en Pantalla**
● Nueva entrada **"Pagos Emitidos"** en el menú de Informes.
● Toggle de cambio de modo de agrupación con animación, sin perder filtros.
● Botón inline en cada cheque para cambiar su estado desde el reporte mismo.

**Beneficio**
El área de tesorería tiene por primera vez una vista clara de todos los pagos emitidos y del estado real de cada cheque en circulación. Permite anticipar qué cheques se acercan a su fecha de cobro, identificar rápidamente cheques vencidos sin presentar, conciliar con los extractos bancarios y rendir cuentas a la gerencia con información organizada por proveedor o por método según la conversación.

-----17. Previsiones de Gastos

**Problema Actual**
La planificación semanal de pagos se realiza fuera del sistema, en planillas Excel que se completan manualmente cada lunes y se descartan al cierre del período. No queda registro digital de qué se previó, qué efectivamente se pagó y qué quedó pendiente. La carga es repetitiva: muchos pagos se repiten semana tras semana (sueldos, servicios, alquileres) y hay que volver a tipearlos cada vez. Tampoco hay manera de generar un registro real (un pago de salario, una compra de stock) a partir de lo previsto sin volver a cargar todos los datos.

**Mejora Implementada**
Se incorpora un módulo completo de **Previsiones** que funciona como una planilla digital con período definido, ítems tipificados según la categoría de gasto, integración con los módulos existentes y un sistema de plantillas frecuentes para acelerar la carga de previsiones recurrentes.

**Funcionalidades Principales**

1. **Previsión por Período:** Cada previsión cubre un rango de fechas (típicamente una semana) y agrupa los ítems esperados de gasto. Tiene un nombre, descripción, total previsto y total ya aplicado.

2. **Ítems Tipificados:** Cada ítem corresponde a una de seis categorías: **Salario, Servicio, Reparación, Compra de Stock, Patente** u **Otro** (texto libre con monto, sin relación a entidades). Según el tipo elegido, el formulario solicita los datos correspondientes (empleado, proveedor, vehículo, stock, etc.), garantizando que la previsión esté lista para convertirse en registro real.

3. **Estados Controlados:** Una previsión pasa por tres estados:
   ○ **Borrador:** Editable libremente. Se agregan, modifican o eliminan ítems.
   ○ **Confirmada:** Se cierra a edición pero **se habilita el botón "Aplicar gasto"** sobre cada ítem.
   ○ **Cerrada:** Estado final cuando todos los ítems fueron aplicados u omitidos. Solo lectura.

4. **Aplicar Gasto:** Es la funcionalidad más importante. Sobre cada ítem confirmado, un botón "Aplicar" genera automáticamente el registro real correspondiente — un pago de salario, un pago de servicio, una reparación, una compra de stock o un pago de patente — con todos los datos ya completados desde la previsión. El ítem queda marcado como Aplicado, con link al registro generado y opción de **revertir** la aplicación (que elimina el registro creado y vuelve el ítem a Pendiente).
   ○ Existe también un botón "Aplicar todos los pendientes" para procesar la previsión completa con un solo clic.

5. **Plantillas Frecuentes:** El usuario puede guardar plantillas con los ítems que se repiten cada período (los sueldos del mes, los servicios fijos, los alquileres). Al crear una nueva previsión "desde plantilla", todos los ítems se cargan automáticamente, ajustando las fechas según el rango elegido. Modificar una plantilla no afecta previsiones ya creadas.

6. **Duplicar Previsión:** Convierte una previsión existente en un nuevo borrador con los mismos ítems, ideal cuando la previsión de la semana es prácticamente igual a la anterior con cambios menores.

7. **Importación / Exportación / Plantilla Excel:** Siguiendo el mismo formato que el módulo de Asistencia, el usuario puede:
   ○ Descargar una plantilla Excel vacía con instrucciones y ejemplos.
   ○ Cargar masivamente una previsión desde Excel (validación exhaustiva fila por fila).
   ○ Descargar cualquier previsión en Excel (compatible con la plantilla de importación) o PDF (formato profesional con totales por tipo).

**Experiencia en Pantalla**

● Nuevo módulo **"Previsiones"** en el menú principal.
● Vista de detalle tipo planilla, con grid editable de ítems, agregar fila inline y formulario con campos condicionales según el tipo de gasto.
● Toolbar con botones Confirmar, Aplicar todos, Importar, Descargar, Duplicar.
● Indicadores visuales de estado por fila (Pendiente, Aplicado con link al registro, Omitido con motivo, Solo informativo para gastos libres).

**Beneficio**
Lo que hoy se hace en una planilla Excel descartable se convierte en una herramienta integrada al sistema: lo previsto se materializa en pagos reales sin volver a cargar datos, lo recurrente se carga desde plantillas en segundos, y queda registro histórico de qué se planificó cada semana y cómo se ejecutó. La separación entre "previsión" (intención) y "registro" (ejecución) le permite al usuario ajustar la planificación tantas veces como necesite sin contaminar los reportes financieros.

-----18. Recupero de Valores

**Problema Actual**
Algunas empresas operan con esquemas de **recupero de gastos** con ciertos proveedores: por convenios particulares, cesiones, reintegros o programas de devolución, una parte del neto facturado más el IVA total se recupera y se acredita en una caja específica. Hoy este cálculo se hace manualmente fuera del sistema, llevando una planilla aparte por cada proveedor con recupero, multiplicando los montos uno por uno y registrando los movimientos en la caja a mano. El proceso es propenso a errores y consume tiempo administrativo importante.

**Mejora Implementada**
Se incorpora una funcionalidad de **Recupero de Valores** que automatiza por completo este flujo. Cuando se registra una factura A asociada a un proveedor configurado para recupero, el sistema **calcula y acredita automáticamente** el monto correspondiente en la caja designada, registrando el movimiento con trazabilidad completa.

**Activación de la Funcionalidad**
La función está oculta hasta que la empresa la habilita. Para activarla, el usuario crea un sector o área llamado exactamente **"Recupero"**. A partir de ese momento, en la configuración de ese sector aparece una sección adicional: **"Configuración de Recupero"**, donde se asocian los proveedores que participan del esquema. Esta activación es automática, no requiere intervención técnica.

**Configuración por Proveedor**
Para cada proveedor con recupero, se configura:

● **Porcentaje de Recupero:** El porcentaje que se recupera sobre el neto del comprobante (ejemplo: 90 %).
● **Caja Destino:** La caja en efectivo (de las creadas en Tesorería) donde se acreditará el recupero.

Un proveedor puede aparecer una sola vez en la configuración del sector. La asociación se hace mediante un modal de búsqueda de proveedores.

**Cálculo del Recupero**
Cuando se registra una **factura tipo A** del proveedor configurado, asociada al sector "Recupero", el sistema calcula automáticamente:

> **Monto Recuperado** = (Neto de la factura × Porcentaje configurado) + IVA total de la factura

**Ejemplo:** Factura A de $100 netos + $21 de IVA, proveedor configurado al 90 %.
Recupero = (100 × 90 %) + 21 = **$111** acreditados en la caja designada.

El neto incluye automáticamente todos los gastos asociados a la factura (ítems del comprobante, reparaciones, cargas de combustible, salarios y compras de stock vinculadas), ya que el sistema ya consolida ese total. **El IVA se recupera siempre al 100 %**, independientemente del porcentaje del proveedor.

**Reglas y Validaciones**

● Solo **facturas tipo A** se pueden asociar al sector "Recupero". El sistema bloquea cualquier intento de asociar otro tipo de comprobante con un mensaje claro.
● El proveedor del comprobante debe estar configurado en la sección de Recupero, sino el sistema rechaza la operación con instrucciones para resolver.
● El porcentaje y la caja **se congelan al momento del recupero**: cambios futuros en la configuración del proveedor no afectan recuperos ya generados.

**Reverso Automático**
Si una factura ya recuperada se modifica o se elimina, el sistema **revierte automáticamente** el movimiento de caja generado y, si corresponde, lo regenera con los nuevos montos. Lo mismo ocurre con las **notas de crédito**: una nota de crédito sobre una factura recuperada genera un reverso proporcional en la caja, usando el porcentaje histórico del recupero original. El usuario nunca tiene que intervenir manualmente en estos ajustes.

**Trazabilidad Completa**
Cada operación de recupero queda registrada como un evento auditable, con tipo (Generado, Revertido, Ajustado por Nota de Crédito), fecha, usuario, factura origen, porcentaje aplicado y monto. Una pantalla dedicada de **"Eventos de Recupero"** permite consultar el historial filtrando por proveedor, fecha, factura o caja, para auditoría o conciliación.

**Experiencia en Pantalla**

● Sección "Configuración de Recupero" embebida en el detalle del sector "Recupero" (no aparece para otros sectores).
● Tabla de proveedores con porcentaje, caja, cantidad de eventos y total recuperado histórico por proveedor.
● Modal de alta con autocompletado de proveedor y caja, advertencias informativas sobre el cálculo.
● Pantalla de "Eventos de Recupero" accesible desde el menú solo cuando la funcionalidad está activa, con filtros y badges coloreados por tipo de evento (verde para acreditaciones, rojo para reversos).
● En el formulario de comprobantes, validación inline cuando se intenta asociar una factura no-A o un proveedor no configurado al sector Recupero.

**Beneficio**
Un cálculo recurrente que hoy lleva minutos por factura y es propenso a errores se elimina por completo. La caja siempre refleja el estado correcto del recupero, los movimientos quedan auditables, las modificaciones se ajustan solas y las conciliaciones con los proveedores se simplifican enormemente. Para la empresa que utiliza este esquema es una de las mejoras de mayor impacto operativo del período.
