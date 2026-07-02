CREATE TABLE tenant_module (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenant(id),
    module_key    VARCHAR(50) NOT NULL REFERENCES module(module_key),
    status        VARCHAR(20) NOT NULL,
    trial_ends_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_tenant_module PRIMARY KEY (id),
    CONSTRAINT uq_tenant_module UNIQUE (tenant_id, module_key),
    CONSTRAINT chk_tenant_module_status
        CHECK (status IN ('ACTIVE', 'TRIAL', 'CANCELLED')),
    CONSTRAINT chk_trial_ends_at
        CHECK (trial_ends_at IS NULL OR status = 'TRIAL')
);
