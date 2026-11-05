package PSG.backEnd.model.entity.notification;

import PSG.backEnd.model.entity.TenantEntity;
import PSG.backEnd.model.enums.notification.ReminderRecurrence;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "custom_reminders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class CustomReminder extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private ReminderRecurrence recurrenceType;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
