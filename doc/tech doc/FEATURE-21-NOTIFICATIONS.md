# Feature 21 — Sistema de Notificaciones (WebSocket y Email)

## Resumen

Sistema de **notificaciones suscribibles multicanal** que alerta a los usuarios sobre fechas de vencimiento próximas en cinco módulos del sistema: VTV de vehículos, cheques pendientes, pólizas de seguro, afectaciones de servicios y contratos de obra.

Cada usuario (o un usuario con permisos) puede suscribirse a un sujeto concreto o a todos los del tipo, configurar cuántos avisos desea recibir y con cuánta antelación, y elegir los canales de entrega: notificación en sistema (WebSocket) y/o email.

**Herramientas externas:**
- **WebSocket:** STOMP sobre SockJS con autenticación JWT en el handshake.
- **Email:** [Resend](https://resend.com) — SDK Java oficial, 3.000 emails/mes gratis, deliverability superior a Gmail SMTP.

**Dependencias del proyecto:**
- Ninguna feature anterior es prerrequisito directo.

---

## 1. Modelo de Datos

### 1.1 Enum: `NotificationSubjectType`

**Ubicación:** `model/enums/notification/NotificationSubjectType.java`

```java
public enum NotificationSubjectType {
    VEHICLE_VTV        ("VTV de vehículo"),
    CHECK_PAYMENT      ("Cheque pendiente"),
    INSURANCE_POLICY   ("Póliza de seguro"),
    SERVICE_ASSIGNMENT ("Afectación de servicio"),
    WORK_CONTRACT      ("Contrato de obra");

    private final String displayName;

    NotificationSubjectType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
```

### 1.2 Enum: `NotificationChannel`

**Ubicación:** `model/enums/notification/NotificationChannel.java`

```java
public enum NotificationChannel {
    SYSTEM,
    EMAIL
}
```

### 1.3 Enum: `NotificationDeliveryStatus`

**Ubicación:** `model/enums/notification/NotificationDeliveryStatus.java`

```java
public enum NotificationDeliveryStatus {
    SENT,
    FAILED
}
```

### 1.4 Entidad: `NotificationSubscription`

**Ubicación:** `model/entity/notification/NotificationSubscription.java`

```java
@Entity
@Table(name = "notification_subscriptions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class NotificationSubscription extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subscribed_by_user_id", nullable = false)
    private Long subscribedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 30)
    private NotificationSubjectType subjectType;

    // null = suscripción a TODOS los sujetos de ese tipo en el tenant
    @Column(name = "subject_id")
    private Long subjectId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "notification_subscription_channels",
                     joinColumns = @JoinColumn(name = "subscription_id"))
    @Column(name = "channel", length = 20, nullable = false)
    @Builder.Default
    private Set<NotificationChannel> channels = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotificationAlert> alerts = new ArrayList<>();
}
```

**Nota sobre la unicidad:** la unicidad se delega al motor de base de datos mediante un índice `NULLS NOT DISTINCT` (PostgreSQL 15+), que sí considera dos NULLs como iguales. El `@UniqueConstraint` JPA se omite de la entidad para evitar la generación de un índice estándar que no aplicaría este comportamiento. El servicio realiza una única comprobación de duplicado (sin bifurcación `isNull`) apoyándose en que el motor aplica la restricción correctamente.

### 1.5 Entidad: `NotificationAlert`

**Ubicación:** `model/entity/notification/NotificationAlert.java`

Esta entidad es hija de `NotificationSubscription` y no extiende `TenantEntity` (la tenencia la hereda a través de la suscripción padre).

```java
@Entity
@Table(name = "notification_alerts")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class NotificationAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private NotificationSubscription subscription;

    @Column(name = "days_before_alert", nullable = false)
    private Integer daysBeforeAlert;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
```

### 1.6 Entidad: `NotificationLog`

**Ubicación:** `model/entity/notification/NotificationLog.java`

Tabla de auditoría de envíos. También se usa como mecanismo de deduplicación (evita reenvíos si el scheduler corre más de una vez en el mismo día).

```java
@Entity
@Table(name = "notification_logs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class NotificationLog extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 30)
    private NotificationSubjectType subjectType;

    @Column(name = "subject_id")
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private NotificationDeliveryStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
```

### 1.7 Record interno: `SubjectDueDateInfo`

**Ubicación:** `model/entity/notification/SubjectDueDateInfo.java`

Record de uso interno (no es entidad JPA) que cada `NextDueDateResolver` devuelve:

```java
public record SubjectDueDateInfo(
    Long subjectId,
    String displayName,
    LocalDate dueDate
) {}
```

### 1.8 Record interno: `NotificationPayload`

**Ubicación:** `model/entity/notification/NotificationPayload.java`

```java
public record NotificationPayload(
    NotificationSubjectType subjectType,
    Long subjectId,
    String subjectDisplayName,
    LocalDate dueDate,
    int daysUntilDue,
    Long userId,
    // Mapa canal → dirección de contacto. Aísla al payload de los canales concretos:
    // agregar un canal nuevo = una línea en el scheduler, cero cambios aquí.
    // EMAIL     → dirección de correo
    // SYSTEM    → no requiere entrada (el routing se hace por userId vía WebSocket)
    Map<NotificationChannel, String> channelAddresses
) {}
```

---

## 2. Migración SQL

```sql
-- Tabla principal de suscripciones
CREATE TABLE notification_subscriptions (
    id                    BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
    tenant_id             BIGINT        NOT NULL,
    user_id               BIGINT        NOT NULL,
    subscribed_by_user_id BIGINT        NOT NULL,
    subject_type          VARCHAR(30)   NOT NULL,
    subject_id            BIGINT,
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_notif_sub_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Unicidad real con NULLs tratados como iguales (requiere PostgreSQL 15+)
CREATE UNIQUE INDEX idx_unique_subscription
    ON notification_subscriptions (tenant_id, user_id, subject_type, subject_id)
    NULLS NOT DISTINCT;

CREATE INDEX idx_notif_sub_tenant_user      ON notification_subscriptions(tenant_id, user_id);
CREATE INDEX idx_notif_sub_tenant_type      ON notification_subscriptions(tenant_id, subject_type);
CREATE INDEX idx_notif_sub_active_deleted   ON notification_subscriptions(tenant_id, active, deleted);

-- Canales de cada suscripción (ElementCollection)
CREATE TABLE notification_subscription_channels (
    subscription_id BIGINT      NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    PRIMARY KEY (subscription_id, channel),
    CONSTRAINT fk_nsc_subscription FOREIGN KEY (subscription_id) REFERENCES notification_subscriptions(id) ON DELETE CASCADE
);

-- Avisos configurados por suscripción
CREATE TABLE notification_alerts (
    id              BIGINT   NOT NULL GENERATED ALWAYS AS IDENTITY,
    subscription_id BIGINT   NOT NULL,
    days_before_alert INTEGER NOT NULL,
    active          BOOLEAN  NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_subscription FOREIGN KEY (subscription_id) REFERENCES notification_subscriptions(id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_alert_sub ON notification_alerts(subscription_id);

-- Log de envíos (auditoría + deduplicación)
CREATE TABLE notification_logs (
    id              BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
    tenant_id       BIGINT        NOT NULL,
    subscription_id BIGINT        NOT NULL,
    alert_id        BIGINT        NOT NULL,
    subject_type    VARCHAR(30)   NOT NULL,
    subject_id      BIGINT,
    channel         VARCHAR(20)   NOT NULL,
    sent_at         TIMESTAMP     NOT NULL,
    log_date        DATE          NOT NULL,
    status          VARCHAR(10)   NOT NULL,
    error_message   VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_notif_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_notif_log_dedup    ON notification_logs(subscription_id, alert_id, subject_id, log_date);
CREATE INDEX idx_notif_log_user     ON notification_logs(tenant_id, subscription_id, channel, log_date DESC);
```

---

## 3. Nuevas Dependencias (pom.xml)

Agregar dentro del bloque `<dependencies>`:

```xml
<!-- WebSocket / STOMP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Email transaccional — Resend -->
<dependency>
    <groupId>com.resend</groupId>
    <artifactId>resend-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

---

## 4. Configuración

### 4.1 `application.yml`

Agregar las siguientes secciones (los valores reales van en variables de entorno):

```yaml
resend:
  api-key: ${RESEND_API_KEY}
  from-address: ${RESEND_FROM_ADDRESS:noreply@civilcontrol.app}
  from-name: ${RESEND_FROM_NAME:CivilControl}
```

### 4.2 `WebSocketConfig`

**Ubicación:** `config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtInterceptor jwtInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtInterceptor);
    }
}
```

### 4.3 `WebSocketJwtInterceptor`

**Ubicación:** `config/WebSocketJwtInterceptor.java`

Extrae el JWT del header `Authorization` en el frame STOMP CONNECT y establece el `Principal` del mensaje como el `userId`. Este Principal es el que usa `SimpMessagingTemplate.convertAndSendToUser()` para enrutar los mensajes.

```java
@Component
@RequiredArgsConstructor
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    String username = jwtService.extractUsername(jwt);
                    Long tenantId = jwtService.extractTenantId(jwt);
                    TenantContext.setCurrentTenant(tenantId);
                    User user = userRepository.findByCredentials_UsernameAndDeletedFalse(username)
                            .orElseThrow();
                    accessor.setUser(() -> user.getId().toString());
                } catch (Exception ignored) {
                    // JWT inválido: el broker rechazará la conexión
                }
            }
        }
        return message;
    }
}
```

**Nota:** El método `findByCredentials_UsernameAndDeletedFalse` ya debe existir en `UserRepository` (o uno equivalente que resuelva el user por username). Verificar el nombre exacto del método antes de implementar.

### 4.4 `AsyncConfig`

**Ubicación:** `config/AsyncConfig.java`

Configura el executor dedicado para el despacho asíncrono de notificaciones, aislando esos hilos del pool general de Spring.

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationTaskExecutor")
    public ThreadPoolTaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-dispatch-");
        executor.initialize();
        return executor;
    }
}
```

