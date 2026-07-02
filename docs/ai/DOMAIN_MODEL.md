# DOMAIN_MODEL — Centro Cinofilo SaaS

## Entità del dominio

### Tenant (`tenant` table)

| Campo | Tipo | Constraint |
|-------|------|-----------|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `name` | VARCHAR(255) | NOT NULL |
| `type` | VARCHAR(100) | NOT NULL |
| `slug` | VARCHAR (aggiunto in migrazione successiva) | NOT NULL UNIQUE — usato per login e routing |
| `capacity_boxes` | INT | NOT NULL, DEFAULT 0 |
| `preferences` | JSONB | NOT NULL — configurazione flessibile (locale, timezone, ecc.) |
| `billing_email` | VARCHAR | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, auto |

Nota: `slug`, `preferences`, `billing_email` sono stati aggiunti in migrazioni successive alla V2 (che crea la tabella base).

---

### AppUser (`app_user` table)

| Campo | Tipo | Constraint |
|-------|------|-----------|
| `id` | UUID | PK |
| `tenant_id` | UUID | FK → `tenant(id)` ON DELETE RESTRICT |
| `username` | VARCHAR(100) | NOT NULL; UNIQUE per tenant (`uk_tenant_username`) |
| `password_hash` | VARCHAR(255) | NOT NULL (BCrypt) |
| `role` | VARCHAR(50) | NOT NULL — valori: `ADMIN_APP`, `TENANT_OWNER`, `TENANT_STAFF` |
| `enabled` | BOOLEAN | NOT NULL, DEFAULT TRUE |
| `email` | VARCHAR | nullable |
| `email_verified` | BOOLEAN | NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL, auto |

---

### Customer (`customer` table)

| Campo | Tipo | Constraint |
|-------|------|-----------|
| `id` | UUID | PK |
| `tenant_id` | UUID | FK → `tenant(id)` NOT NULL |
| `first_name` | VARCHAR | NOT NULL |
| `last_name` | VARCHAR | nullable (reso opzionale in V9) |
| `email` | VARCHAR | nullable |
| `phone` | VARCHAR | nullable |
| `notes` | TEXT | nullable |
| `created_at` / `updated_at` | TIMESTAMPTZ | auto |

---

### Dog (`dog` table)

| Campo | Tipo | Constraint |
|-------|------|-----------|
| `id` | UUID | PK |
| `tenant_id` | UUID | NOT NULL (colonna diretta, non join JPA) |
| `customer_id` | UUID | FK → `customer(id)` ManyToOne LAZY |
| `name` | VARCHAR | NOT NULL |
| `breed` | VARCHAR | nullable |
| `birth_date` | DATE | nullable |
| `notes` | TEXT | nullable |
| `created_at` / `updated_at` | TIMESTAMPTZ (OffsetDateTime in Java) | auto |

---

### Booking (`booking` table)

| Campo | Tipo | Constraint |
|-------|------|-----------|
| `id` | UUID | PK |
| `tenant_id` | UUID | NOT NULL (colonna diretta, non join JPA) |
| `customer_id` | UUID | FK → `customer(id)` ON DELETE CASCADE |
| `dog_id` | UUID | FK → `dog(id)` ON DELETE CASCADE |
| `start_date` | DATE | NOT NULL |
| `end_date` | DATE | NOT NULL |
| `notes` | TEXT | nullable |
| `status` | VARCHAR(50) | NOT NULL — valori: `CONFIRMED`, `CANCELLED` |
| `created_at` / `updated_at` | TIMESTAMPTZ | auto |

Constraint DB: `CHECK (end_date > start_date)` — applicato nella V6.

Semantica date: `end_date` è **esclusiva** (come il checkout in un hotel). Un booking che occupa il box dal 1 al 3 luglio ha `start_date = 2025-07-01`, `end_date = 2025-07-03`.

---

### EmailToken (`email_token` table)

