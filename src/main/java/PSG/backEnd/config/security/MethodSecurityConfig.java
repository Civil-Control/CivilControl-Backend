package PSG.backEnd.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method-level security configuration.
 * Enables the use of annotations @PreAuthorize, @PostAuthorize, etc.
 * and injects our CustomMethodSecurityExpressionHandler with God Mode.
 *
 * Usage example in controllers:
 *
 * @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
 * public ResponseEntity<?> getEmployee() { ... }
 *
 * If the user has ROLE_ROOT or ROLE_ADMIN, the check will be TRUE automatically.
 * Otherwise, it will verify if they have the specific EMPLOYEE_READ permission.
 */
@Configuration
@EnableMethodSecurity(
        prePostEnabled = true,  // Enables @PreAuthorize and @PostAuthorize
        securedEnabled = true,  // Enables @Secured
        jsr250Enabled = true    // Enables @RolesAllowed
)
public class MethodSecurityConfig {

    /**
     * Injects our custom handler that includes God Mode logic.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new CustomMethodSecurityExpressionHandler();
    }
}