### 4.5 `ResendConfig`

**Ubicación:** `config/ResendConfig.java`

```java
@Configuration
public class ResendConfig {

    @Value("${resend.api-key}")
    private String apiKey;

    @Bean
    public Resend resendClient() {
        return new Resend(apiKey);
    }
}
```

---

## 5. DTOs

### 5.1 `NotificationAlertDTO`

**Ubicación:** `model/dto/notification/NotificationAlertDTO.java`

```java
public record NotificationAlertDTO(

    @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
    @Min(value = 1, message = "{notification.alert.daysBeforeAlert.min}", groups = {OnCreate.class, OnUpdate.class})
    @Max(value = 365, message = "{notification.alert.daysBeforeAlert.max}", groups = {OnCreate.class, OnUpdate.class})
    Integer daysBeforeAlert,

    boolean active
) {}
```

### 5.2 `NotificationSubscriptionDTO`

**Ubicación:** `model/dto/notification/NotificationSubscriptionDTO.java`

```java
public record NotificationSubscriptionDTO(

    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    Long userId,

    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    NotificationSubjectType subjectType,

    // null = suscripción wildcard a todos los sujetos del tipo en el tenant
    Long subjectId,

    @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
    @NotEmpty(message = "{validation.notEmpty}", groups = {OnCreate.class, OnUpdate.class})
    Set<NotificationChannel> channels,

    @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
    @NotEmpty(message = "{validation.notEmpty}", groups = {OnCreate.class, OnUpdate.class})
    @Valid
    List<NotificationAlertDTO> alerts,

    boolean active
) {}
```

### 5.3 `NotificationSubscriptionResponseDTO`

**Ubicación:** `model/dto/notification/NotificationSubscriptionResponseDTO.java`

```java
public record NotificationSubscriptionResponseDTO(
    Long id,
    Long userId,
    String userFullName,
    Long subscribedByUserId,
    NotificationSubjectType subjectType,
    Long subjectId,
    String subjectDisplayName,
    Set<NotificationChannel> channels,
    List<NotificationAlertResponseDTO> alerts,
    boolean active
) {
    public record NotificationAlertResponseDTO(
        Long id,
        Integer daysBeforeAlert,
        boolean active
    ) {}
}
```

### 5.4 `NotificationInboxItemDTO`

**Ubicación:** `model/dto/notification/NotificationInboxItemDTO.java`

Usado por el endpoint de bandeja de entrada del canal SYSTEM.

```java
public record NotificationInboxItemDTO(
    Long logId,
    NotificationSubjectType subjectType,
    String subjectTypeDisplayName,
    Long subjectId,
    String subjectDisplayName,
    LocalDate dueDate,
    int daysUntilDue,
    LocalDateTime sentAt
) {}
```

---

## 6. Mapper

### 6.1 `NotificationSubscriptionMapper`

**Ubicación:** `model/mapper/NotificationSubscriptionMapper.java`

```java
@Mapper(componentModel = "spring")
public interface NotificationSubscriptionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "channels", ignore = true)
    @Mapping(target = "alerts", ignore = true)
    @Mapping(target = "subscribedByUserId", ignore = true)
    NotificationSubscription toEntity(NotificationSubscriptionDTO dto);

    @Mapping(target = "userFullName", ignore = true)
    @Mapping(target = "subjectDisplayName", ignore = true)
    NotificationSubscriptionResponseDTO toResponseDto(NotificationSubscription entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    NotificationAlert alertToEntity(NotificationAlertDTO dto);

    NotificationSubscriptionResponseDTO.NotificationAlertResponseDTO alertToResponseDto(NotificationAlert alert);
}
```

Los campos `userFullName` y `subjectDisplayName` se pueblan manualmente en el servicio después del mapeo base (requieren queries adicionales).

---

## 7. Repositorios

### 7.1 `NotificationSubscriptionRepository`

**Ubicación:** `repository/NotificationSubscriptionRepository.java`

```java
@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

    List<NotificationSubscription> findAllByDeletedFalseAndActiveTrue();

    List<NotificationSubscription> findAllByUserIdAndDeletedFalse(Long userId);

    // Verificación de duplicado unificada — el índice NULLS NOT DISTINCT garantiza
    // que subjectId=null también sea tratado como valor único por el motor.
    boolean existsByUserIdAndSubjectTypeAndSubjectIdAndDeletedFalse(
            Long userId, NotificationSubjectType subjectType, Long subjectId);

    @Modifying
    @Query("UPDATE NotificationSubscription s SET s.deleted = true WHERE s.userId = :userId")
    void markDeletedByUserId(@Param("userId") Long userId);
}
```

### 7.2 `NotificationAlertRepository`

**Ubicación:** `repository/NotificationAlertRepository.java`

```java
@Repository
public interface NotificationAlertRepository extends JpaRepository<NotificationAlert, Long> {
    // Sin métodos adicionales: los alerts siempre se acceden a través de NotificationSubscription.alerts
}
```

### 7.3 `NotificationLogRepository`

**Ubicación:** `repository/NotificationLogRepository.java`

