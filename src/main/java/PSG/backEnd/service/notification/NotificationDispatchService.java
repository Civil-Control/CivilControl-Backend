package PSG.backEnd.service.notification;

import PSG.backEnd.model.entity.notification.NotificationLog;
import PSG.backEnd.model.entity.notification.NotificationPayload;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationDeliveryStatus;
import PSG.backEnd.repository.NotificationLogRepository;
import PSG.backEnd.service.notification.sender.NotificationSenderRegistry;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final NotificationSenderRegistry senderRegistry;
    private final NotificationLogRepository logRepository;

    @Async("notificationTaskExecutor")
    @Transactional
    public void dispatch(NotificationPayload payload,
                         Set<NotificationChannel> channels,
                         Long subscriptionId,
                         Long alertId,
                         Long tenantId) {
        TenantContext.setCurrentTenant(tenantId);
        try {
            for (NotificationChannel channel : channels) {
                NotificationDeliveryStatus status = NotificationDeliveryStatus.SENT;
                String errorMsg = null;
                try {
                    senderRegistry.get(channel).send(payload);
                } catch (Exception e) {
                    status = NotificationDeliveryStatus.FAILED;
                    errorMsg = e.getMessage();
                    log.error("Dispatch failed [channel={}, subscriptionId={}, subjectId={}]: {}",
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
        } finally {
            TenantContext.clear();
        }
    }
}
