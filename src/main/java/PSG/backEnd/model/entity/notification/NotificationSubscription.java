package PSG.backEnd.model.entity.notification;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "notification_subscriptions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
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