```java
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    // Carga bulk de claves de ciclo ya enviadas (status SENT) de vencimientos vigentes.
    // Clave por ciclo: (subscriptionId, alertId, subjectId, dueDate). Evita N+1 en el scheduler
    // y permite envío único por ciclo dentro de la ventana de recuperación.
    @Query("SELECT CONCAT(str(l.subscriptionId), '_', str(l.alertId), '_', " +
           "COALESCE(str(l.subjectId), 'null'), '_', str(l.dueDate)) " +
           "FROM NotificationLog l " +
           "WHERE l.tenantId = :tenantId AND l.status = 'SENT' AND l.dueDate >= :fromDate")
    Set<String> findSentCycleKeysForTenant(@Param("tenantId") Long tenantId,
                                           @Param("fromDate") LocalDate fromDate);

    // Bandeja de entrada del canal SYSTEM para un usuario (obtenido vía subscriptionId en subquery o join)
    @Query("SELECT l FROM NotificationLog l " +
           "WHERE l.subscriptionId IN :subscriptionIds " +
           "AND l.channel = 'SYSTEM' " +
           "AND l.status = 'SENT' " +
           "ORDER BY l.sentAt DESC")
    List<NotificationLog> findSystemInboxBySubscriptionIds(
            @Param("subscriptionIds") List<Long> subscriptionIds,
            Pageable pageable);
}
```

### 7.4 Modificación a `TenantRepository`

Agregar el método que el scheduler necesita para iterar todos los tenants activos sin filtro de tenencia (el aspecto `TenantFilterAspect` excluye `TenantRepository` explícitamente):

```java
List<Tenant> findAllByDeletedFalseAndActiveTrue();
```

---

## 8. Patrones Strategy

### 8.1 Interface `NextDueDateResolver`

**Ubicación:** `service/notification/resolver/NextDueDateResolver.java`

```java
public interface NextDueDateResolver {

    NotificationSubjectType getSubjectType();

    // Devuelve info de vencimiento de un sujeto concreto (0 o 1 elemento)
    List<SubjectDueDateInfo> resolveForId(Long subjectId);

    // Devuelve info de todos los sujetos activos del tenant actual (para suscripciones wildcard)
    List<SubjectDueDateInfo> resolveAll();
}
```

### 8.2 `VehicleVtvDueDateResolver`

**Ubicación:** `service/notification/resolver/VehicleVtvDueDateResolver.java`

```java
@Component
@RequiredArgsConstructor
public class VehicleVtvDueDateResolver implements NextDueDateResolver {

    private final VehicleRepository vehicleRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.VEHICLE_VTV; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long vehicleId) {
        return vehicleRepository.findByIdAndDeletedFalse(vehicleId)
                .filter(v -> v.getVtvExpirationDate() != null)
                .map(v -> new SubjectDueDateInfo(v.getId(), buildDisplayName(v), v.getVtvExpirationDate()))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return vehicleRepository.findAllByDeletedFalse().stream()
                .filter(v -> v.getVtvExpirationDate() != null)
                .map(v -> new SubjectDueDateInfo(v.getId(), buildDisplayName(v), v.getVtvExpirationDate()))
                .toList();
    }

    private String buildDisplayName(Vehicle v) {
        String brand = v.getBrand() != null ? v.getBrand() + " " : "";
        String model = v.getModel() != null ? v.getModel() + " " : "";
        return brand + model + v.getLicensePlate() + " — VTV";
    }
}
```

**Nota:** Si `VehicleRepository` no tiene `findAllByDeletedFalse()` ni `findByIdAndDeletedFalse()`, verificar los métodos existentes y adaptar el nombre. El patrón existe en otros repositorios del proyecto.

### 8.3 `CheckPaymentDueDateResolver`

**Ubicación:** `service/notification/resolver/CheckPaymentDueDateResolver.java`

Solo los cheques con `status = PENDIENTE` son relevantes (los terminales COBRADO/CANCELADO no necesitan aviso).

```java
@Component
@RequiredArgsConstructor
public class CheckPaymentDueDateResolver implements NextDueDateResolver {

    private final CheckPaymentRepository checkPaymentRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.CHECK_PAYMENT; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long checkId) {
        return checkPaymentRepository.findById(checkId)
                .filter(c -> c.getStatus() == CheckStatus.PENDIENTE && c.getDueDate() != null && !c.getDeleted())
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getDueDate()))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return checkPaymentRepository.findAllByStatusAndDeletedFalse(CheckStatus.PENDIENTE).stream()
                .filter(c -> c.getDueDate() != null)
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getDueDate()))
                .toList();
    }

    private String buildDisplayName(CheckPayment c) {
        String num = c.getCheckNumber() != null ? " #" + c.getCheckNumber() : "";
        return "Cheque" + num + " — " + c.getBankAccount().getName();
    }
}
```

**Nota:** Agregar `findAllByStatusAndDeletedFalse(CheckStatus status)` a `CheckPaymentRepository` si no existe.

### 8.4 `InsurancePolicyDueDateResolver`

**Ubicación:** `service/notification/resolver/InsurancePolicyDueDateResolver.java`

Calcula la **próxima fecha de pago** de la póliza a partir de `effectiveFrom`, `paymentFrequency` y `periodicDueDay`.

```java
@Component
@RequiredArgsConstructor
public class InsurancePolicyDueDateResolver implements NextDueDateResolver {

    private final InsurancePolicyRepository insurancePolicyRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.INSURANCE_POLICY; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long policyId) {
        return insurancePolicyRepository.findById(policyId)
                .flatMap(this::computeNextPaymentDate)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return insurancePolicyRepository.findAllByDeletedFalseAndPolicyStatus(PolicyStatus.ACTIVO).stream()
                .flatMap(p -> computeNextPaymentDate(p).stream())
                .toList();
    }

    private Optional<SubjectDueDateInfo> computeNextPaymentDate(InsurancePolicy policy) {
        if (policy.getDeleted() || policy.getPolicyStatus() != PolicyStatus.ACTIVO) return Optional.empty();

        LocalDate today = LocalDate.now();
        LocalDate effectiveTo = policy.getEffectiveTo();
        if (today.isAfter(effectiveTo)) return Optional.empty();

        if (policy.getPaymentFrequency() == PaymentFrequency.PAGO_UNICO) {
            LocalDate dueDate = policy.getEffectiveFrom();
            if (!dueDate.isBefore(today)) {
                return Optional.of(new SubjectDueDateInfo(policy.getId(), buildDisplayName(policy), dueDate));
            }
            return Optional.empty();
        }

        int intervalMonths = switch (policy.getPaymentFrequency()) {
            case MENSUAL    -> 1;
            case BIMESTRAL  -> 2;
            case TRIMESTRAL -> 3;
            case SEMI_ANUAL -> 6;
            case ANUAL      -> 12;
            default         -> 1;
        };

        int dueDay = policy.getPeriodicDueDay() != null
                ? policy.getPeriodicDueDay()
                : policy.getEffectiveFrom().getDayOfMonth();

        LocalDate cursor = policy.getEffectiveFrom();
        while (!cursor.isAfter(effectiveTo)) {
            LocalDate paymentDate = cursor.withDayOfMonth(Math.min(dueDay, cursor.lengthOfMonth()));
            if (!paymentDate.isBefore(today) && !paymentDate.isAfter(effectiveTo)) {
                return Optional.of(new SubjectDueDateInfo(policy.getId(), buildDisplayName(policy), paymentDate));
            }
            cursor = cursor.plusMonths(intervalMonths);
        }
        return Optional.empty();
    }

    private String buildDisplayName(InsurancePolicy p) {
        return "Póliza " + p.getPolicyNumber() + " — " + p.getPolicyType().name();
    }
}
```

