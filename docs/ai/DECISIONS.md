# DECISIONS — Architectural Decision Records

Formato: Problema → Decisione → Motivazione → Conseguenze

---

## ADR-001 — TenantCapacityGuard come owner del dominio capacità

**Stato:** implementato

**Problema:** La creazione, modifica e cancellazione di booking deve verificare atomicamente che la capacità dei box non venga superata. Con richieste concorrenti, una semplice lettura + scrittura porta a overbooking (race condition classica read-modify-write).

**Decisione:** Introdurre `TenantCapacityGuard` come componente dedicato che acquisisce un lock pessimistico (`SELECT FOR UPDATE`) sulla riga del tenant prima di ogni operazione che modifica l'occupazione.

**Motivazione:** Il lock pessimistico serializza le operazioni per tenant, garantendo atomicità dell'intera sequenza "leggi occupazione → verifica capacità → scrivi". È la soluzione più semplice e corretta per un carico B2B con concorrenza moderata (centri cinofili, non e-commerce ad alto volume).

**Conseguenze:**
- Nessuna race condition di overbooking
- Throughput ridotto su prenotazioni simultanee per lo stesso tenant (serializzazione intenzionale e accettabile)
- `TenantCapacityGuard` usa `Propagation.MANDATORY` — deve sempre essere chiamato dentro una transazione attiva del chiamante (`BookingService`)
- Tutta la logica futura che modifica capacità/occupazione **deve** passare per questo componente — non va dispersa in `BookingService` o altrove
- Il componente è progettato per evolvere in punto centrale del dominio "capacità" (waitlist, import massivi, overbooking controllato)

---

## ADR-002 — JWT HS512 con secret minimo 64 byte, algoritmo hard-pinned

**Stato:** implementato

**Problema:** JJWT 0.12.x deriverebbe l'algoritmo HMAC dalla lunghezza del secret se non specificato esplicitamente, rischiando divergenze silenziose tra generazione e verifica del token. Un secret troppo corto indebolisce la firma crittografica.

**Decisione:** Hard-pin esplicito di HS512 in `JwtService`. La classe valida al bootstrap che `JWT_SECRET` abbia almeno 64 byte e rifiuta lo startup se la condizione non è soddisfatta.

**Motivazione:** Sicurezza deterministica. Algoritmo fisso → nessuna deriva implicita. Lunghezza minima garantita → nessun secret debole in produzione. Fail-fast al deploy → errori di configurazione emergono immediatamente, non a runtime.

**Conseguenze:**
- Il deploy fallisce esplicitamente se `JWT_SECRET` è troppo corto (no silent degradation)
- Tutti i token generati usano HS512 — coerenza garantita tra generazione e verifica
- I deploy in produzione devono fornire un secret di almeno 64 byte (es: generato con `openssl rand -hex 64`)

---

## ADR-003 — Security headers: backend gestisce `/api/`, Nginx gestisce la SPA

**Stato:** implementato (backend); Nginx non ancora configurato per prod

**Problema:** Sovrapporre header di sicurezza tra Spring Boot e Nginx porta a header duplicati (vietato da RFC) o lacune di copertura per la SPA Angular.