| Campo | Tipo | Constraint |
|-------|------|-----------|
| `id` | UUID | PK |
| `user_id` | UUID | FK → `app_user(id)` ON DELETE CASCADE |
| `token` | VARCHAR(64) | NOT NULL UNIQUE |
| `type` | `email_token_type` (ENUM PostgreSQL nativo) | NOT NULL — valori: `EMAIL_VERIFICATION`, `PASSWORD_RESET` |
| `expires_at` | TIMESTAMPTZ | NOT NULL |
| `used` | BOOLEAN | NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL, auto |

---

### Module (`module` table)

| Campo | Tipo | Constraint |
|-------|------|-----------|
| `module_key` | VARCHAR(50) | PK |
| `name` | VARCHAR(100) | NOT NULL |
| `description` | TEXT | nullable |
| `type` | Enum(`ModuleType`) | BASE / OPTIONAL |
| `activation_status` | Enum(`ModuleActivationStatus`) | — |

---

## Enum Java verificati

| Enum | Valori |
|------|--------|
| `Role` | `ADMIN_APP`, `TENANT_OWNER`, `TENANT_STAFF` |
| `BookingStatus` | `CONFIRMED`, `CANCELLED` |
| `EmailTokenType` | `EMAIL_VERIFICATION`, `PASSWORD_RESET` |

---

## Relazioni principali

```
Tenant ──< AppUser       (1:N, FK ON DELETE RESTRICT)
Tenant ──< Customer      (1:N, FK ON DELETE RESTRICT)
Customer ──< Dog         (1:N, FK ON DELETE CASCADE)
Customer ──< Booking     (1:N, FK ON DELETE CASCADE)
Dog ──< Booking          (1:N, FK ON DELETE CASCADE)
AppUser ──< EmailToken   (1:N, FK ON DELETE CASCADE)
```

---

## Diagramma testuale

```
┌───────────────────────────────────────────────────────────────┐
│                           TENANT                              │
│  id*, name, type, slug*, capacity_boxes, preferences(JSONB)   │
└──────────────┬─────────────────────┬─────────────────────────-┘
               │                     │
        ┌──────▼──────┐       ┌──────▼──────┐
        │   AppUser   │       │  Customer   │
        │ username*   │       │ firstName   │
        │ role        │       │ lastName?   │
        │ pwd, enabled│       │ email?, ph? │
        └──────┬──────┘       └──────┬──────┘
               │                     │
        ┌──────▼──────┐       ┌──────▼──────┐
        │ EmailToken  │       │     Dog     │
        │ token*, type│       │ name, breed?│
        │ expiresAt   │       │ birthDate?  │
        │ used        │       └──────┬──────┘
        └─────────────┘              │
                              ┌──────▼──────┐
                              │   Booking   │
                              │ startDate   │
                              │ endDate     │
                              │ status      │
                              └─────────────┘

┌─────────────┐
│   Module    │
│ moduleKey*  │
│ type, status│
└─────────────┘

  * = unique/PK   ? = nullable   JSONB = colonna JSON
```

---

## Invarianti di dominio

1. Il numero di booking con `status = CONFIRMED` che si sovrappongono a un dato periodo non può superare `tenant.capacity_boxes`
2. `start_date < end_date` (constraint CHECK in V6)
3. Un `Dog` appartiene sempre allo stesso tenant del suo `Customer`
4. Un `Booking` appartiene sempre allo stesso tenant del suo `Dog` e del suo `Customer`
5. `EmailToken.token` è univoco globalmente; un token `used = true` non può essere riusato
6. `AppUser.username` è univoco per tenant (constraint `uk_tenant_username`)
7. `Tenant.slug` è univoco globalmente

---

## Bounded context

Il progetto ha un **singolo bounded context: Centro Cinofilo Management**. Non esiste separazione in microservizi o bounded context multipli. I package Java rappresentano sottodomini funzionali (`auth`, `bookings`, `customer`, `dogs`, `catalog`) ma condividono lo stesso database e processo JVM.
