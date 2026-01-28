package it.cinofilo.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import it.cinofilo.tenancy.TenantContext;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that extracts tenant ID from JWT claims and stores it in TenantContext.
 * Runs once per request for authenticated users.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public TenantContextFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Check if user is authenticated
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                // If principal is a Jwt token, extract tenantId claim
                if (principal instanceof Jwt jwt) {
                    String tenantIdClaim = jwt.getClaimAsString("tenantId");
                    if (tenantIdClaim != null && !tenantIdClaim.isEmpty()) {
                        try {
                            UUID tenantId = UUID.fromString(tenantIdClaim);
                            TenantContext.setTenantId(tenantId);
                        } catch (IllegalArgumentException e) {
                            logger.warn("Invalid tenantId format in JWT: " + tenantIdClaim);
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear tenant context to prevent memory leaks
            TenantContext.clear();
        }
    }
}