**Decisione:** Il backend Spring emette solo gli header pertinenti alle risposte API (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Cache-Control`). HSTS e CSP sono deliberatamente omessi nel backend: HSTS Spring Boot è disabilitato esplicitamente; entrambi sono demandati a Nginx quando TLS e infrastruttura saranno finalizzati.

**Motivazione:** Ogni layer gestisce il proprio dominio, senza sovrapposizioni. La SPA Angular viene servita da Nginx in produzione, che è il posto corretto per CSP e HSTS.

**Conseguenze:**
- La SPA Angular non ha HSTS né CSP finché Nginx non è configurato per produzione
- HSTS e CSP devono essere aggiunti a Nginx prima del go-live su HTTPS
- In Nginx, il path `/api/` deve usare `^~` per non interferire con la location root della SPA

---

## ADR-004 — CORS configurabile via environment

**Stato:** implementato

**Problema:** Hardcodare le origini CORS nel codice impedisce il riuso dello stesso artefatto Docker in ambienti diversi (dev, staging, prod) senza modifiche al codice o rebuild.

**Decisione:** Le origini CORS vengono lette da `CORS_ALLOWED_ORIGINS` (env var), che supporta liste separate da virgola. Gestite tramite `CorsProperties` (`@ConfigurationProperties`) + `SecurityConfig`.

**Motivazione:** Un unico Docker image deployabile in qualsiasi ambiente senza modifiche. Configurazione esternalizzata come da Twelve-Factor App.

**Conseguenze:**
- Il deploy richiede che `CORS_ALLOWED_ORIGINS` sia sempre impostato (il backend non ha un fallback permissivo)
- `setAllowCredentials(true)` richiede origini esplicite — il wildcard `*` non è compatibile e non va usato

---

## ADR-005 — Rate limiting custom con Bucket4j, doppio bucket su login

**Stato:** implementato

**Problema:** Il login è il vettore principale di attacchi brute-force. Un singolo limite per IP non è sufficiente: un attaccante distribuito può tentare più username per lo stesso tenant usando IP diversi (attacco credential stuffing su account specifico).

**Decisione:** Rate limiter custom basato su Bucket4j con pattern a **doppio bucket** per il login:
- `IP_TENANT`: 20 req / 15 min (limite per IP + tenant — blocca attacchi concentrati)
- `TENANT_USERNAME`: 10 req / 15 min (limite per tenant + username — blocca attacchi distribuiti su account target)

I bucket sono namespaciati nel `PolicyRegistry` come `POLICY:KEY_TYPE:VALUE` per evitare collisioni tra policy diverse.

**Motivazione:** Il doppio bucket copre sia attacchi concentrati (stesso IP) sia attacchi distribuiti (stesso account target da IP diversi). Bucket4j non richiede infrastruttura aggiuntiva per un singolo nodo.

**Conseguenze:**
- In-memory → i contatori si azzerano al restart del backend; non distribuito tra istanze multiple
- Se il backend viene scalato orizzontalmente, occorre sostituire il provider in-memory con un backend condiviso (Redis)
- `PolicyRegistry` deve essere aggiornato se si aggiungono nuovi endpoint sensibili da proteggere

---

## ADR-006 — Sistema entitlement tipizzato (BooleanEntitlement / QuotaEntitlement)

**Stato:** infrastruttura parzialmente presente; enforcement nei servizi non documentato

**Problema:** Il progetto è destinato a diventare un SaaS con piani commerciali differenziati (Base, Pro, Enterprise). Servono meccanismi per gate feature e quote per tenant senza anticipare la logica di billing.

**Decisione:** Introdurre un sistema di entitlement tipizzato nel package `domain/entitlement`:
- `BooleanEntitlement` — entitlement on/off (es: `API_ACCESS`, `CALENDAR_VIEW`, `SMS_NOTIFICATIONS`)
- `QuotaEntitlement` — entitlement con quota numerica (es: `MAX_DOGS_PER_TENANT`, `MAX_BOOKINGS_PER_MONTH`)
- La tabella `module` (V11) gestisce l'attivazione feature a livello platform (`ModuleType.BASE` / `OPTIONAL`)

**Motivazione:** Preparare l'infrastruttura per i piani commerciali senza implementare Stripe o logica di billing prematuramente (YAGNI). Le strutture dati esistono; l'enforcement specifico viene aggiunto quando i piani sono definiti.

**Conseguenze:**
- Il sistema entitlement esiste come struttura dati ma l'enforcement effettivo nei servizi non è documentato — verificare nel codice prima di usarlo
- L'integrazione con Stripe è pianificata solo quando ci saranno clienti reali (Fase 4 della roadmap)

---

## ADR-007 — Multi-tenancy via discriminator column (no schema separation)

**Stato:** implementato

**Problema:** Esistono tre strategie principali di multi-tenancy: schema separato per tenant, database separato per tenant, discriminator column condiviso. La scelta impatta complessità operativa, performance e isolamento.

**Decisione:** Discriminator column (`tenant_id` UUID su ogni tabella di business). Tutti i tenant condividono lo stesso schema PostgreSQL e lo stesso database.

**Motivazione:** Semplicità operativa: un solo database da gestire, monitorare, fare backup. Adeguato alla fase attuale (centri cinofili, carico basso, numero di tenant moderato). Schema-per-tenant richiederebbe connection pool multipli e gestione dinamica dello schema.

**Conseguenze:**
- Tutti i repository devono filtrare per `tenant_id` — un'omissione espone dati cross-tenant (isolamento enforcement applicativo, non infrastrutturale)
- Regola: usare sempre `findBy...AndTenantId(...)`, mai query senza filtro tenant nelle API
- Migrazione a schema-per-tenant sarebbe costosa — scelta da rivalutare se i requisiti di compliance lo richiedessero

---

## ADR-008 — YAGNI come principio guida

**Stato:** principio attivo

**Problema:** In fase di sviluppo di un SaaS è facile anticipare feature che potrebbero non servire, aumentando complessità e costo di manutenzione senza valore immediato.

**Decisione:** Nessuna complessità aggiunta senza motivazione concreta e documentata. Feature future descritte nella roadmap e in `replit.md`, non anticipate nel codice. Ogni proposta di feature richiede discussione con il CTO (ruolo agente) prima dell'implementazione.

**Motivazione:** Codebase più semplice → meno debito tecnico → iterazioni più veloci → meno bug.

**Conseguenze:**
- Alcune aree coscientemente fuori scope attuale: waitlist, overbooking controllato, staff multipli, audit log
- Il processo obbligatorio per ogni modifica è: discuti → approva → implementa → testa → commit
