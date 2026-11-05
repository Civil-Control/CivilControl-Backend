package PSG.backEnd.model.dto.notification;

import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.ReminderRecurrence;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CustomReminderDTO(

        @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
        Long userId,

        @NotBlank(message = "{validation.notBlank}", groups = {OnCreate.class, OnUpdate.class})
        @Size(max = 150, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
        String title,

        @Size(max = 500, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate reminderDate,

        @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
        ReminderRecurrence recurrenceType,

        LocalDate recurrenceEndDate,

        @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
        @NotEmpty(message = "{validation.notEmpty}", groups = {OnCreate.class, OnUpdate.class})
        Set<NotificationChannel> channels,

        @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
        @Valid
        List<NotificationAlertDTO> alerts,

        boolean active
) {}
