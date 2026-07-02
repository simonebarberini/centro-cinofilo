CREATE TABLE module_entitlement (
    module_key       VARCHAR(50)  NOT NULL REFERENCES module(module_key) ON DELETE CASCADE,
    entitlement_key  VARCHAR(100) NOT NULL,
    bool_value       BOOLEAN      NULL,
    quota_value      INTEGER      NULL,

    CONSTRAINT pk_module_entitlement
        PRIMARY KEY (module_key, entitlement_key),
    CONSTRAINT chk_module_entitlement_single_type CHECK (
        (bool_value IS NOT NULL AND quota_value IS NULL) OR
        (bool_value IS NULL     AND quota_value IS NOT NULL)
    )
);
