package PSG.backEnd.model.entity.notification;

import jakarta.persistence.*;
import lombok.*;

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
