package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.model.enums.notification.NotificationChannel;

public interface IUserVerificationService {

    void sendOtp(Long userId, NotificationChannel channel);

    UserResponseDTO confirmOtp(Long userId, NotificationChannel channel, String code);
}
