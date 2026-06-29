package it.cinofilo.ratelimit.core;

/**
 * Built-in key extractor strategies.
 *
 * Values are referenced in YAML bucket definitions (key-type: IP_TENANT).
 * USER_ID and TENANT_ID are reserved for future authenticated endpoints.
 */
public enum KeyExtractorType {
    IP,
    IP_TENANT,
    TENANT_USERNAME,
    TENANT_EMAIL,
    USER_ID,
    TENANT_ID
}
