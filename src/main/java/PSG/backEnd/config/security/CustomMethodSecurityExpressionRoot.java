package PSG.backEnd.config.security;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom Expression Root that implements "God Mode" logic.
 *
 * GOLDEN RULE (God Mode):
 * If the user has the ROLE_ROOT or ROLE_ADMIN role, they are automatically granted access
 * to ALL system permissions through getAuthorities().
 *
 * NOTE: In Spring Security 6, the hasAuthority/hasRole methods are final and cannot be
 * overridden. God Mode is implemented in the User entity by adding all authorities
 * automatically when the user has ROLE_ROOT or ROLE_ADMIN.
 *
 * This ExpressionRoot is maintained for future extensibility and custom methods.
 */
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot
        implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;

    /**
     * Roles that have "God Mode" (full system access).
     */
    private static final String GOD_ROLE_ROOT = "ROLE_ROOT";
    private static final String GOD_ROLE_ADMIN = "ROLE_ADMIN";

    public CustomMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    /**
     * Custom method to check if the user has God Mode.
     * Can be used in @PreAuthorize expressions like: @PreAuthorize("isGodMode() or hasAuthority('SOME_PERMISSION')")
     *
     * @return true if the user has ROLE_ROOT or ROLE_ADMIN
     */
    public boolean isGodMode() {
        if (getAuthentication() == null || getAuthentication().getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : getAuthentication().getAuthorities()) {
            String authorityName = authority.getAuthority();
            if (GOD_ROLE_ROOT.equals(authorityName) || GOD_ROLE_ADMIN.equals(authorityName)) {
                return true;
            }
        }

        return false;
    }

    // ==================== MethodSecurityExpressionOperations Implementation ====================

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return this;
    }
}

