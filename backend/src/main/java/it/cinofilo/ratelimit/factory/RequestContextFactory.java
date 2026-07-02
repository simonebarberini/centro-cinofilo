package it.cinofilo.ratelimit.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cinofilo.ratelimit.core.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a RequestContext from an HttpServletRequest.
 *
 * Centralizes all data-extraction logic so that key extractors and the
 * interceptor never need to know WHERE data comes from:
 *
 *   IP          → X-Forwarded-For / X-Real-IP / remoteAddr (in priority order)
 *   tenantSlug  → JSON request body field "tenantSlug"
 *   username    → JSON request body field "username"
 *   email       → JSON request body field "email"
 *   userId      → JWT claim "sub"       (authenticated endpoints)
 *   tenantId    → JWT claim "tenantId"  (authenticated endpoints)
 *
 * Body parsing is safe because RequestBodyCachingFilter wraps every request
 * with a RepeatableContentRequestWrapper before this factory is invoked,
 * allowing the body to be read multiple times (interceptor + controller).
 *
 * All extractions fail gracefully: missing or unparseable fields become
 * Optional.empty() in RequestContext — callers must handle absent values.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestContextFactory {

    private final ObjectMapper objectMapper;

    public RequestContext build(HttpServletRequest request) {
        String ip = extractIp(request);
        String path = request.getServletPath();

        String tenantSlug = null;
        String username = null;
        String email = null;

        try {
            byte[] bodyBytes = request.getInputStream().readAllBytes();
            if (bodyBytes.length > 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(
                        new String(bodyBytes, StandardCharsets.UTF_8), Map.class);
                tenantSlug = (String) body.get("tenantSlug");
                username = (String) body.get("username");
                email = (String) body.get("email");
            }
        } catch (Exception e) {
            log.debug("Rate limit: cannot parse request body for context extraction on {}: {}",
                    path, e.getMessage());
        }

        UUID userId = null;
        UUID tenantId = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Map<String, Object> claims = jwtAuth.getToken().getClaims();
                Object sub = claims.get("sub");
                Object tid = claims.get("tenantId");
                if (sub instanceof String s && !s.isBlank()) userId = UUID.fromString(s);
                if (tid instanceof String t && !t.isBlank()) tenantId = UUID.fromString(t);
            }
        } catch (Exception e) {
            log.debug("Rate limit: cannot extract JWT claims for context on {}: {}",
                    path, e.getMessage());
        }

        return RequestContext.of(ip, tenantSlug, username, email, userId, tenantId, path);
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].strip();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.strip();
        }
        return request.getRemoteAddr();
    }
}
