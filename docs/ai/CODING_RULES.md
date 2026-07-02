# CODING_RULES — Convenzioni e regole di sviluppo

## Package Java

- **Root:** `it.cinofilo`
- **Struttura per feature/dominio:** `it.cinofilo.<feature>` (es: `it.cinofilo.bookings`, `it.cinofilo.customer`, `it.cinofilo.dogs`)
- **Sub-package per layer o sotto-dominio:** `it.cinofilo.bookings.dto`, `it.cinofilo.bookings.availability`, `it.cinofilo.bookings.calendar`
- **Package trasversali:** `it.cinofilo.config`, `it.cinofilo.security`, `it.cinofilo.tenancy`, `it.cinofilo.ratelimit`, `it.cinofilo.domain`
- **Non esistono** package generici `controller/`, `service/`, `repository/` a livello root

---

## Naming Java

| Elemento | Convenzione | Esempio |
|----------|------------|---------|
| Classi | PascalCase | `BookingService`, `TenantCapacityGuard` |
| Metodi | camelCase | `findByIdAndTenantId`, `createBooking` |
| Costanti | UPPER_SNAKE_CASE | `MAX_TOKEN_LENGTH` |
| Eccezioni | `<Dominio>Exception` | `OverbookingException`, `BookingNotFoundException` |
| Controller | `<Feature>Controller` | `BookingController` |
| Service | `<Feature>Service` | `BookingService` |
| Repository | `<Feature>Repository` | `BookingRepository` |
| DTO request | `Create<Feature>Request`, `Update<Feature>Request` | `CreateBookingRequest` |
| DTO response | `<Feature>Response` | `BookingResponse` |
| Guard | `Tenant<Scope>Guard` | `TenantCapacityGuard` |

---

## Naming TypeScript / Angular

| Elemento | Convenzione | Esempio |
|----------|------------|---------|
| Componenti | `<Feature>Component` | `BookingsComponent` |
| Servizi API | `<Feature>ApiService` | `BookingsApiService` |
| File | kebab-case | `token.interceptor.ts`, `auth.guard.ts` |
| Metodi | camelCase | `getBookings`, `createBooking` |
| Costanti | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |

---

## Convenzioni Spring Boot

### Controller

```java
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("isAuthenticated()")
public class BookingController {

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        // ...
    }
}
```

- `@RestController` + `@RequestMapping` a livello classe
- `@Valid` su ogni request body
- `@PreAuthorize("isAuthenticated()")` — minimo per tutti gli endpoint protetti
- **Non** catturare eccezioni nel controller — lasciarle propagare a `GlobalExceptionHandler`

### Service

```java
@Service
public class BookingService {

    @Transactional
    public BookingResponse create(CreateBookingRequest request) { ... }

    @Transactional(readOnly = true)
    public BookingResponse findById(UUID id) { ... }
}
```

- `@Service` + `@Transactional` di default su metodi che scrivono
- `@Transactional(readOnly = true)` su metodi che leggono
- Logica di business qui, non nei controller o nei repository

### Repository

```java
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Booking> findAllByTenantId(UUID tenantId);
}
```

- Spring Data JPA — nessuna query manuale dove non necessario
- **Ogni metodo deve includere `AndTenantId`** per garantire l'isolamento tenant
- Mai query senza filtro `tenant_id` su tabelle di business

### Entità JPA

```java
@Entity
@Table(name = "booking", indexes = { ... })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @CreationTimestamp
    private Instant createdAt;
}
```

- Fetch LAZY di default per `@ManyToOne` — no EAGER senza motivazione esplicita
- `@CreationTimestamp` per `createdAt` (immutabile)
- FK, indici e constraint espliciti nella migration Flyway (non generati da JPA)
- Lombok usato per boilerplate (`@Slf4j` su controller e service, `@Builder`, `@Getter`, ecc.)

### DTO

- Classi separate dalle entità JPA — **mai** restituire l'entità direttamente dal controller
- Validazioni Jakarta su request DTO: `@NotNull`, `@NotBlank`, `@Future`, `@Size`, ecc.
- Il `tenant_id` **non** compare mai in un request DTO — viene sempre letto da `TenantContext.get()`

