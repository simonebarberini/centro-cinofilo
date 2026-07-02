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

---

## ADR-009 — Cancellazione subscription come transizione di stato via SubscriptionAdminController dedicato

**Stato:** implementato

**Contesto:** Il platform backoffice (ADMIN_APP) deve poter disattivare un modulo attivo o in trial per un tenant. `SubscriptionService.cancel()` esiste già nel dominio; mancava solo la superficie REST.

**Problema:** Come esporre l'operazione di cancellazione — su quale controller, con quale metodo HTTP, e con quale semantica della risorsa?

**Decisione:**
- Nuovo controller dedicato `SubscriptionAdminController` su `/admin/subscriptions`
- Endpoint: `POST /admin/subscriptions/{tenantId}/{moduleKey}/cancel`
- Risposta: `204 No Content`
- Il controller delega interamente a `SubscriptionService.cancel()` senza aggiungere logica

**Motivazione:**

*Controller separato.* `TenantAdminController` è read-only per design: lista e dettaglio tenant, senza side effect. `AdminGrantController` gestisce l'attivazione con audit (creazione di `AdminGrant`). La cancellazione è una terza responsabilità distinta — una transizione di stato sul `TenantModule` senza record di audit. Tenerla in un controller dedicato rispetta la separazione delle responsabilità e mantiene ogni controller focalizzato su un'unica area del dominio.

*POST invece di DELETE.* `DELETE /admin/tenants/{tenantId}/modules/{moduleKey}` sarebbe semanticamente scorretto: nel modello di dominio la risorsa `TenantModule` non viene rimossa — il suo stato transisce da `ACTIVE` o `TRIAL` a `CANCELLED`. Il record rimane, l'occupazione del moduleKey nella history esiste. `DELETE` implica distruzione della risorsa; `POST .../cancel` esprime esplicitamente l'intenzione come comando di dominio. Questo allineamento tra HTTP e semantica di business riduce l'ambiguità per chiunque legga l'API e facilita l'aggiunta futura di audit sulla transizione stessa.

*Idempotenza della delega.* `SubscriptionService.cancel()` è idempotente: se il modulo è già `CANCELLED` o non esiste per quel tenant, non fa nulla senza errori. Il controller restituisce sempre `204` — nessun side effect visibile differenzia il caso "già cancellato" da "appena cancellato". Questo è intenzionale: l'ADMIN_APP non deve distinguere i due scenari, solo garantire che lo stato finale sia `CANCELLED`.

**Alternative scartate:**

`DELETE /admin/tenants/{tenantId}/modules/{moduleKey}` — scartato perché semanticamente errato: `DELETE` implica rimozione della risorsa dal sistema, non transizione di stato. Confonde chi legge l'API e rende difficile l'aggiunta di audit log futuri.

`PATCH /admin/tenants/{tenantId}/modules/{moduleKey}` con body `{status: "CANCELLED"}` — scartato perché espone lo stato interno del dominio come campo direttamente scrivibile. Il dominio non deve accettare stati arbitrari dall'esterno; deve ricevere comandi (`cancel`, `activate`) e gestire internamente la transizione valida.

`POST /admin/grants/{tenantId}/cancel` su `AdminGrantController` — scartato perché i grant sono record di audit immutabili che documentano l'attivazione. Cancellare un modulo non annulla il grant — sono concetti separati. Mescolarli in `AdminGrantController` violerebbe la responsabilità singola del controller.

**Conseguenze:**
- La surface REST del backoffice cresce in modo ordinato: lettura (`TenantAdminController`), attivazione con audit (`AdminGrantController`), cancellazione (`SubscriptionAdminController`)
- Aggiungere un audit log per la cancellazione in futuro richiede solo di estendere `SubscriptionService.cancel()` o il controller — senza modificare altri controller
- `SubscriptionAdminController` non contiene logica di dominio: delega a `SubscriptionService` e non conosce `TenantModule` direttamente

---

## ADR-010 — Platform Backoffice: layout separato, routing dedicato, assenza di TenantContext

**Stato:** implementato

**Contesto:** Il SaaS introduce un secondo tipo di utente — `ADMIN_APP` — che opera sulla piattaforma trasversalmente a tutti i tenant. L'app Angular esistente è costruita intorno al concetto di singolo tenant attivo: layout, navigazione e servizi assumono un `TenantContext` sempre presente.

