CREATE TABLE customer (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_customer_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_customer_tenant_id ON customer(tenant_id);
CREATE INDEX idx_customer_tenant_email ON customer(tenant_id, email);
