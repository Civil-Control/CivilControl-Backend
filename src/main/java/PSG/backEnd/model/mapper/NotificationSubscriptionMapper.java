package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.notification.NotificationAlertDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionResponseDTO;
import PSG.backEnd.model.entity.notification.NotificationAlert;
import PSG.backEnd.model.entity.notification.NotificationSubscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationSubscriptionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "channels", ignore = true)
    @Mapping(target = "alerts", ignore = true)
    @Mapping(target = "subscribedByUserId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    NotificationSubscription toEntity(NotificationSubscriptionDTO dto);

    @Mapping(target = "userFullName", ignore = true)
    @Mapping(target = "subjectDisplayName", ignore = true)
    NotificationSubscriptionResponseDTO toResponseDto(NotificationSubscription entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    NotificationAlert alertToEntity(NotificationAlertDTO dto);

    NotificationSubscriptionResponseDTO.NotificationAlertResponseDTO alertToResponseDto(NotificationAlert alert);
}
