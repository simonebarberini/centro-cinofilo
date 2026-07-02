package it.cinofilo.config;

import it.cinofilo.security.JwtAuthenticationConverter;
import it.cinofilo.security.JwtService;
import it.cinofilo.security.TenantContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;

    /**
     * Security filter chain for public endpoints.
     * Order(1) ensures this chain is evaluated first for matching requests.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatchers(matchers -> matchers
                .requestMatchers("/auth/**", "/actuator/health", "/health")
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(securityHeaders())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * Security filter chain for protected endpoints.
     * Requires JWT authentication and sets up tenant context.
     */
    @Bean
    public SecurityFilterChain protectedSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(securityHeaders())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            )
            .addFilterAfter(new TenantContextFilter(jwtService), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Security headers applied to ALL API responses (both filter chains).
     *
     * <p>Ownership split: Spring owns the headers for {@code /api} responses;
     * Nginx owns the headers for the Angular SPA (HTML shell + static assets).
     *
     * <p>Content-Security-Policy and Strict-Transport-Security are deliberately
     * deferred: HSTS is disabled here on purpose and will be enabled together
     * with CSP once the deploy architecture and TLS termination are finalized.
     */
    private Customizer<HeadersConfigurer<HttpSecurity>> securityHeaders() {
        return headers -> headers
            // X-Content-Type-Options: nosniff — block MIME-type sniffing.
            .contentTypeOptions(Customizer.withDefaults())
            // X-Frame-Options: DENY — API responses must never be framed.
            .frameOptions(frame -> frame.deny())
            // Referrer-Policy: no-referrer — API responses never need to leak a referrer.
            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
            // Cache-Control: no-cache, no-store, must-revalidate (+ Pragma/Expires) —
            // API payloads may contain tenant data and tokens; never cache them.
            .cacheControl(Customizer.withDefaults())
            // Strict-Transport-Security intentionally OFF for now (deferred with CSP).
            .httpStrictTransportSecurity(hsts -> hsts.disable())
            // Permissions-Policy: disable powerful browser features the app never uses.
            .addHeaderWriter(new StaticHeadersWriter(
                "Permissions-Policy", "geolocation=(), camera=(), microphone=()"));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = new SecretKeySpec(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8),
            "HmacSHA512"
        );
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
