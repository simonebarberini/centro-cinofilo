# ARCHITECTURE — Centro Cinofilo SaaS

## Backend — Spring Boot 3.2 / Java 21

### Struttura a layer per feature

```
HTTP Request
    │
    ▼
*Controller          ← REST endpoint, @Valid, @PreAuthorize
    │
    ▼
*Service             ← logica di business, @Transactional
    │         │
    ▼         ▼
*Repository   TenantCapacityGuard    ← solo per operazioni che modificano l'occupazione
(JPA)              │
    │              ▼
    └──────► PostgreSQL (SELECT FOR UPDATE su tenant row)
              │
    TenantContext (ThreadLocal) — propaga tenantId in tutto il thread
```

Ogni package feature (`bookings`, `customer`, `dogs`, ecc.) contiene:
- `*Controller.java` — REST endpoint; validazione input con `@Valid`; autorizzazione con `@PreAuthorize("isAuthenticated()")`
- `*Service.java` — logica di business; `@Transactional` di default
- `*Repository.java` — Spring Data JPA; metodi `findBy...AndTenantId(...)` per isolamento tenant
- `dto/` — request e response DTO separati dalle entità JPA

---

## Multi-tenancy

- **Strategia:** discriminator column (`tenant_id` UUID su ogni tabella di business)
- **Propagazione:** `TenantContextFilter` (Spring Security filter chain) estrae `tenantId` dal claim JWT e lo scrive in `TenantContext` (ThreadLocal)
- **Cleanup:** `TenantContext.clear()` in blocco `finally` — obbligatorio per evitare memory leak nel thread pool
- **Enforcement nei repository:** `findByIdAndTenantId(UUID id, UUID tenantId)` — `tenantId` viene sempre da `TenantContext.get()`, **mai** dal client

**Regola critica:** il `tenant_id` non viene **mai** accettato dal client. Viene sempre letto dal JWT → `TenantContextFilter` → `TenantContext`.

---

## Autenticazione

- **Meccanismo:** JWT stateless (Spring Security OAuth2 Resource Server)
- **Algoritmo:** HS512 — secret minimo 64 byte
- **Startup check:** `JwtService` valida al bootstrap che `JWT_SECRET` abbia almeno 64 byte; rifiuta lo startup se la condizione non è soddisfatta (fail-fast)
- **Claims nel token:** `userId`, `tenantId`, `role`, `username`
- **`JwtService`:** genera e valida token con JJWT 0.12.3; algoritmo hard-pinned a HS512
- **`JwtAuthenticationConverter`:** mappa il claim `role` a `GrantedAuthority` Spring Security con prefisso `ROLE_`
- **Password:** `BCryptPasswordEncoder`
- **Login:** richiede `tenantSlug` + `username` + `password`

### Flusso autenticazione completo

```
Client → POST /api/auth/login { tenantSlug, username, password }
       → AuthController → AuthService
       → 1. trova Tenant per slug
       → 2. trova AppUser per username + tenant_id
       → 3. verifica password (BCrypt)
       → 4. verifica emailVerified == true
       → 5. JwtService.generateToken(userId, tenantId, role, username)
       → risposta: { accessToken }
```

---

## Security configuration (`SecurityConfig`)

Due `SecurityFilterChain` distinte:

| Chain | Percorsi | Auth |
|-------|---------|------|
| Pubblica | `/auth/**`, `/health` | Nessuna auth richiesta |
| Protetta | Tutto il resto | JWT required; `TenantContextFilter` iniettato |

