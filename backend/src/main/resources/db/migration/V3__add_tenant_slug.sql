-- Add slug column to tenant table for per-tenant login
ALTER TABLE tenant ADD COLUMN slug VARCHAR(255) NOT NULL UNIQUE;

-- Create index on slug for performance
CREATE INDEX idx_tenant_slug ON tenant(slug);
