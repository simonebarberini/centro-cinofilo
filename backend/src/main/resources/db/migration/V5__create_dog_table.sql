-- Create dog table with multi-tenant isolation
CREATE TABLE dog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    breed VARCHAR(255),
    birth_date DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Foreign keys
    CONSTRAINT fk_dog_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_dog_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

-- Index for tenant isolation queries
CREATE INDEX idx_dog_tenant_id ON dog(tenant_id);

-- Index for customer queries within tenant
CREATE INDEX idx_dog_tenant_customer ON dog(tenant_id, customer_id);

-- Comment
COMMENT ON TABLE dog IS 'Dogs managed by kennels, linked to customers with multi-tenant isolation';
COMMENT ON COLUMN dog.tenant_id IS 'Tenant owning this dog record';
COMMENT ON COLUMN dog.customer_id IS 'Customer owning this dog';
