# PROJECT_CONTEXT — Centro Cinofilo SaaS

## Scopo

SaaS B2B multi-tenant per la gestione di centri cinofili (pensioni per cani).
Ogni tenant è un centro indipendente con i propri clienti, cani e prenotazioni di box.

## Problema che risolve

I centri cinofili gestiscono prenotazioni di box per cani con capacità fisica limitata.
Senza un sistema dedicato:
- Gestione manuale soggetta a errori e overbooking
- Nessun controllo automatico della capacità disponibile
- Nessuna storica centralizzata di clienti e animali

## Utenti

| Ruolo | Stato |
|-------|-------|
| `TENANT_OWNER` — proprietario del centro cinofilo | Attivo, ruolo operativo attuale |
| `TENANT_STAFF` — personale del centro | Pianificato (Fase 2) |
| `ADMIN_APP` — amministratore piattaforma SaaS | Pianificato (Fase 2) |

Oggi l'unico ruolo operativo è `TENANT_OWNER`. Staff e super-admin sono presenti nell'enum `Role` ma non ancora attivi nel sistema.

## Stack tecnologico

| Layer | Tecnologia | Versione |
|-------|-----------|---------|
| Backend | Spring Boot | 3.2.0 |
| Runtime | Java | 21 |
| Build | Maven | 3.9+ |
| Database | PostgreSQL | 16 |
| Migrazioni | Flyway | incluso in Spring Boot |
| Auth | JJWT | 0.12.3 |
| Rate limiting | Bucket4j | custom package `ratelimit` |
| Frontend | Angular Standalone Components | 17 |
| Frontend language | TypeScript | 5.2 |
| UI | TailwindCSS + DaisyUI | — |
| Email dev | MailHog | — |
| Infrastruttura | Docker + Docker Compose v2+ | — |
| Reverse proxy (prod) | Nginx | — |

## Struttura del repository

```
centro-cinofilo/
├── backend/src/main/java/it/cinofilo/
│   ├── auth/           # Autenticazione, registrazione, email, reset password
│   ├── bookings/       # Prenotazioni, disponibilità, calendario, TenantCapacityGuard
│   │   ├── availability/
│   │   ├── calendar/
│   │   └── dto/
│   ├── catalog/        # Module entity, attivazione feature
│   ├── config/         # SecurityConfig, CorsProperties, GlobalExceptionHandler
│   ├── controller/     # HealthController
│   ├── customer/       # Customer entity + CRUD
│   ├── dogs/           # Dog entity + CRUD
│   ├── domain/
│   │   └── entitlement/ # BooleanEntitlement, QuotaEntitlement, Entitlements
│   ├── ratelimit/      # RateLimitInterceptor, BucketProvider, PolicyRegistry
│   ├── security/       # JwtService, JwtAuthenticationConverter, TenantContextFilter
│   ├── tenancy/        # TenantContext (ThreadLocal), Tenant entity, Role enum
│   └── users/          # AppUser entity
├── backend/src/main/resources/
│   ├── application.yml
│   └── db/migration/   # V1__init.sql … V11__create_module_table.sql
├── backend/src/test/java/it/cinofilo/
│   └── (mirror struttura main — AbstractPostgresIT come base IT)
├── frontend/src/app/
│   ├── core/api/       # BookingsApiService, TenantApiService, ecc.
│   ├── core/auth/      # AuthService, authGuard, tokenInterceptor
│   ├── core/models/    # Interfacce TypeScript
│   ├── layout/         # MainLayoutComponent
│   └── pages/          # Login, Register, Bookings, Calendar, Customers, Dogs
├── infra/docker/
│   ├── dev/            # Solo DB + MailHog
│   ├── local-prod/     # Tutto containerizzato (BE + FE + DB + MailHog)
│   └── prod/           # db.yml + backend.yml + frontend.yml separati
├── docs/               # Documentazione tecnica
├── postman/            # Collection e environment Postman
└── attached_assets/    # Note e analisi architetturali
```

## Principi architetturali fondamentali

1. **Isolamento tenant via discriminator column** — `tenant_id` su ogni tabella di business; ogni query filtra per `tenant_id` estratto dal JWT (mai accettato dal client)
2. **Backend stateless** — `SessionCreationPolicy.STATELESS`, nessuna sessione server-side
3. **ThreadLocal per contesto tenant** — `TenantContext` propaga il `tenantId` nel thread corrente; `clear()` in `finally` obbligatorio
4. **Locking pessimistico per la capacità** — `TenantCapacityGuard` con `SELECT FOR UPDATE` serializza le operazioni sui box
5. **DTO espliciti** — separazione netta tra JPA entity e oggetti API (request/response)
6. **Configurazione via environment** — nessun valore hardcoded; `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, ecc. sono sempre env var
7. **YAGNI rigoroso** — nessuna complessità anticipata senza motivazione concreta documentata
8. **Singola responsabilità per componente** — `BookingService` coordina il caso d'uso, `TenantCapacityGuard` gestisce la capacità
