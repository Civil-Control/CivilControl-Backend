# Feature 7 — Generación de PDF de Órdenes de Pago

## Contexto

Cuando la empresa registra un pago a un proveedor, necesita generar un documento formal impreso: la **Orden de Pago**. Este documento replica el formulario oficial en papel de la empresa e incluye datos del beneficiario, concepto, comprobantes imputados, medio de pago detallado y totales con retenciones.

Esta feature implementa la generación dinámica de un PDF con iText 7 a partir de una entidad `PaymentDetails` existente y su entidad `Tenant` (para los datos de la empresa).

---

## 1. Modelo de Datos (Existente)

### 1.1 Entidad: `PaymentDetails`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador del pago |
| `paymentDate` | `LocalDate` | Fecha del pago |
| `supplier` | FK → `Supplier` | Proveedor beneficiario |
| `amount` | `BigDecimal` | Monto total |
| `comment` | `String` | Concepto del pago |
| `paidDocuments` | ManyToMany → `TransactionalDocument` | Comprobantes imputados al pago |

### 1.2 Entidades de Medio de Pago (OneToOne → PaymentDetails)

| Entidad | Campos clave |
|---|---|
| `CheckPayment` | banco, número de cheque, fecha de emisión, fecha de cobro |
| `TransferPayment` | banco, CBU destino, número de transferencia |
| _(Efectivo)_ | Sin entidad dedicada — se infiere cuando no hay cheque ni transferencia |

### 1.3 Entidad: `Tenant`

Se utiliza para obtener: razón social, CUIT, dirección, condición IVA, logo de la empresa.

---

## 2. Backend — Generador de PDF

### 2.1 Servicio: `PaymentOrderPdfService`

**Archivo:** `service/export/PaymentOrderPdfService.java` (~420 líneas)

Componente Spring (`@Component`) que utiliza iText 7 para generar el PDF.

**Método público:**

```java
public byte[] generate(PaymentDetails payment, Tenant tenant)
```

Retorna el PDF como array de bytes listo para ser enviado al cliente.

**Secciones del PDF:**

| # | Método | Contenido |
|---|---|---|
| 1 | `addHeader()` | Layout de 3 columnas: datos empresa \| "X" \| "Orden de pago" + número, fecha, CUIT |
| 2 | `addBeneficiary()` | Nombre del proveedor, CUIT, dirección, condición IVA |
| 3 | `addConcept()` | Texto del campo `comment` del pago |
| 4 | `addBody()` | Dos bloques lado a lado: tabla de comprobantes imputados + detalle del medio de pago |
| 5 | `addTotals()` | Neto, bruto, retenciones, saldo no imputado |

**Detalle de Comprobantes Imputados:**

La tabla "Comprobantes Imputados" lista cada `TransactionalDocument` vinculado al pago con: tipo, número, fecha, monto.

**Detalle del Medio de Pago:**

Se determina dinámicamente:
- Si existe `CheckPayment` → muestra datos del cheque.
- Si existe `TransferPayment` → muestra datos de la transferencia.
- Caso contrario → indica "Efectivo".

**Diseño visual:** Blanco y negro, bordes finos, tipografía Helvetica estándar. Formato A4 vertical.

### 2.2 Integración en `PaymentService`

**Archivo:** `service/implementation/PaymentService.java`

```java
public byte[] generatePaymentOrderPdf(Long id) {
    PaymentDetails paymentDetails = paymentRepository.findByIdWithPaymentType(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));
    Hibernate.initialize(paymentDetails.getPaidDocuments());
    Hibernate.initialize(paymentDetails.getSupplier());
    var tenant = iTenantService.getEntityById(paymentDetails.getTenantId());
    return paymentOrderPdfService.generate(paymentDetails, tenant);
}
```

> **Nota:** Se usa `Hibernate.initialize()` para cargar las colecciones lazy antes de pasarlas al generador de PDF (que opera fuera de sesión transaccional).

### 2.3 Interfaz: `IPaymentService`

```java
byte[] generatePaymentOrderPdf(Long id);
```

---

## 3. Controller

**Archivo:** `controller/PaymentController.java`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/payments/{id}/payment-order` | `PAYMENT_READ` | Genera y descarga el PDF de la orden de pago |

**Respuesta:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename=orden-de-pago-{id}.pdf`
- Body: `byte[]` del PDF generado

---

## 4. Frontend

### 4.1 Servicio: `PaymentService`

**Archivo:** `domains/finance/payment/services/payment.service.ts`

```typescript
downloadPaymentOrder(id: number): void {
    this.http.get(`${this.baseUrl}/${id}/payment-order`, { responseType: 'blob' })
        .subscribe(blob => {
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `orden-de-pago-${id}.pdf`;
            a.click();
            URL.revokeObjectURL(url);
        });
}
```

### 4.2 Componente: `PaymentDetail`

**Archivo:** `domains/finance/payment/payment-detail/payment-detail.html`

Botón **"Generar Orden de Pago"** visible para usuarios con permiso `PAYMENT_READ`:

```html
<button *appHasPermission="'PAYMENT_READ'" (click)="onGeneratePaymentOrder()">
    Generar Orden de Pago
</button>
```

Al hacer clic, invoca `paymentService.downloadPaymentOrder(id)` que descarga el archivo directamente al navegador.

---

## 5. Permisos

No se agregan permisos nuevos. Reutiliza el permiso existente:

| Constante | Uso |
|---|---|
| `PAYMENT_READ` | Ver pagos y generar PDF de orden de pago |

---

## 6. Migraciones

No requiere migraciones. El endpoint opera sobre las tablas existentes de `payment_details`, `check_payments`, `transfer_payments` y `transactional_documents`.

---

## 7. Dependencias

| Librería | Versión | Uso |
|---|---|---|
| `com.itextpdf:itext-core` | 7.x | Generación de PDF (layout, tablas, celdas) |

---