**Nota:** Agregar `findAllByDeletedFalseAndPolicyStatus(PolicyStatus status)` a `InsurancePolicyRepository` si no existe.

### 8.5 `ServiceAssignmentDueDateResolver`

**Ubicación:** `service/notification/resolver/ServiceAssignmentDueDateResolver.java`

Para periodicidades regulares calcula el próximo vencimiento a partir de `estimatedDueDay`. Para `IRREGULAR` usa la próxima fecha de `specificDueDates` que sea >= hoy.

```java
@Component
@RequiredArgsConstructor
public class ServiceAssignmentDueDateResolver implements NextDueDateResolver {

    private final ServiceAssignmentRepository serviceAssignmentRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.SERVICE_ASSIGNMENT; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long assignmentId) {
        return serviceAssignmentRepository.findById(assignmentId)
                .flatMap(this::computeNextDueDate)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return serviceAssignmentRepository.findAllByDeletedFalse().stream()
                .flatMap(a -> computeNextDueDate(a).stream())
                .toList();
    }

    private Optional<SubjectDueDateInfo> computeNextDueDate(ServiceAssignment assignment) {
        if (assignment.isDeleted()) return Optional.empty();
        LocalDate today = LocalDate.now();

        if (assignment.getPeriodicity() == Periodicity.IRREGULAR) {
            return assignment.getSpecificDueDates().stream()
                    .map(SpecificDueDate::getDueDate)
                    .filter(d -> !d.isBefore(today))
                    .min(Comparator.naturalOrder())
                    .map(d -> new SubjectDueDateInfo(assignment.getId(), buildDisplayName(assignment), d));
        }

        if (assignment.getEstimatedDueDay() == null) return Optional.empty();

        int intervalMonths = switch (assignment.getPeriodicity()) {
            case MENSUAL    -> 1;
            case BIMESTRAL  -> 2;
            case TRIMESTRAL -> 3;
            case SEMESTRAL  -> 6;
            case ANUAL      -> 12;
            default         -> 1;
        };

        int dueDay = assignment.getEstimatedDueDay();
        LocalDate candidate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));
        if (candidate.isBefore(today)) {
            candidate = candidate.plusMonths(intervalMonths)
                                 .withDayOfMonth(Math.min(dueDay, candidate.plusMonths(intervalMonths).lengthOfMonth()));
        }
        return Optional.of(new SubjectDueDateInfo(assignment.getId(), buildDisplayName(assignment), candidate));
    }

    private String buildDisplayName(ServiceAssignment a) {
        return a.getServiceSupplier().getName() + " — " + a.getServiceType().name();
    }
}
```

**Nota:** Verificar el nombre del getter de `name` en `ServiceSupplier` antes de implementar.

### 8.6 `WorkContractDueDateResolver`

**Ubicación:** `service/notification/resolver/WorkContractDueDateResolver.java`

```java
@Component
@RequiredArgsConstructor
public class WorkContractDueDateResolver implements NextDueDateResolver {

    private final WorkContractRepository workContractRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.WORK_CONTRACT; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long contractId) {
        return workContractRepository.findById(contractId)
                .filter(c -> !c.getDeleted() && c.getStatus() == WorkContractStatus.ACTIVO && c.getEndDate() != null)
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getEndDate()))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return workContractRepository.findAllByDeletedFalseAndStatus(WorkContractStatus.ACTIVO).stream()
                .filter(c -> c.getEndDate() != null)
                .map(c -> new SubjectDueDateInfo(c.getId(), buildDisplayName(c), c.getEndDate()))
                .toList();
    }

    private String buildDisplayName(WorkContract c) {
        return "Contrato " + c.getContractNumber() + " — " + c.getClient().getName();
    }
}
```

**Nota:** Agregar `findAllByDeletedFalseAndStatus(WorkContractStatus status)` a `WorkContractRepository` si no existe. Verificar el getter `getName()` en `Client`.

### 8.7 Interface `NotificationSender`

**Ubicación:** `service/notification/sender/NotificationSender.java`

```java
public interface NotificationSender {
    NotificationChannel getChannel();
    void send(NotificationPayload payload);
}
```

### 8.8 `EmailNotificationSender`

**Ubicación:** `service/notification/sender/EmailNotificationSender.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final Resend resendClient;

    @Value("${resend.from-address}")
    private String fromAddress;

    @Value("${resend.from-name}")
    private String fromName;

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }

    @Override
    public void send(NotificationPayload payload) {
        String email = payload.channelAddresses().get(NotificationChannel.EMAIL);
        if (email == null || email.isBlank()) {
            log.warn("Email notification skipped for userId={}: no email address", payload.userId());
            return;
        }

        String subject = "CivilControl — " + payload.subjectDisplayName() + " vence en " + payload.daysUntilDue() + " días";
        String body = buildHtmlBody(payload);

        SendEmailRequest request = SendEmailRequest.builder()
                .from(fromName + " <" + fromAddress + ">")
                .to(email)
                .subject(subject)
                .html(body)
                .build();

        resendClient.emails().send(request);
    }

    private String buildHtmlBody(NotificationPayload p) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto">
              <h2 style="color:#1a56db">CivilControl — Aviso de vencimiento</h2>
              <p><strong>%s</strong></p>
              <p>Fecha de vencimiento: <strong>%s</strong></p>
              <p>Días restantes: <strong>%d</strong></p>
              <hr/>
              <small style="color:#6b7280">Este mensaje fue generado automáticamente. No responder.</small>
            </div>
            """.formatted(p.subjectDisplayName(),
                          p.dueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                          p.daysUntilDue());
    }
}
```

### 8.10 `WebSocketNotificationSender`

**Ubicación:** `service/notification/sender/WebSocketNotificationSender.java`

```java
@Component
@RequiredArgsConstructor
public class WebSocketNotificationSender implements NotificationSender {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.SYSTEM; }

    @Override
    public void send(NotificationPayload payload) {
        messagingTemplate.convertAndSendToUser(
                payload.userId().toString(),
                "/queue/notifications",
                payload
        );
    }
}
```

### 8.11 `NotificationSenderRegistry`

**Ubicación:** `service/notification/sender/NotificationSenderRegistry.java`

Componente central de registro de senders. Reemplaza la inyección frágil de `Map<NotificationChannel, NotificationSender>` (que Spring resolvería por bean name, no por `getChannel()`). Al iniciar el contexto, recolecta automáticamente todos los beans `NotificationSender` presentes, construye el mapa indexado por canal y falla rápido (`IllegalStateException`) si un valor del enum no tiene sender registrado.

**Contrato de extensibilidad:** agregar un canal nuevo = crear un `@Component` que implemente `NotificationSender`. El registry lo detecta sin ningún cambio adicional.

```java
@Component
public class NotificationSenderRegistry {

    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationSenderRegistry(List<NotificationSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::getChannel,
                        Function.identity()));
        Arrays.stream(NotificationChannel.values()).forEach(channel -> {
            if (!this.senders.containsKey(channel)) {
                throw new IllegalStateException(
                        "No NotificationSender registered for channel: " + channel);
            }
        });
    }

    public NotificationSender get(NotificationChannel channel) {
        return senders.get(channel);
    }
}
```

---

## 9. Servicios

### 9.1 `INotificationSubscriptionService`

**Ubicación:** `service/port/INotificationSubscriptionService.java`

```java
public interface INotificationSubscriptionService {
    NotificationSubscriptionResponseDTO createSubscription(NotificationSubscriptionDTO dto);
    NotificationSubscriptionResponseDTO getSubscriptionById(Long id);
    NotificationSubscriptionResponseDTO updateSubscription(Long id, NotificationSubscriptionDTO dto);
    void deleteSubscription(Long id);
    List<NotificationSubscriptionResponseDTO> getMySubscriptions();
    List<NotificationSubscriptionResponseDTO> getUserSubscriptions(Long userId);
    List<NotificationInboxItemDTO> getInbox(int limit);
}
```

