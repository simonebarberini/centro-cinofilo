package it.cinofilo.ratelimit.core;

/**
 * Named rate limit policies.
 *
 * Each value maps to a named configuration block in application.yml
 * (rate-limit.policies.*). Adding a new endpoint requires adding a new
 * entry here and a corresponding YAML block — no other code changes.
 */
public enum RateLimitPolicyType {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    RESEND_VERIFICATION,
    RESET_PASSWORD
}
