# Feature 22 — Verificación de Canales de Notificación

## Contexto

El sistema de notificaciones permite suscribir usuarios a alertas por tres canales: `SYSTEM`, `EMAIL` y `WHATSAPP`. Actualmente no existe garantía de que la dirección de email o el número de WhatsApp registrado en el perfil del usuario sean válidos o pertenezcan al destinatario real.

Esta feature agrega un flujo de verificación por OTP (código de un solo uso) para los canales `EMAIL` y `WHATSAPP`, y bloquea la suscripción a esos canales hasta que el usuario los haya verificado.

---

## Flujo de usuario

### Verificación desde el perfil

```
Usuario abre /perfil
        │
        ▼
Sección "Canales de Notificación"
  · Email:     [maxim@ejemplo.com]  [ Sin verificar → Verificar ]
  · WhatsApp:  [+5491155554444]     [ Sin verificar → Verificar ]
        │
        ▼
Click "Verificar" (email o WhatsApp)
        │
        ├─ Si email/número no está cargado → toast: "Completá el campo primero"
        ├─ Backend envía OTP al canal seleccionado
        ▼
Modal OTP: ingresa el código de 6 dígitos recibido
        │
        ├─ Código correcto → canal marcado como verificado ✓
        └─ Código incorrecto / expirado → mensaje de error
```

### Bloqueo en formulario de nueva suscripción

```
Usuario abre formulario de suscripción
        │
        ▼
Chips de canal:
  [● Sistema]  [✉ Email 🔒]  [💬 WhatsApp 🔒]
                  ↓
  Chip deshabilitado + texto:
  "Este usuario no tiene [Email/WhatsApp] verificado."
  [→ Ir a Mi Perfil]    (solo si es la propia cuenta)
  "El usuario debe verificar su canal desde su perfil."
  (si el admin está asignando a otro usuario)
```

---

## Modelo de datos

### Cambios en tabla `users`

| Campo nuevo        | Tipo      | Default | Descripción                     |
|--------------------|-----------|---------|--------------------------------|
| `email_verified`   | `BOOLEAN` | `FALSE` | Email confirmado por OTP       |
| `whatsapp_verified`| `BOOLEAN` | `FALSE` | WhatsApp confirmado por OTP    |

**Regla de reset:** si el usuario cambia su `email` → `email_verified = false`. Si cambia `whatsapp_number` → `whatsapp_verified = false`.

### Nueva tabla `user_verification_tokens`

| Campo        | Tipo          | Descripción                             |
|--------------|---------------|-----------------------------------------|
| `id`         | `BIGSERIAL PK`|                                         |
| `user_id`    | `BIGINT FK`   | Referencia a `users(id)`               |
| `channel`    | `VARCHAR(20)` | `'EMAIL'` o `'WHATSAPP'`              |
| `code`       | `VARCHAR(10)` | Código OTP de 6 dígitos (plain text)   |
| `expires_at` | `TIMESTAMP`   | Expira 10 minutos desde la emisión     |
| `created_at` | `TIMESTAMP`   | `DEFAULT NOW()`                        |

> Un usuario puede tener un solo token activo por canal. Al emitir un nuevo OTP se elimina el anterior para ese canal.

---

## Migración de base de datos — `V81`

```sql
-- V81__add_channel_verification.sql

ALTER TABLE users
    ADD COLUMN email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN whatsapp_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_verification_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    channel    VARCHAR(20) NOT NULL,
    code       VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_uvt_user_channel
    ON user_verification_tokens (user_id, channel);
```

---

## Entidades y repositorios Backend

### Nueva entidad: `UserVerificationToken`

```java
@Entity
@Table(name = "user_verification_tokens")
public class UserVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationChannel channel;
    @Column(nullable = false, length = 10) private String code;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
```

> No extiende `TenantEntity` — los tokens son globales por usuario (un usuario = una fila de `users`, un tenant).

### Modificaciones a `User`

```java
@Column(name = "email_verified", nullable = false)
private Boolean emailVerified = false;

@Column(name = "whatsapp_verified", nullable = false)
private Boolean whatsappVerified = false;
```

### Nuevo repositorio: `UserVerificationTokenRepository`

```java
Optional<UserVerificationToken> findByUserIdAndChannel(Long userId, NotificationChannel channel);
void deleteByUserIdAndChannel(Long userId, NotificationChannel channel);
```

---

## DTOs

### `VerifyChannelRequestDTO`
```java
public record VerifyChannelRequestDTO(
    @NotBlank @Size(min = 6, max = 6) String code
) {}
```

---

## Servicio: `UserVerificationService`

### Método `sendOtp(Long userId, NotificationChannel channel)`

```
1. Validar que el canal sea EMAIL o WHATSAPP (SYSTEM no aplica).
2. Cargar User por userId.
3. Verificar que el campo de destino exista:
     EMAIL      → user.email no null/blank
     WHATSAPP   → user.whatsappNumber no null/blank
4. Generar código: 6 dígitos aleatorios.
5. Eliminar token previo del mismo (userId, channel).
6. Persistir nuevo UserVerificationToken con expiresAt = now() + 10 min.
7. Enviar el código al canal correspondiente:
     EMAIL    → Resend con template de verificación (texto plano HTML simple)
     WHATSAPP → WhatsApp Cloud API con template "channel_verification_otp"
                (ver nota sobre templates)
```

