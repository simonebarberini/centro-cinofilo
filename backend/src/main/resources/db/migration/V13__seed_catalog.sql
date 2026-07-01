INSERT INTO module (module_key, name, description, type, activation_status) VALUES
    ('base',    'Modulo Base',     'Gestione prenotazioni, clienti e cani', 'BASE',     'ACTIVE'),
    ('staff',   'Staff',           'Utenti staff aggiuntivi',               'OPTIONAL', 'INACTIVE'),
    ('sms',     'Notifiche SMS',   'Invio SMS ai clienti',                  'OPTIONAL', 'INACTIVE'),
    ('api',     'Accesso API',     'API REST pubblica',                     'OPTIONAL', 'INACTIVE'),
    ('reports', 'Report avanzati', 'Statistiche e report personalizzati',   'OPTIONAL', 'INACTIVE')
ON CONFLICT (module_key) DO NOTHING;

INSERT INTO module_entitlement (module_key, entitlement_key, bool_value, quota_value) VALUES
    ('base', 'BOOKING_MANAGEMENT',  true, NULL),
    ('base', 'CUSTOMER_MANAGEMENT', true, NULL),
    ('base', 'DOG_MANAGEMENT',      true, NULL),
    ('base', 'CALENDAR_VIEW',       true, NULL),
    ('base', 'MAX_STAFF_USERS',     NULL, 1),
    ('base', 'MAX_DOGS_PER_TENANT', NULL, 500)
ON CONFLICT (module_key, entitlement_key) DO NOTHING;
