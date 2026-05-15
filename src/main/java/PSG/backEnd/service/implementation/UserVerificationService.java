package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.security.ChannelVerificationNotValidException;
import PSG.backEnd.exception.user.UserNotFoundException;
import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.security.UserVerificationToken;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.mapper.UserMapper;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.repository.UserVerificationTokenRepository;
import PSG.backEnd.service.port.IUserVerificationService;
import PSG.backEnd.service.util.MessageSourceHelper;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVerificationService implements IUserVerificationService {

    private final UserRepository userRepository;
    private final UserVerificationTokenRepository tokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MessageSourceHelper messageSourceHelper;
    private final Resend resendClient;
    private final RestClient whatsAppRestClient;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-address}")
    private String fromAddress;

    @Value("${resend.from-name}")
    private String fromName;

    @Value("${whatsapp.cloud-api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.cloud-api.access-token:}")
    private String accessToken;

    @Override
    @Transactional
    public void sendOtp(Long userId, NotificationChannel channel) {
        if (channel == NotificationChannel.SYSTEM) {
            throw new ChannelVerificationNotValidException(
                    messageSourceHelper.getMessage("channel.verification.invalidChannel"));
        }

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        validateChannelAddress(user, channel);

        String rawOtp = generateOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);

        tokenRepository.deleteByUserIdAndChannel(userId, channel);

        UserVerificationToken token = UserVerificationToken.builder()
                .userId(userId)
                .channel(channel)
                .code(hashedOtp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .createdAt(LocalDateTime.now())
                .build();
        tokenRepository.save(token);

        if (channel == NotificationChannel.EMAIL) {
            dispatchEmailOtp(user.getEmail(), rawOtp);
        } else {
            dispatchWhatsAppOtp(user.getWhatsappNumber(), rawOtp);
        }

        log.info("OTP sent for channel={} userId={}", channel, userId);
    }

    @Override
    @Transactional
    public UserResponseDTO confirmOtp(Long userId, NotificationChannel channel, String code) {
        UserVerificationToken token = tokenRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new ChannelVerificationNotValidException(
                        messageSourceHelper.getMessage("channel.verification.invalidOrExpired")));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new ChannelVerificationNotValidException(
                    messageSourceHelper.getMessage("channel.verification.expired"));
        }

        if (!passwordEncoder.matches(code, token.getCode())) {
            throw new ChannelVerificationNotValidException(
                    messageSourceHelper.getMessage("channel.verification.incorrectCode"));
        }

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (channel == NotificationChannel.EMAIL) {
            user.setEmailVerified(true);
        } else {
            user.setWhatsappVerified(true);
        }

        userRepository.save(user);
        tokenRepository.delete(token);

        log.info("Channel {} verified successfully for userId={}", channel, userId);

        return userMapper.toResponseDto(user);
    }

    // ==================== Private helpers ====================

    private void validateChannelAddress(User user, NotificationChannel channel) {
        if (channel == NotificationChannel.EMAIL) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                throw new ChannelVerificationNotValidException(
                        messageSourceHelper.getMessage("channel.verification.email.notSet"));
            }
        } else if (channel == NotificationChannel.WHATSAPP) {
            if (user.getWhatsappNumber() == null || user.getWhatsappNumber().isBlank()) {
                throw new ChannelVerificationNotValidException(
                        messageSourceHelper.getMessage("channel.verification.whatsapp.notSet"));
            }
        }
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void dispatchEmailOtp(String email, String otp) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("[DEV] Email OTP for {}: {}", email, otp);
            return;
        }

        CreateEmailOptions request = CreateEmailOptions.builder()
                .from(fromName + " <" + fromAddress + ">")
                .to(email)
                .subject("CivilControl — Código de verificación")
                .html(buildEmailHtml(otp))
                .build();

        try {
            resendClient.emails().send(request);
        } catch (ResendException e) {
            log.error("Resend API error sending OTP to {}: {}", email, e.getMessage());
            throw new ChannelVerificationNotValidException(
                    messageSourceHelper.getMessage("channel.verification.email.sendError"));
        }
    }

    private void dispatchWhatsAppOtp(String phoneNumber, String otp) {
        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("[DEV] WhatsApp OTP for {}: {}", phoneNumber, otp);
            return;
        }

        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "to", phoneNumber,
                "type", "template",
                "template", Map.of(
                        "name", "verification_otp",
                        "language", Map.of("code", "es_AR"),
                        "components", List.of(
                                Map.of(
                                        "type", "body",
                                        "parameters", List.of(
                                                Map.of("type", "text", "text", otp)
                                        )
                                )
                        )
                )
        );

        try {
            whatsAppRestClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("WhatsApp OTP send failed for {}: {}", phoneNumber, e.getMessage());
            throw new ChannelVerificationNotValidException(
                    messageSourceHelper.getMessage("channel.verification.whatsapp.sendError"));
        }
    }

    private String buildEmailHtml(String otp) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden">
                  <div style="background:#1a56db;padding:20px 24px">
                    <h2 style="color:#fff;margin:0;font-size:18px">CivilControl — Verificación de canal</h2>
                  </div>
                  <div style="padding:28px 24px">
                    <p style="margin:0 0 16px;color:#374151">Tu código de verificación es:</p>
                    <div style="background:#f3f4f6;border-radius:8px;padding:18px;text-align:center;letter-spacing:10px;font-size:32px;font-weight:700;color:#1a56db">%s</div>
                    <p style="margin:16px 0 0;color:#6b7280;font-size:13px">Válido por %d minutos. No lo compartas con nadie.</p>
                  </div>
                  <div style="background:#f9fafb;padding:12px 24px;border-top:1px solid #e5e7eb">
                    <small style="color:#9ca3af">Este mensaje fue generado automáticamente. No responder.</small>
                  </div>
                </div>
                """.formatted(otp, OTP_EXPIRY_MINUTES);
    }
}