---

## TenantCapacityGuard — regola critica

```
QUALSIASI operazione che modifica l'occupazione dei box (create, update status, cancel)
DEVE passare per TenantCapacityGuard.
```

- `TenantCapacityGuard` usa `Propagation.MANDATORY` — il chiamante **deve** aprire la transazione prima di invocarlo
- Non aggiungere logica di verifica o modifica capacità in `BookingService` o altrove
- Vedi ADR-001 in DECISIONS.md

---

## Migrazioni Flyway

- **Cartella:** `backend/src/main/resources/db/migration/`
- **Naming:** `V<numero_progressivo>__<descrizione_snake_case>.sql` (es: `V12__add_tenant_slug.sql`)
- **Numerazione:** strettamente progressiva (V1, V2, … Vn) — nessun gap, nessun duplicato
- **Immutabilità:** **MAI modificare un file di migration già committato** — Flyway usa il checksum e fallisce al restart
- **`tenant_id`:** includere `UUID NOT NULL` in tutte le nuove tabelle di business
- **Constraint:** definire FK, indici e constraint esplicitamente nella migration — non affidarsi alla generazione JPA
- **ENUM PostgreSQL:** creare con `CREATE TYPE ... AS ENUM (...)` **prima** della tabella che li usa (vedi V8 come riferimento)

---

## Gestione eccezioni

- Tutte le eccezioni domain-specific estendono `RuntimeException` — **non** usare checked exception
- `GlobalExceptionHandler` (`@RestControllerAdvice`) è l'unico punto di traduzione eccezione → risposta HTTP
- **Non** gestire eccezioni nei controller — lasciarle propagare
- `OverbookingException` → HTTP 409 con dettagli di capacità (già implementato)
- Aggiungere nuove eccezioni in `GlobalExceptionHandler` quando si introduce un nuovo tipo di errore di dominio

---

## Test

- **Base per integration test:** `AbstractPostgresIT` (Testcontainers + PostgreSQL reale)
- I test IT usano MockMvc non-transazionale — i commit sono reali, richiedono cleanup esplicito
- **Cleanup DB:** un singolo `@BeforeEach` FK-safe in `AbstractPostgresIT` — non replicare il cleanup nei singoli test
- **I test IT non possono girare nel sandbox Replit** (Ryuk/sysfs bloccati) — eseguirli in CI o in locale
- I test unitari (Mockito, no DB) possono girare ovunque senza problemi
- **Struttura test:** mirror della struttura `main` (stesso package path sotto `src/test/java/`)
- Verifica la compilazione con `mvn test-compile` per catch errori statici senza eseguire i test IT

---

## Commit e branching

- **Formato commit:** Conventional Commits `<type>(<scope>): <subject>`
  - Tipi: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`
  - Scope comuni: `backend`, `frontend`, `infra`, `docs`
- **Branching:** feature branch da `dev` → PR verso `dev` → PR verso `main` per release
- `main` = produzione stabile — nessun push diretto

---

## Cose da non fare (checklist negativa)

| ❌ Non fare | ✅ Fare invece |
|------------|--------------|
| Accettare `tenant_id` dal client | Leggerlo sempre da `TenantContext.get()` (estratto dal JWT) |
| Aggiungere logica di capacità fuori da `TenantCapacityGuard` | Centralizzare in `TenantCapacityGuard` |
| Modificare migration già committate | Creare una nuova migration Vn+1 |
| Hardcodare secrets o origini nel codice | Usare env var (`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, ecc.) |
| Aggiungere complessità senza motivazione | Documentare la motivazione concreta e discutere prima |
| Usare fetch EAGER senza motivazione | LAZY di default; EAGER solo con motivazione esplicita e documentata |
| Omettere `AndTenantId` nelle query JPA | Sempre `findBy...AndTenantId(...)` su tabelle di business |
| Gestire eccezioni nei controller | Lasciarle propagare a `GlobalExceptionHandler` |
| Restituire l'entità JPA direttamente dal controller | Mappare sempre su DTO response |
| Committare valori reali nel Postman environment | Usare placeholder nei file committati |