### Método `confirmOtp(Long userId, NotificationChannel channel, String code)`

```
1. Buscar token por (userId, channel).
2. Si no existe → lanzar excepción "Código inválido o expirado".
3. Si expiresAt < now() → eliminar token → excepción "Código expirado".
4. Si token.code != code → excepción "Código incorrecto".
5. Actualizar User: emailVerified = true  (si channel = EMAIL)
                    whatsappVerified = true (si channel = WHATSAPP)
6. Eliminar el token.
7. Retornar DTO actualizado del usuario.
```

> **Nota sobre el template de WhatsApp:** La verificación por WhatsApp requiere un template pre-aprobado en Meta Business (`channel_verification_otp`). El mensaje podría ser: *"Tu código de verificación de CivilControl es {{1}}. Expira en 10 minutos. No lo compartas."* Este template debe crearse y aprobarse en Meta antes de activar el envío real. Hasta entonces, el `sendOtp` para WHATSAPP loguea el código y retorna 200 OK (modo desarrollo).

---

## Controlador: `UserVerificationController`

Base: `/api/v1/users/me/verify`  
Seguridad: usuario autenticado (sin permiso especial — solo puede verificar su propia cuenta).

| Método | Ruta                  | Descripción                                      |
|--------|-----------------------|--------------------------------------------------|
| `POST` | `/email/send`         | Emite OTP y lo envía al email del usuario        |
| `POST` | `/email/confirm`      | Valida OTP, marca `email_verified = true`        |
| `POST` | `/whatsapp/send`      | Emite OTP y lo envía al WhatsApp del usuario     |
| `POST` | `/whatsapp/confirm`   | Valida OTP, marca `whatsapp_verified = true`     |

Todos retornan `200 OK` con el `UserResponseDTO` actualizado (para que el frontend refresque el estado).

---

## Validación en `NotificationSubscriptionService`

En `createSubscription(dto)`, después del check de duplicado y antes de persistir:

```
Para cada canal en dto.channels():
  Si canal = EMAIL:
    Cargar User por dto.userId()
    Si !user.emailVerified → throw NotificationSubscriptionNotValidException(
        "El usuario no tiene el canal de email verificado.")
  Si canal = WHATSAPP:
    Cargar User por dto.userId()
    Si !user.whatsappVerified → throw NotificationSubscriptionNotValidException(
        "El usuario no tiene el número de WhatsApp verificado.")
  Si canal = SYSTEM:
    Sin validación (siempre disponible)
```

---

## Cambios en `UserService`

En el método de actualización de usuario, detectar cambios en campos sensibles:

```java
if (dto.email() != null && !dto.email().equals(entity.getEmail())) {
    entity.setEmail(dto.email());
    entity.setEmailVerified(false);
}
if (dto.whatsappNumber() != null && !dto.whatsappNumber().equals(entity.getWhatsappNumber())) {
    entity.setWhatsappNumber(dto.whatsappNumber());
    entity.setWhatsappVerified(false);
}
```

---

## Cambios en `UserResponseDTO`

Agregar campos al record existente:

```java
Boolean emailVerified,
Boolean whatsappVerified,
String  whatsappNumber
```

---

## Frontend — Modelo

### Cambios en `user.model.ts`

```typescript
export interface User {
  // ... campos existentes ...
  whatsappNumber?: string | null;
  emailVerified?: boolean;
  whatsappVerified?: boolean;
}
```

---

## Frontend — Perfil (`/perfil`)

### Nueva sección: "Canales de Notificación"

Ubicada entre "Información Personal" y "Ubicación por Defecto".

**Contenido:**

```
┌─────────────────────────────────────────────────────────────────┐
│ Canales de Notificación                                          │
│ Verificá los canales que querés usar para recibir alertas.       │
├─────────────────────────────────────────────────────────────────┤
│ ✉  Email                                                         │
│    maxim@ejemplo.com          ✓ Verificado           [—]         │
│                                                                  │
│ 💬 WhatsApp                                                      │
│    [+5491155554444        ]   ✗ Sin verificar  [Verificar]       │
└─────────────────────────────────────────────────────────────────┘
```

**Componente:** `VerificationSectionComponent` (inline en `profile-page.html`) o como bloque dentro del `profile-page.ts`.

**Estado local:**
```typescript
showOtpModal  = signal(false);
otpChannel    = signal<'email' | 'whatsapp' | null>(null);
otpCode       = signal('');
otpSending    = signal(false);
otpConfirming = signal(false);
otpError      = signal('');
```

**Flujo del modal OTP:**
1. Click "Verificar" → `sendOtp(channel)` → `otpSending = true` → backend `POST /verify/{channel}/send`
2. Si OK → `showOtpModal = true`, `otpSending = false`
3. Usuario ingresa 6 dígitos → click "Confirmar" → `confirmOtp(channel, code)`
4. Si OK → `user.emailVerified = true` (o `whatsappVerified`) → `showOtpModal = false` → toast "Canal verificado"
5. Si error → `otpError = mensaje del backend`

