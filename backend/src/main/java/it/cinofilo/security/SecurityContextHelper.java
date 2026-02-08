package it.cinofilo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Utility class per estrarre informazioni dal SecurityContext JWT
 */
public class SecurityContextHelper {

    private SecurityContextHelper() {
        // Utility class
    }

    private static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            return (Jwt) authentication.getPrincipal();
        }
        throw new IllegalStateException("No JWT found in security context");
    }

    public static UUID currentUserId() {
        Jwt jwt = getCurrentJwt();
        String sub = jwt.getSubject();
        return UUID.fromString(sub);
    }

    public static UUID currentTenantId() {
        Jwt jwt = getCurrentJwt();
        String tenantId = jwt.getClaim("tenantId");
        return UUID.fromString(tenantId);
    }

    public static String currentRole() {
        Jwt jwt = getCurrentJwt();
        return jwt.getClaim("role");
    }

    public static String currentUsername() {
        Jwt jwt = getCurrentJwt();
        return jwt.getClaim("username");
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               authentication.getPrincipal() instanceof Jwt;
    }
}