### 9.2 `NotificationSubscriptionService`

**Ubicación:** `service/implementation/NotificationSubscriptionService.java`

`@Service @RequiredArgsConstructor`. Inyecciones: `NotificationSubscriptionRepository`, `NotificationLogRepository`, `UserRepository`, `NotificationSubscriptionMapper`, `MessageSourceHelper`, `Map<NotificationSubjectType, NextDueDateResolver> resolvers`.

**Autorización en `createSubscription`:**

```java
Long currentUserId = getCurrentUserId();  // extraído de SecurityContextHolder
if (dto.userId().equals(currentUserId)) {
    checkPermission(AppPermissions.NOTIFICATION_SELF_SUBSCRIBE);
} else {
    checkPermission(AppPermissions.NOTIFICATION_ASSIGN_OTHERS);
}
```

**Validaciones en CREATE:**

1. `dto.userId()` debe existir y no estar deleted → `UserNotFoundException`
2. Si `dto.subjectId() != null`, el sujeto debe existir en el tenant actual (delegar verificación al resolver correspondiente: `resolver.resolveForId(dto.subjectId())` debe devolver al menos un elemento)
3. Verificar que no existe ya una suscripción activa para la misma combinación `(userId, subjectType, subjectId)`:
   - Llamar a `existsByUserIdAndSubjectTypeAndSubjectIdAndDeletedFalse()` en todos los casos (incluido `subjectId = null`). El índice `NULLS NOT DISTINCT` del motor garantiza que dos wildcard del mismo tipo también colisionen.
   - Si ya existe → `NotificationSubscriptionNotValidException` (mensaje `notification.subscription.duplicate`)
4. `dto.channels()` no puede estar vacío (Bean Validation ya lo valida, reforzar en servicio)
5. `dto.alerts()` no puede estar vacío; cada `daysBeforeAlert` debe ser único dentro de la lista → si hay duplicados → `NotificationSubscriptionNotValidException` (mensaje `notification.subscription.alerts.duplicate`)

**Flujo de `createSubscription`:**

```
validarPermisos(dto.userId())
→ validarUsuario(dto.userId())
→ validarSubjetIdExiste(dto)
→ validarNoDuplicado(dto)
→ validarAlertsUnicos(dto.alerts())
→ entity = mapper.toEntity(dto)
→ entity.setSubscribedByUserId(currentUserId)
→ entity.setChannels(dto.channels())
→ List<NotificationAlert> alerts = dto.alerts().stream()
       .map(a -> mapper.alertToEntity(a))
       .peek(a -> a.setSubscription(entity))
       .toList()
→ entity.setAlerts(alerts)
→ saved = repository.save(entity)
→ return enrichAndMap(saved)
```

**Método `enrichAndMap`:** convierte la entidad al DTO de respuesta y añade `userFullName` (fetching `User` por `userId`) y `subjectDisplayName` (llamando `resolver.resolveForId(subjectId)` y tomando el `displayName`; para suscripciones wildcard `subjectId == null`, `subjectDisplayName = "Todos (" + subjectType.getDisplayName() + ")"`).

**Actualización en `updateSubscription`:** solo se puede actualizar `channels`, `alerts` y `active`. `userId`, `subjectType` y `subjectId` son inmutables después de la creación.

Al actualizar `alerts`: la lista recibida **reemplaza completa** la lista existente (orphanRemoval se encarga de eliminar los que ya no están).

**Soft delete en cascada desde `User`:**

Cuando un usuario es eliminado lógicamente, el scheduler no debe procesar suscripciones huérfanas. Agregar en `NotificationSubscriptionService` un listener que reaccione al evento de dominio `UserDeletedEvent` (publicado por `UserService.deleteUser()` vía `ApplicationEventPublisher`):

```java
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
public void onUserDeleted(UserDeletedEvent event) {
    subscriptionRepository.markDeletedByUserId(event.userId());
}
```

`UserDeletedEvent` es un record simple `public record UserDeletedEvent(Long userId) {}`. Publicarlo en `UserService` con `eventPublisher.publishEvent(new UserDeletedEvent(user.getId()))` justo antes del `save()` del soft-delete.

**Método `getInbox`:**
```java
Long currentUserId = getCurrentUserId();
List<Long> subscriptionIds = subscriptionRepository
    .findAllByUserIdAndDeletedFalse(currentUserId)
    .stream().map(NotificationSubscription::getId).toList();

List<NotificationLog> logs = logRepository.findSystemInboxBySubscriptionIds(
    subscriptionIds, PageRequest.of(0, limit));

// Mapear a NotificationInboxItemDTO calculando daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), log.dueDate)
// El dueDate no está en NotificationLog: necesitamos resolverlo
```

**Problema:** el `NotificationLog` no almacena la fecha de vencimiento (sería redundante). Para el inbox, calcular los días restantes requiere llamar de nuevo al resolver. Alternativa: guardar también `due_date` en `NotificationLog`.

**Decisión:** agregar `@Column(name = "due_date") private LocalDate dueDate` a `NotificationLog` (y al script SQL `ALTER TABLE notification_logs ADD COLUMN due_date DATE`). El `NotificationDispatchService` lo poblará al crear el log.

### 9.3 `NotificationDispatchService`

**Ubicación:** `service/notification/NotificationDispatchService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final NotificationSenderRegistry senderRegistry;
    private final NotificationLogRepository logRepository;

    // @Async libera el hilo del scheduler de inmediato.
    // @Transactional garantiza que cada invocación asíncrona maneje su propia transacción
    // al guardar los logs, evitando contención en la tabla notification_logs.
    @Async("notificationTaskExecutor")
    @Transactional
    public void dispatch(NotificationPayload payload,
                         Set<NotificationChannel> channels,
                         Long subscriptionId,
                         Long alertId) {
        for (NotificationChannel channel : channels) {
            NotificationDeliveryStatus status = NotificationDeliveryStatus.SENT;
            String errorMsg = null;
            try {
                senderRegistry.get(channel).send(payload);
            } catch (Exception e) {
                status = NotificationDeliveryStatus.FAILED;
                errorMsg = e.getMessage();
                log.error("Notification dispatch failed [channel={}, subscriptionId={}, subjectId={}]: {}",
                          channel, subscriptionId, payload.subjectId(), e.getMessage());
            }
            logRepository.save(NotificationLog.builder()
                    .subscriptionId(subscriptionId)
                    .alertId(alertId)
                    .subjectType(payload.subjectType())
                    .subjectId(payload.subjectId())
                    .channel(channel)
                    .sentAt(LocalDateTime.now())
                    .logDate(LocalDate.now())
                    .dueDate(payload.dueDate())
                    .status(status)
                    .errorMessage(errorMsg)
                    .build());
        }
    }
}
```

**Nota:** El `NotificationSenderRegistry` (sección 8.11) gestiona la inyección y validación del mapa de senders. `NotificationDispatchService` no conoce los canales concretos; solo delega al registry.

### 9.4 `NotificationSchedulerService`

