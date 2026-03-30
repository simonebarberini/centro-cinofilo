-- V8: Aggiunge verifica email su app_user e tabella email_token per verification/reset

-- 1. Estendi app_user con email e flag di verifica
ALTER TABLE app_user
    ADD COLUMN email          VARCHAR(255),
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Indice parziale: email unica per tenant (ignora i NULL delle righe preesistenti)
CREATE UNIQUE INDEX uk_app_user_tenant_email
    ON app_user (tenant_id, email)
    WHERE email IS NOT NULL;

COMMENT ON COLUMN app_user.email          IS 'Indirizzo email per verifica e reset password';
COMMENT ON COLUMN app_user.email_verified IS 'True dopo che l''utente ha cliccato il link di verifica';

-- 2. Tipo enum per i token
CREATE TYPE email_token_type AS ENUM ('EMAIL_VERIFICATION', 'PASSWORD_RESET');

-- 3. Tabella token (condivisa per entrambi i flussi)
CREATE TABLE email_token (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID                NOT NULL,
    token      VARCHAR(64)         NOT NULL,
    type       email_token_type    NOT NULL,
    expires_at TIMESTAMPTZ         NOT NULL,
    used       BOOLEAN             NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_email_token_user FOREIGN KEY (user_id)
        REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uk_email_token_token UNIQUE (token)
);

CREATE INDEX idx_email_token_user_id ON email_token(user_id);
CREATE INDEX idx_email_token_token   ON email_token(token);

COMMENT ON TABLE  email_token           IS 'Token monouso per verifica email e reset password';
COMMENT ON COLUMN email_token.token     IS 'Token casuale (32 hex chars da UUID)';
COMMENT ON COLUMN email_token.type      IS 'EMAIL_VERIFICATION o PASSWORD_RESET';
COMMENT ON COLUMN email_token.expires_at IS 'Scadenza: 24h per verifica, 1h per reset';
COMMENT ON COLUMN email_token.used      IS 'True dopo il primo utilizzo (previene replay)';
