-- Add JSONB preferences column to tenant table
ALTER TABLE tenant ADD COLUMN preferences JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN tenant.preferences IS 'Flexible key-value preferences for the tenant (e.g. timezone, locale, notifications)';
