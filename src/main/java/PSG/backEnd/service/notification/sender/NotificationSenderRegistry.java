package PSG.backEnd.service.notification.sender;

import PSG.backEnd.model.enums.notification.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