**WhatsApp number edit:** El campo de WhatsApp es editable en esta sección (no en Información Personal). Al guardar un número nuevo → `whatsappVerified` se resetea (el backend lo indica en el `UserResponseDTO` de respuesta).

---

## Frontend — Formulario de suscripción

### Cambios en `notification-subscription-form.ts`

El formulario ya tiene acceso al `userId` objetivo. Necesita saber si ese usuario tiene los canales verificados.

**Nuevo signal/computed:**
```typescript
targetUserVerification = signal<{ emailVerified: boolean; whatsappVerified: boolean } | null>(null);
```

Al seleccionar un usuario (o al inicializar con la propia cuenta), cargar el objeto `User` del picker que ya trae los campos `emailVerified` / `whatsappVerified`.

**Computed para bloqueo de canal:**
```typescript
isChannelLocked = (ch: NotificationChannel): boolean => {
  if (ch === NotificationChannel.SYSTEM) return false;
  const v = this.targetUserVerification();
  if (!v) return true;
  if (ch === NotificationChannel.EMAIL)    return !v.emailVerified;
  if (ch === NotificationChannel.WHATSAPP) return !v.whatsappVerified;
  return false;
};
```

**En el template** — chip de canal bloqueado:

```html
<button class="nf__chip"
        [class.nf__chip--active]="selectedChannels().includes(ch)"
        [class.nf__chip--locked]="isChannelLocked(ch)"
        [disabled]="isChannelLocked(ch)"
        (click)="!isChannelLocked(ch) && toggleChannel(ch)">
  {{ channelLabel(ch) }}
  @if (isChannelLocked(ch)) { 🔒 }
</button>
```

**Mensaje bajo los chips (si hay algún canal bloqueado):**

```html
@if (hasLockedChannels()) {
  <p class="nf__channel-warning">
    Algunos canales no están disponibles porque el usuario no los ha verificado.
    @if (isSelf()) {
      <a routerLink="/perfil">Verificar en Mi Perfil →</a>
    } @else {
      El usuario debe verificarlo desde su perfil.
    }
  </p>
}
```

**Estilo CSS (`.nf__chip--locked`):**
```scss
&--locked {
  opacity: 0.45;
  cursor: not-allowed;
  border-color: #e5e7eb;
  background: #f9fafb;
  color: #9ca3af;
}
```

---

## Nuevo servicio HTTP: `UserVerificationHttpService`

```typescript
@Injectable({ providedIn: 'root' })
export class UserVerificationHttpService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/users/me/verify`;

  sendEmailOtp()           { return this.http.post(`${this.base}/email/send`, {}); }
  confirmEmailOtp(code: string)    { return this.http.post<User>(`${this.base}/email/confirm`, { code }); }
  sendWhatsappOtp()        { return this.http.post(`${this.base}/whatsapp/send`, {}); }
  confirmWhatsappOtp(code: string) { return this.http.post<User>(`${this.base}/whatsapp/confirm`, { code }); }
}
```

---

## Permisos nuevos

Ninguno. La verificación es una acción propia del usuario autenticado — no requiere permisos adicionales. El endpoint usa `SecurityContextHolder` directamente (igual que el perfil actual).

---

## Resumen de archivos nuevos

| Archivo | Tipo |
|---|---|
| `V81__add_channel_verification.sql` | Migración DB |
| `UserVerificationToken.java` | Entidad |
| `UserVerificationTokenRepository.java` | Repositorio |
| `UserVerificationService.java` | Servicio |
| `UserVerificationController.java` | Controlador |
| `VerifyChannelRequestDTO.java` | DTO |
| `user-verification-http.service.ts` | Servicio HTTP Angular |

## Resumen de archivos modificados

| Archivo | Cambio |
|---|---|
| `User.java` | +`emailVerified`, +`whatsappVerified` |
| `UserResponseDTO.java` | +`emailVerified`, +`whatsappVerified`, +`whatsappNumber` |
| `UserService.java` | Reset de flags al cambiar email/whatsapp |
| `NotificationSubscriptionService.java` | Validación de verificación al crear suscripción |
| `user.model.ts` | +`emailVerified`, +`whatsappVerified`, +`whatsappNumber` |
| `profile-page.ts` / `.html` / `.scss` | Nueva sección + modal OTP |
| `notification-subscription-form.ts` / `.html` / `.scss` | Chips bloqueados + mensaje |

---

## Dependencias externas y restricciones

- **Email OTP**: Resend ya está configurado. Listo para implementar.
- **WhatsApp OTP**: Requiere template `channel_verification_otp` aprobado en Meta Business. Hasta su aprobación, el backend registra el código en logs (modo desarrollo) y retorna 200 OK igualmente. El frontend mostrará el campo de código en ambos casos.
- **Expiración de tokens**: Los tokens expirados se eliminan en el `confirmOtp`. No se implementa limpieza periódica (cron) en esta fase — el volumen es bajo.
