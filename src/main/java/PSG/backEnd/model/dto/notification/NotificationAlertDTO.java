package PSG.backEnd.model.dto.notification;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NotificationAlertDTO(

        @NotNull(message = "{validation.notNull}", groups = {OnCreate.class, OnUpdate.class})
        @Min(value = 1, message = "{notification.alert.daysBeforeAlert.min}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 365, message = "{notification.alert.daysBeforeAlert.max}", groups = {OnCreate.class, OnUpdate.class})
        Integer daysBeforeAlert,

        boolean active
) {}