**Ubicación:** `service/notification/NotificationSchedulerService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchedulerService {

    private final TenantRepository tenantRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository logRepository;
    private final UserRepository userRepository;
    private final Map<NotificationSubjectType, NextDueDateResolver> resolvers;
    private final NotificationDispatchService dispatchService;

    @Scheduled(cron = "0 0 8 * * *")
    @Async
    public void runDailyCheck() {
        log.info("NotificationScheduler: starting daily run");
        tenantRepository.findAllByDeletedFalseAndActiveTrue().forEach(tenant -> {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                processTenant(tenant.getId());
            } catch (Exception e) {
                log.error("NotificationScheduler: error processing tenant {}: {}", tenant.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        });
        log.info("NotificationScheduler: daily run complete");
    }

    private void processTenant(Long tenantId) {
        LocalDate today = LocalDate.now();

        // Una sola query por tenant. Carga las claves de ciclo ya enviadas (status SENT)
        // de vencimientos vigentes (dueDate >= hoy). Evita N+1 y garantiza envío único por ciclo.
        Set<String> sentCycleKeys = logRepository.findSentCycleKeysForTenant(tenantId, today);

        List<NotificationSubscription> subscriptions = subscriptionRepository.findAllByDeletedFalseAndActiveTrue();

        for (NotificationSubscription sub : subscriptions) {
            NextDueDateResolver resolver = resolvers.get(sub.getSubjectType());

            List<SubjectDueDateInfo> subjects = sub.getSubjectId() != null
                    ? resolver.resolveForId(sub.getSubjectId())
                    : resolver.resolveAll();

            for (SubjectDueDateInfo info : subjects) {
                for (NotificationAlert alert : sub.getAlerts()) {
                    if (!alert.isActive()) continue;
                    processAlert(sub, alert, info, today, sentCycleKeys);
                }
            }
        }
    }

    private void processAlert(NotificationSubscription sub, NotificationAlert alert,
                               SubjectDueDateInfo info, LocalDate today,
                               Set<String> sentCycleKeys) {
        // Ventana de recuperación: dispara desde triggerDate hasta dueDate.
        // La deduplicación por ciclo (dueDate) garantiza un único envío aunque el
        // scheduler falle o la suscripción se cree con el vencimiento ya dentro de rango.
        LocalDate triggerDate = info.dueDate().minusDays(alert.getDaysBeforeAlert());
        if (today.isBefore(triggerDate) || today.isAfter(info.dueDate())) return;

        String dedupKey = sub.getId() + "_" + alert.getId() + "_"
                + (info.subjectId() != null ? info.subjectId() : "null")
                + "_" + info.dueDate();
        if (sentCycleKeys.contains(dedupKey)) return;

        userRepository.findById(sub.getUserId()).ifPresent(user -> {
            // Construir el mapa de direcciones de contacto por canal.
            // Agregar un canal nuevo = añadir una línea aquí. Sin más cambios.
            Map<NotificationChannel, String> channelAddresses = new EnumMap<>(NotificationChannel.class);
            channelAddresses.put(NotificationChannel.EMAIL, user.getEmail());

            NotificationPayload payload = new NotificationPayload(
                    sub.getSubjectType(),
                    info.subjectId(),
                    info.displayName(),
                    info.dueDate(),
                    alert.getDaysBeforeAlert(),
                    user.getId(),
                    channelAddresses
            );
            dispatchService.dispatch(payload, sub.getChannels(), sub.getId(), alert.getId());
        });
    }
}
```

---

## 10. Endpoints REST

### 10.1 `NotificationSubscriptionController`

**Ubicación:** `controller/NotificationSubscriptionController.java`

`@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor`

`@Tag(name = "Notifications", description = "Gestión de suscripciones a notificaciones y bandeja de entrada.")`

| Método | Path | Permiso | Status | Descripción |
|---|---|---|---|---|
| `POST`   | `/api/v1/notifications/subscriptions`           | `NOTIFICATION_SELF_SUBSCRIBE` o `NOTIFICATION_ASSIGN_OTHERS` (resuelto en servicio) | 201 | Crear suscripción |
| `GET`    | `/api/v1/notifications/subscriptions/me`        | Autenticado | 200 | Suscripciones del usuario autenticado |
| `GET`    | `/api/v1/notifications/subscriptions/user/{id}` | `NOTIFICATION_ASSIGN_OTHERS` | 200 | Suscripciones de otro usuario |
| `GET`    | `/api/v1/notifications/subscriptions/{id}`      | Autenticado | 200 | Detalle de una suscripción |
| `PATCH`  | `/api/v1/notifications/subscriptions/{id}`      | Autenticado (dueño) o `NOTIFICATION_ASSIGN_OTHERS` | 200 | Actualizar canales/alertas/activo |
| `DELETE` | `/api/v1/notifications/subscriptions/{id}`      | Autenticado (dueño) o `NOTIFICATION_ASSIGN_OTHERS` | 204 | Eliminar suscripción (soft delete) |
| `GET`    | `/api/v1/notifications/inbox`                   | Autenticado | 200 | Bandeja de entrada SYSTEM del usuario autenticado |

**Parámetros del `GET /inbox`:**

`limit` (default 30, max 100) — cantidad de notificaciones a devolver, ordenadas por fecha descendente.

**Control de acceso en PATCH y DELETE:** el servicio verifica que el `userId` de la suscripción sea el del usuario autenticado, o que el usuario tenga `NOTIFICATION_ASSIGN_OTHERS`. Si ninguna condición se cumple → `ForbiddenException` (HTTP 403).

---

## 11. Permisos

**Archivo:** `model/constants/AppPermissions.java`

Agregar las dos constantes nuevas:

```java
public static final String NOTIFICATION_SELF_SUBSCRIBE = "NOTIFICATION_SELF_SUBSCRIBE";
public static final String NOTIFICATION_ASSIGN_OTHERS  = "NOTIFICATION_ASSIGN_OTHERS";
```

El `PermissionSeeder` los sincronizará automáticamente en la base de datos al arrancar. Asignar ambos permisos al rol administrador del sistema.

---

## 12. Mensajes i18n

```properties
# ====== Feature 21 - Notifications ======
notification.subscription.notFound=Suscripción de notificación con id {0} no encontrada
notification.subscription.duplicate=Ya existe una suscripción activa para este usuario, tipo y sujeto
notification.subscription.alerts.duplicate=Los avisos no pueden tener días de antelación repetidos
notification.subscription.noChannels=Debe seleccionar al menos un canal de notificación
notification.subscription.noAlerts=Debe configurar al menos un aviso
notification.subscription.forbidden=No tiene permiso para modificar esta suscripción
notification.subscription.immutableFields=Los campos userId, subjectType y subjectId no pueden modificarse
notification.subscription.subjectNotFound=El sujeto indicado no existe o no pertenece al tenant actual
notification.alert.daysBeforeAlert.min=La antelación del aviso debe ser de al menos 1 día
notification.alert.daysBeforeAlert.max=La antelación del aviso no puede superar los 365 días
```

---

## 13. Frontend

### 13.1 Dependencias npm

Ejecutar en `CivilControl-Frontend/`:

```bash
npm install @stomp/stompjs sockjs-client
npm install --save-dev @types/sockjs-client
```

### 13.2 Modelos TypeScript

**Ubicación:** `src/app/shared/models/notification.model.ts`

