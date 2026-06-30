CREATE TABLE module (
    module_key        VARCHAR(50)  NOT NULL,
    name              VARCHAR(100) NOT NULL,
    description       TEXT,
    type              VARCHAR(10)  NOT NULL,
    activation_status VARCHAR(20)  NOT NULL DEFAULT 'INACTIVE',

    CONSTRAINT pk_module PRIMARY KEY (module_key),
    CONSTRAINT chk_module_type
        CHECK (type IN ('BASE', 'OPTIONAL')),
    CONSTRAINT chk_module_activation_status
        CHECK (activation_status IN ('INACTIVE', 'ACTIVE', 'DEPRECATED'))
);
