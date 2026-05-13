package PSG.backEnd.config.websocket;

import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.service.implementation.JwtService;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    String username = jwtService.extractUsername(jwt);
                    Long tenantId  = jwtService.extractTenantId(jwt);
                    TenantContext.setCurrentTenant(tenantId);
                    userRepository.findByCredentialsUsernameAndDeletedFalse(username)
                            .ifPresent(user -> accessor.setUser(() -> user.getId().toString()));
                } catch (Exception ignored) {
                    // JWT inválido: el broker rechazará la conexión
                }
            }
        }
        return message;
    }
}