```typescript
export enum NotificationSubjectType {
  VEHICLE_VTV        = 'VEHICLE_VTV',
  CHECK_PAYMENT      = 'CHECK_PAYMENT',
  INSURANCE_POLICY   = 'INSURANCE_POLICY',
  SERVICE_ASSIGNMENT = 'SERVICE_ASSIGNMENT',
  WORK_CONTRACT      = 'WORK_CONTRACT',
}

export const NotificationSubjectTypeLabels: Record<NotificationSubjectType, string> = {
  [NotificationSubjectType.VEHICLE_VTV]:        'VTV de vehículo',
  [NotificationSubjectType.CHECK_PAYMENT]:      'Cheque pendiente',
  [NotificationSubjectType.INSURANCE_POLICY]:   'Póliza de seguro',
  [NotificationSubjectType.SERVICE_ASSIGNMENT]: 'Afectación de servicio',
  [NotificationSubjectType.WORK_CONTRACT]:      'Contrato de obra',
};

export enum NotificationChannel {
  SYSTEM = 'SYSTEM',
  EMAIL  = 'EMAIL',
}

export interface NotificationAlertDto {
  daysBeforeAlert: number;
  active: boolean;
}

export interface NotificationAlertResponse {
  id: number;
  daysBeforeAlert: number;
  active: boolean;
}

export interface NotificationSubscription {
  id?: number;
  userId: number;
  userFullName?: string;
  subscribedByUserId?: number;
  subjectType: NotificationSubjectType;
  subjectId?: number | null;
  subjectDisplayName?: string;
  channels: NotificationChannel[];
  alerts: NotificationAlertDto[];
  active: boolean;
}

export interface NotificationSubscriptionResponse extends NotificationSubscription {
  id: number;
  alerts: NotificationAlertResponse[];
}

export interface NotificationInboxItem {
  logId: number;
  subjectType: NotificationSubjectType;
  subjectTypeDisplayName: string;
  subjectId: number;
  subjectDisplayName: string;
  dueDate: string;
  daysUntilDue: number;
  sentAt: string;
}

// Payload recibido via WebSocket (canal SYSTEM)
export interface WsNotificationPayload {
  subjectType: NotificationSubjectType;
  subjectId: number;
  subjectDisplayName: string;
  dueDate: string;
  daysUntilDue: number;
  userId: number;
}
```

Agregar en `src/app/shared/models/index.ts`:
```typescript
export * from './notification.model';
```

### 13.3 `WsNotificationService`

**Ubicación:** `src/app/core/services/ws-notification.service.ts`

Gestiona la conexión WebSocket STOMP y expone las notificaciones en tiempo real como Observable.

```typescript
@Injectable({ providedIn: 'root' })
export class WsNotificationService {

  private client: Client | null = null;
  private notificationsSubject = new Subject<WsNotificationPayload>();
  notifications$ = this.notificationsSubject.asObservable();

  constructor(private authService: AuthService) {}

  connect(): void {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      // beforeConnect se ejecuta antes de cada intento de conexión (inicial y reconexiones).
      // Obtiene un token válido, renovándolo si expiró, antes de enviar el CONNECT frame.
      // Esto resuelve el caso en que el JWT caduca mientras la pestaña está abierta:
      // si el socket se cae, la reconexión automática usará el token renovado.
      beforeConnect: async () => {
        const token = await firstValueFrom(this.authService.getValidToken());
        this.client!.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onConnect: () => {
        this.client!.subscribe('/user/queue/notifications', (msg) => {
          const payload: WsNotificationPayload = JSON.parse(msg.body);
          this.notificationsSubject.next(payload);
        });
      },
      onStompError: (frame) => {
        log.error('WS STOMP error:', frame.headers['message']);
      },
    });
    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }
}
```

**Requisito en `AuthService`:** exponer `getValidToken(): Observable<string>`, que devuelve el token actual si sigue siendo válido, o invoca el flujo de refresh y devuelve el nuevo token. El interceptor HTTP de la app ya debe implementar lógica equivalente; se puede reutilizar.

El servicio se conecta llamando `connect()` en el flujo de autenticación (sin recibir el token como parámetro, lo resuelve internamente) y se desconecta en logout.

### 13.4 `NotificationHttpService`

**Ubicación:** `src/app/domains/notifications/services/notification.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class NotificationHttpService {

  private readonly base = '/notifications';

  constructor(private http: HttpClient) {}

  createSubscription(dto: NotificationSubscription): Observable<NotificationSubscriptionResponse> {
    return this.http.post<NotificationSubscriptionResponse>(`${this.base}/subscriptions`, dto);
  }

  getMySubscriptions(): Observable<NotificationSubscriptionResponse[]> {
    return this.http.get<NotificationSubscriptionResponse[]>(`${this.base}/subscriptions/me`);
  }

  getUserSubscriptions(userId: number): Observable<NotificationSubscriptionResponse[]> {
    return this.http.get<NotificationSubscriptionResponse[]>(`${this.base}/subscriptions/user/${userId}`);
  }

  updateSubscription(id: number, dto: Partial<NotificationSubscription>): Observable<NotificationSubscriptionResponse> {
    return this.http.patch<NotificationSubscriptionResponse>(`${this.base}/subscriptions/${id}`, dto);
  }

  deleteSubscription(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/subscriptions/${id}`);
  }

  getInbox(limit = 30): Observable<NotificationInboxItem[]> {
    return this.http.get<NotificationInboxItem[]>(`${this.base}/inbox`, { params: { limit } });
  }
}
```

### 13.5 Componente `notification-bell`

**Ubicación:** `src/app/shared/components/notification-bell/`

Componente standalone que se coloca en el header/navbar de la aplicación.

**Estado del componente (`.ts`):**

```typescript
unreadCount = signal(0);
items = signal<NotificationInboxItem[]>([]);
open = signal(false);
```

**Comportamiento:**
- Al inicializar: llama `notificationHttpService.getInbox(10)` y puebla `items`.
- Suscribe a `wsNotificationService.notifications$` via `takeUntilDestroyed(destroyRef)`. Al recibir un payload WebSocket:
  1. Incrementa `unreadCount`.
  2. Prepend del nuevo item en `items` (construir un `NotificationInboxItem` parcial con los datos del payload).
- Al hacer click en la campana: alterna `open` y resetea `unreadCount = 0`.
- Al hacer click en un item: navega al módulo correspondiente según `subjectType` (usando el `Router`).

**Template HTML** — mostrar:
- Icono campana con badge rojo cuando `unreadCount() > 0`.
- Dropdown con lista de `items()`, mostrando `subjectDisplayName`, `daysUntilDue` y `sentAt` (formato `dd/MM/yyyy HH:mm`).
- Si `items().length === 0`: texto "Sin notificaciones recientes".
- Enlace "Ver todas" que navega a la página de gestión de suscripciones.

### 13.6 Gestión de suscripciones

**Ubicación:** `src/app/domains/notifications/`

Estructura:

```
notifications/
├── services/
│   └── notification.service.ts         — HTTP (ya descrito en 13.4)
├── notification-settings-page/
│   ├── notification-settings-page.ts
│   ├── notification-settings-page.html
│   └── notification-settings-page.scss
└── notification-subscription-form/
    ├── notification-subscription-form.ts
    ├── notification-subscription-form.html
    └── notification-subscription-form.scss
