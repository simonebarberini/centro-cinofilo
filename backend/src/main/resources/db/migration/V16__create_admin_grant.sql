CREATE TABLE admin_grant (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenant(id),
    module_key  VARCHAR(50)  NOT NULL REFERENCES module(module_key),
    granted_by  VARCHAR(255) NOT NULL,
    note        TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin_grant PRIMARY KEY (id)
);
