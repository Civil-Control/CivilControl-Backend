package PSG.backEnd.controller;

import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.model.dto.security.VerifyChannelRequestDTO;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.service.port.IUserVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/verify")
@RequiredArgsConstructor
@Tag(name = "Channel Verification", description = "OTP-based verification for notification channels (email and WhatsApp)")
public class UserVerificationController {

    private final IUserVerificationService verificationService;

    @PostMapping("/email/send")
    @Operation(summary = "Send email OTP", description = "Generates and sends a 6-digit OTP to the authenticated user's email address.")
    public ResponseEntity<Void> sendEmailOtp() {
        Long userId = getCurrentUserId();
        verificationService.sendOtp(userId, NotificationChannel.EMAIL);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/confirm")
    @Operation(summary = "Confirm email OTP", description = "Validates the OTP and marks the email channel as verified. Returns the updated user profile.")
    public ResponseEntity<UserResponseDTO> confirmEmailOtp(@Valid @RequestBody VerifyChannelRequestDTO dto) {
        Long userId = getCurrentUserId();
        UserResponseDTO updated = verificationService.confirmOtp(userId, NotificationChannel.EMAIL, dto.code());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/whatsapp/send")
    @Operation(summary = "Send WhatsApp OTP", description = "Generates and sends a 6-digit OTP to the authenticated user's WhatsApp number.")
    public ResponseEntity<Void> sendWhatsappOtp() {
        Long userId = getCurrentUserId();
        verificationService.sendOtp(userId, NotificationChannel.WHATSAPP);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/whatsapp/confirm")
    @Operation(summary = "Confirm WhatsApp OTP", description = "Validates the OTP and marks the WhatsApp channel as verified. Returns the updated user profile.")
    public ResponseEntity<UserResponseDTO> confirmWhatsappOtp(@Valid @RequestBody VerifyChannelRequestDTO dto) {
        Long userId = getCurrentUserId();
        UserResponseDTO updated = verificationService.confirmOtp(userId, NotificationChannel.WHATSAPP, dto.code());
        return ResponseEntity.ok(updated);
    }

    private Long getCurrentUserId() {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }
}
