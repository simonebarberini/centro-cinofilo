-- V2: Create tenant and user tables for multi-tenancy support

-- Tabella tenant
CREATE TABLE tenant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    capacity_boxes INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabella app_user
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_app_user_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT uk_tenant_username UNIQUE (tenant_id, username)
);

-- Indici per performance
CREATE INDEX idx_app_user_tenant_id ON app_user(tenant_id);
CREATE INDEX idx_app_user_username ON app_user(username);
CREATE INDEX idx_tenant_type ON tenant(type);

-- Commenti per documentazione
COMMENT ON TABLE tenant IS 'Tabella dei tenant (centri cinofili)';
COMMENT ON TABLE app_user IS 'Tabella utenti applicazione con tenant isolation';
COMMENT ON COLUMN app_user.role IS 'Ruoli: ADMIN_APP, TENANT_OWNER, TENANT_STAFF';