**Problema:** Come integrare il Platform Backoffice nell'applicazione Angular esistente senza inquinare il codice tenant-aware con logica di piattaforma, e senza che le due aree si interferiscano nel routing, nel layout e nei servizi?

**Decisione:**
- `AdminLayoutComponent` separato da `MainLayoutComponent`, senza alcun riferimento a `TenantContext`
- `admin.routes.ts` dedicato, lazy-loaded come blocco autonomo da `app.routes.ts`
- `adminGuard` separato da `authGuard`, che verifica il claim `role === 'ADMIN_APP'` decodificando il JWT
- `AuthService.getRole()` aggiunto per leggere il claim singolo `"role"` dal payload JWT (decodifica base64 pura, senza dipendenze esterne)
- Nessun servizio tenant-scoped (`TenantApiService`, `TenantContext`) importato nell'area admin

**Motivazione:**

*Layout separato.* `MainLayoutComponent` è costruito attorno al contesto tenant: mostra il nome del centro, usa slug, e i nav item puntano a rotte tenant-specifiche. Un `ADMIN_APP` non ha un tenant attivo — ha visibilità su tutti i tenant. Condividere il layout introdurrebbe rami condizionali (`*ngIf isAdmin`) che frammentano la responsabilità del componente. Un layout separato mantiene ogni componente focalizzato sul proprio contesto.

*Routing dedicato.* Le rotte admin vivono sotto `/admin/**`, completamente separate da `/calendar`, `/customers` e le altre rotte tenant. Il lazy loading (`loadChildren`) garantisce che il bundle admin non venga mai scaricato da un utente `TENANT_OWNER` o `TENANT_STAFF`. Aggiungere una nuova sezione admin (es. `/admin/billing`) richiede solo una riga in `admin.routes.ts`, senza toccare `app.routes.ts` né le rotte tenant.

*Assenza di TenantContext.* `TenantContext` risolve il tenant attivo per l'utente corrente — concetto senza significato per `ADMIN_APP`, che opera su tutti i tenant contemporaneamente. Iniettare `TenantContext` nell'area admin produrrebbe errori silenziosi (tenant undefined) o richiederebbe guard aggiuntivi per sopprimerne l'uso. L'assenza totale è la scelta più sicura e più chiara.

*`adminGuard` distinto da `authGuard`.* `authGuard` verifica solo `isAuthenticated()`. `adminGuard` verifica `isAuthenticated() && role === 'ADMIN_APP'`. Tenerli separati permette di applicarli indipendentemente e mantiene ogni guard con una sola responsabilità. Un utente autenticato come `TENANT_OWNER` che tenta di accedere a `/admin/**` viene rediretto al login.

**Alternative scartate:**

*Sezione admin dentro `MainLayoutComponent`* con `*ngIf (isAdmin)` — scartata perché crea un componente con doppia personalità, viola il principio di singola responsabilità, e rende il codice tenant-specific difficile da ragionare quando contiene rami admin.

*Un unico `app.routes.ts` senza lazy loading* per le rotte admin — scartato perché il bundle admin (tabelle tenant, gestione subscription) verrebbe scaricato da ogni utente tenant, aumentando il tempo al primo interazione senza benefici.

*Decodifica JWT con libreria esterna* (es. `jwt-decode`) — scartata per ora: il payload JWT è già base64url-encoded e il campo `role` è un singolo string. `atob()` + `JSON.parse` sono sufficienti e non introducono dipendenze. Se il formato JWT dovesse cambiare (claims annidati, encryption), la libreria potrà essere introdotta allora.

**Conseguenze:**
- Un utente `TENANT_OWNER` o `TENANT_STAFF` che naviga a `/admin/**` viene rediretto a `/login`
- Le rotte admin sono un chunk separato: bundle tenant non contaminato da codice admin
- Aggiungere nuove sezioni admin (P4–P9) richiede modifiche solo a `admin.routes.ts` — zero impatto su `app.routes.ts` e sull'app tenant
- `AdminLayoutComponent` non potrà mai accedere accidentalmente a dati tenant-scoped: nessun servizio tenant è importato