- **Session:** `STATELESS`
- **CSRF:** disabilitato (API stateless)
- **Security headers API:**
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: no-referrer`
  - `Cache-Control: no-cache, no-store, must-revalidate`
- **HSTS e CSP:** deliberatamente assenti nel backend — demandati a Nginx in produzione (vedi ADR-003 in DECISIONS.md)

---

## CORS

- Origini configurabili via env var `CORS_ALLOWED_ORIGINS` (lista separata da virgole)
- `setAllowCredentials(true)` — richiede origini esplicite, no wildcard `*`
- Gestito da `CorsProperties` + `SecurityConfig`

---

## Rate limiting

- **Package:** `it.cinofilo.ratelimit`
- **Meccanismo:** Bucket4j token bucket, in-memory (non distribuito)
- **`RateLimitInterceptor`:** `HandlerInterceptor` che verifica le policy prima di ogni request
- **`PolicyRegistry`:** namespacing dei bucket come `POLICY_TYPE:KEY_TYPE:VALUE` per evitare collisioni tra policy diverse
- **`BucketProvider`:** crea o recupera i bucket per chiave

| Policy | Bucket(s) | Limite |
|--------|-----------|--------|
| `LOGIN` | `IP_TENANT` | 20 req / 15 min |
| `LOGIN` | `TENANT_USERNAME` | 10 req / 15 min |
| `REGISTER` | `IP` | configurato in application.yml |
| `RESET_PASSWORD` | `IP` | configurato in application.yml |

- **Headers di risposta:** `X-RateLimit-Remaining` (sempre); `Retry-After` (su HTTP 429)
- **Limitazione:** in-memory → i contatori si azzerano al restart; non scalabile orizzontalmente senza Redis

---

## Gestione prenotazioni e capacità

```
BookingService.createBooking(request)
    │
    ├─ valida date (startDate < endDate)
    ├─ verifica ownership dog e customer (stesso tenant)
    │
    └─► TenantCapacityGuard.reserve(tenantId, startDate, endDate)
            │
            ├─ SELECT FOR UPDATE su riga tenant (lock pessimistico)
            ├─ calcola occupazione corrente nel periodo
            ├─ verifica: occupazione + 1 <= capacityBoxes
            ├─ se OK: prosegue (il Service crea il Booking)
            └─ se KO: lancia OverbookingException → HTTP 409
```

- `TenantCapacityGuard` usa `Propagation.MANDATORY` — **deve essere chiamato dentro una transazione attiva del chiamante**
- Tutta la logica che modifica l'occupazione dei box deve passare da questo componente (vedi ADR-001)

---

## Gestione eccezioni

`GlobalExceptionHandler` (`@RestControllerAdvice`) è l'unico punto di traduzione eccezione → HTTP:

| Eccezione | HTTP |
|-----------|------|
| `BookingNotFoundException` | 404 |
| `UnauthorizedException` | 401 |
| `OverbookingException` | 409 con dettagli capacità/occupazione |
| `MethodArgumentNotValidException` | 400 con field errors |

I controller **non** catturano eccezioni — le lasciano propagare al handler.

---

## Email

- `EmailService` gestisce l'invio via SMTP configurabile
- `EmailToken` con TTL e flag `used` per verifica email e reset password
- `EmailTokenType`: `EMAIL_VERIFICATION`, `PASSWORD_RESET`
- In sviluppo: MailHog intercetta le email senza consegnarle a destinatari reali

---

## Frontend — Angular 17

- **Standalone Components** — no NgModules
- **Bootstrap:** `bootstrapApplication()` in `main.ts`
- **Routing:** `app.routes.ts` con lazy loading (`loadComponent`) e route protette da `authGuard`
- **Layout:** `MainLayoutComponent` wrappa le route autenticate

### Autenticazione frontend

- JWT stored in `localStorage` con chiave `accessToken`
- `tokenInterceptor` (functional interceptor): aggiunge `Authorization: Bearer <token>` a ogni request verso `environment.apiBaseUrl`
- `authGuard`: verifica `auth.isAuthenticated()`, redirect a `/login` se non autenticato
- Login: l'utente inserisce esplicitamente `tenantSlug` + `username` + `password`

### Servizi API (`core/api/`)

Un servizio per dominio (es: `BookingsApiService`, `TenantApiService`). Incapsulano le chiamate HTTP al backend e tipizzano i DTO come interfacce TypeScript in `core/models/`.

---

## Database

- **DBMS:** PostgreSQL 16
- **Schema evolution:** Flyway (V1–V11, numerazione progressiva, immutabile dopo commit)
- **Indici:** su tutte le FK e sulle colonne più interrogate (`slug`, `username`, `start_date`, `tenant_id` combinato)
- **`tenant_id`** presente su tutte le tabelle di business: `app_user`, `customer`, `dog`, `booking`
- **`email_token_type`** è un ENUM PostgreSQL nativo (non VARCHAR), creato con `CREATE TYPE` nella V8

---

## Infrastruttura

| Ambiente | Configurazione |
|----------|---------------|
| Dev | DB + MailHog in Docker; BE e FE avviati direttamente (Maven + npm) |
| Local-prod | Tutti i 4 servizi containerizzati (DB + BE + FE + MailHog) |
| Prod | 3 Compose file separati: `db.yml`, `backend.yml`, `frontend.yml`; BE con Dockerfile multi-stage; FE servito da Nginx |

Frontend dev proxy: `proxy.conf.json` instrada `/api/*` → `http://localhost:8080`.