```

**`notification-settings-page`:** muestra la lista de suscripciones del usuario autenticado agrupadas por `subjectType`. Botón "Nueva suscripción" abre el formulario. Cada fila muestra canales (íconos), cantidad de avisos y botón eliminar.

**`notification-subscription-form`:** formulario de creación/edición.

Campos:
- `subjectType` — select con los 5 tipos (required)
- `subjectId` — select dinámico según el tipo elegido, cargado via la API de referencia del módulo correspondiente. Toggle "Todos" que setea `subjectId = null`.
- `channels` — checkboxes para SYSTEM y EMAIL.
- `alerts` — lista dinámica. Botón "Agregar aviso" añade un input numérico de días. Mínimo 1 aviso. Botón `×` por fila para eliminar.

**Validaciones frontend:**
- Al menos un canal seleccionado.
- Al menos un aviso configurado.
- Cada aviso debe tener `daysBeforeAlert >= 1` y `<= 365`.
- No puede haber dos avisos con el mismo `daysBeforeAlert` en la misma suscripción.

**Routing:** path `notificaciones` bajo el módulo de configuración/perfil del usuario o como ítem dedicado en el menú de navegación, con permiso `NOTIFICATION_SELF_SUBSCRIBE`.

---

## 14. Decisiones Técnicas

| Decisión | Elección | Motivo |
|---|---|---|
| Canal email | Resend SDK | 3.000 emails/mes gratis, mejor deliverability que Gmail SMTP, sin gestión de infraestructura |
| WebSocket | STOMP + SockJS | Standard de facto en Spring Boot; SockJS provee fallback HTTP para redes restrictivas |
| Autenticación WebSocket | JWT en header CONNECT | Misma estrategia que la API REST; sin cookies ni sesiones adicionales |
| Canales por suscripción | Mismos canales para todos los avisos | Simplifica la UX; cambiar de canal requiere editar la suscripción, no cada aviso |
| `subjectId = null` | Suscripción wildcard a todos los sujetos del tipo | Permite alertas globales ("avisar de cualquier VTV que venza") sin crear N suscripciones |
| Scheduler | Ventana `triggerDate ≤ hoy ≤ dueDate` + dedup por ciclo | Se recupera de corridas perdidas o suscripciones creadas con el vencimiento ya dentro de rango; el log por ciclo (`dueDate`) evita reenvíos |
| Deduplicación | `NotificationLog` con clave de ciclo `(subscriptionId, alertId, subjectId, dueDate)` | Un único envío por ciclo de vencimiento dentro de la ventana de recuperación; también sirve como auditoría |
| `dueDate` en `NotificationLog` | Campo persistido | Evita re-resolver la fecha en el inbox; dato histórico útil aunque el sujeto cambie |
| `NotificationAlert` sin `TenantEntity` | Entidad hijo simple | Siempre se accede a través de su suscripción padre; la tenencia queda garantizada por cascada |
| Inbox SYSTEM | Endpoint REST + WebSocket | WebSocket para tiempo real; REST para histórico en login (usuario que estuvo offline) |
| `userId` como `Long` (no FK) | Sin `@ManyToOne` a `User` | Desacopla el módulo de notificaciones de seguridad; consistente con campos de auditoría del proyecto |
| Registro de senders | `NotificationSenderRegistry` (lista → mapa explícito) | Falla en startup si un canal del enum no tiene sender registrado; evita la inyección frágil de `Map<K,V>` por bean name de Spring. Agregar canal = crear un `@Component`, cero cambios al registro |
| Datos de contacto en payload | `Map<NotificationChannel, String> channelAddresses` | Evita añadir campos al record `NotificationPayload` por cada canal nuevo. Agregar canal = una línea en el scheduler al construir el payload |

---

## 15. Checklist de Implementación

### Backend

- [ ] Agregar 2 dependencias en `pom.xml` (websocket, resend)
- [ ] Crear enum `NotificationSubjectType.java`
- [ ] Crear enum `NotificationChannel.java`
- [ ] Crear enum `NotificationDeliveryStatus.java`
- [ ] Crear record `SubjectDueDateInfo.java`
- [ ] Crear record `NotificationPayload.java`
- [ ] Crear entidad `NotificationSubscription.java`
- [ ] Crear entidad `NotificationAlert.java`
- [ ] Crear entidad `NotificationLog.java` (incluir campo `dueDate`)
- [ ] Ejecutar migración SQL completa (4 tablas nuevas)
- [ ] Agregar `findAllByDeletedFalseAndActiveTrue()` a `TenantRepository`
- [ ] Crear `NotificationSubscriptionRepository.java`
- [ ] Crear `NotificationAlertRepository.java`
- [ ] Crear `NotificationLogRepository.java`
- [ ] Agregar métodos faltantes a repositorios existentes (`VehicleRepository`, `CheckPaymentRepository`, `InsurancePolicyRepository`, `WorkContractRepository`, `ServiceAssignmentRepository`)
- [ ] Agregar `markDeletedByUserId(@Param Long userId)` a `NotificationSubscriptionRepository`
- [ ] Crear `NotificationAlertDTO.java`
- [ ] Crear `NotificationSubscriptionDTO.java`
- [ ] Crear `NotificationSubscriptionResponseDTO.java`
- [ ] Crear `NotificationInboxItemDTO.java`
- [ ] Crear `NotificationSubscriptionMapper.java`
- [ ] Crear `WebSocketConfig.java`
- [ ] Crear `WebSocketJwtInterceptor.java`
- [ ] Crear `AsyncConfig.java` (con `@EnableAsync` y bean `notificationTaskExecutor`)
- [ ] Crear `ResendConfig.java`
- [ ] Agregar sección `resend` a `application.yml`
- [ ] Excluir `NotificationAlertRepository` del `TenantFilterAspect` (no extiende `TenantEntity`) agregando `&& !execution(* PSG.backEnd.repository.NotificationAlertRepository.*(..))` al pointcut
- [ ] Crear interface `NextDueDateResolver.java`
- [ ] Crear `VehicleVtvDueDateResolver.java`
- [ ] Crear `CheckPaymentDueDateResolver.java`
- [ ] Crear `InsurancePolicyDueDateResolver.java`
- [ ] Crear `ServiceAssignmentDueDateResolver.java`
- [ ] Crear `WorkContractDueDateResolver.java`
- [ ] Crear interface `NotificationSender.java`
- [ ] Crear `EmailNotificationSender.java`
- [ ] Crear `WebSocketNotificationSender.java`
- [ ] Crear `NotificationSenderRegistry.java`
- [ ] Crear `INotificationSubscriptionService.java`
- [ ] Crear `NotificationSubscriptionService.java` (incluye `@TransactionalEventListener` para `UserDeletedEvent`)
- [ ] Crear `UserDeletedEvent.java` y publicarlo en `UserService.deleteUser()`
- [ ] Crear `NotificationDispatchService.java` (con `@Async("notificationTaskExecutor")` + `@Transactional`)
- [ ] Crear `NotificationSchedulerService.java` (con carga bulk de `sentToday` por tenant)
- [ ] Crear `NotificationSubscriptionController.java`
- [ ] Agregar `NOTIFICATION_SELF_SUBSCRIBE` y `NOTIFICATION_ASSIGN_OTHERS` a `AppPermissions.java`
- [ ] Agregar mensajes i18n a `messages.properties`
- [ ] Crear excepciones `NotificationSubscriptionNotFoundException.java` y `NotificationSubscriptionNotValidException.java`
- [ ] `mvn -q -Dmaven.test.skip=true compile` → 0 errores

### Frontend

- [ ] `npm install @stomp/stompjs sockjs-client`
- [ ] `npm install --save-dev @types/sockjs-client`
- [ ] Crear `notification.model.ts`
- [ ] Agregar export a `shared/models/index.ts`
- [ ] Crear `WsNotificationService` en `core/services/` (con `beforeConnect` para renovar token)
- [ ] Exponer `getValidToken(): Observable<string>` en `AuthService`
- [ ] Conectar `WsNotificationService.connect()` en el flujo de login
- [ ] Conectar `WsNotificationService.disconnect()` en el flujo de logout
- [ ] Crear `NotificationHttpService`
- [ ] Crear componente `notification-bell` standalone
- [ ] Agregar `notification-bell` al header/navbar de la aplicación
- [ ] Crear `notification-settings-page`
- [ ] Crear `notification-subscription-form`
- [ ] Agregar ruta lazy-loaded `notificaciones` al routing
- [ ] Agregar entrada al menú de navegación con permiso `NOTIFICATION_SELF_SUBSCRIBE`
- [ ] `npx tsc --noEmit` → 0 errores
