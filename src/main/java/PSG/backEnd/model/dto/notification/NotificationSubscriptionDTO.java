package PSG.backEnd.model.dto.notification;

import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record NotificationSubscriptionDTO(

        @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
        Long userId,

        @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
        NotificationSubjectType subjectType,

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
