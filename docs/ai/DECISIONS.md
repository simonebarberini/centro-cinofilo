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

---

## ADR-011 — Platform Dashboard come homepage del backoffice, introdotta prima delle feature operative

**Stato:** implementato

**Contesto:** Il Platform Backoffice ha ora un'infrastruttura di routing e layout (ADR-010), ma nessuna pagina di destinazione. La sidebar prevede una voce Dashboard, Tenant, Catalog. Le due funzionalità operative (gestione tenant, catalogo moduli) richiedono ancora più milestone (P5–P9).

**Problema:** Cosa deve mostrare l'utente `ADMIN_APP` quando accede a `/admin` prima ancora che le funzionalità operative siano complete? Serve una destinazione di default coerente con la struttura a sidebar già approvata.

**Decisione:** Introdurre `DashboardComponent` come homepage del backoffice, raggiungibile su `/admin/dashboard`, con redirect automatico da `/admin` (path vuoto) a `/admin/dashboard`. Il contenuto in questa milestone è limitato a: titolo, descrizione testuale del ruolo del backoffice, e un'area a griglia strutturalmente pronta per le future card di riepilogo — senza dati, senza chiamate HTTP, senza servizi Angular.

**Motivazione:**

*Perché una homepage prima delle feature operative.* La Dashboard non è una feature nel senso stretto — è il punto di atterraggio che rende l'area `/admin` navigabile fin da subito. Senza di essa, `/admin` risulterebbe una rotta vuota o richiederebbe di saltare direttamente alla prima feature operativa disponibile (Tenant List), il che accoppierebbe la struttura di navigazione all'ordine di sviluppo delle milestone. Con la Dashboard come default, l'ordine di implementazione delle feature (P5 Tenant, P6 Catalog, future P7+) resta indipendente dalla struttura del routing.

*Perché nessun dato reale in questa milestone.* Le metriche di piattaforma (numero tenant, moduli attivi, trial in scadenza) dipendono da endpoint che non esistono ancora e da decisioni di aggregazione non ancora prese (es. quali metriche contano, con quale frequenza di refresh). Costruire card con dati reali ora significherebbe anticipare milestone successive e introdurre servizi Angular con chiamate HTTP prima che il loro contratto sia stato discusso e approvato — in violazione del principio YAGNI del progetto (ADR-008). L'area a griglia esiste già nel markup proprio per non richiedere un refactor del layout quando le card verranno aggiunte.

**Alternative scartate:**

*Redirect diretto da `/admin` a `/admin/tenants`* (saltare la dashboard) — scartato perché lega la homepage della piattaforma alla prima feature operativa sviluppata, invece che a un concetto stabile e duraturo. Se in futuro Tenant smettesse di essere la prima voce di menu, il redirect andrebbe riscritto.

*Card con dati finti (mock/lorem ipsum)* — scartato esplicitamente dal requisito "nessun placeholder tecnico": dati finti in produzione comunicano funzionalità inesistenti e possono confondere l'amministratore di piattaforma sul reale stato del sistema.

*Rimandare la Dashboard a dopo Tenant e Catalog* — scartato perché la sidebar è già stata approvata con Dashboard come prima voce (ADR-010): posticiparla lascerebbe un link morto in un'interfaccia già visibile all'utente ADMIN_APP.

**Conseguenze:**
- `/admin` e `/admin/dashboard` sono equivalenti per l'utente; la sidebar punta esplicitamente a `/admin/dashboard`
- Quando le metriche di piattaforma saranno definite (numero tenant, subscription attive, ecc.), si aggiungeranno componenti card dentro l'area già presente in `dashboard.component.html`, senza toccare il routing
- `DashboardComponent` resta senza `constructor` e senza injection finché non verrà introdotto un servizio dedicato alle metriche — nessuna dipendenza da rimuovere in seguito

---

## ADR-012 — Platform Catalog come sezione autonoma, sola lettura, con service dedicato

**Stato:** implementato

**Contesto:** Il backend espone già `GET /admin/catalog` e `GET /admin/catalog/{moduleKey}` (`CatalogAdminController`, protetto da `hasRole('ADMIN_APP')`), usati finora solo internamente al dominio Catalog. Il Platform Backoffice ha ora Dashboard (P4) come homepage e una sidebar con voce "Catalog" ancora non collegata a nessuna pagina.

**Problema:** Come esporre il catalogo dei moduli di piattaforma nel backoffice, riusando esclusivamente endpoint già esistenti, senza introdurre alcuna capacità di modifica del catalogo in questa milestone?

**Decisione:** Introdurre `CatalogListComponent` su `/admin/catalog`, un `CatalogAdminService` dedicato che avvolge i due endpoint esistenti, e un modello `CatalogModule` che rispecchia `ModuleResponse`. La pagina è sola lettura: tabella con Nome, Module Key, Tipo, Stato, più una ricerca client-side sul nome (filtro in memoria su `getAll()`, nessuna chiamata di rete aggiuntiva).

**Motivazione:**

*Perché il Catalog è una sezione autonoma del backoffice.* Il catalogo moduli è un concetto di piattaforma indipendente dal singolo tenant (a differenza di clienti/cani/prenotazioni, che sono tenant-scoped) ed è già un bounded context separato nel backend (package `catalog`, `CatalogAdminController` distinto da `TenantAdminController` e `SubscriptionAdminController`). Rispecchiare questo confine anche nel frontend — una route, un service, un modello dedicati — mantiene la corrispondenza 1:1 tra bounded context di backend e sezione di UI, evitando che la UI mescoli concetti che il dominio tiene separati.

*Perché sola lettura in questa fase.* Gli endpoint di scrittura sul catalogo (creazione/modifica/eliminazione modulo) non esistono ancora lato backend, e introdurli non è nello scope di questa milestone. Costruire una UI di modifica senza le API sottostanti significherebbe anticipare sia il contratto di quegli endpoint sia le regole di business che dovranno validarli (es. impatto su subscription attive quando un modulo viene disattivato) — decisioni che vanno discusse ed approvate a parte, non dedotte implicitamente dalla UI.

*Perché `CatalogAdminService` è separato dagli altri service Angular.* I service esistenti (`CustomersApiService`, `DogsApiService`, `TenantApiService`, ecc.) parlano tutti con endpoint tenant-scoped e assumono implicitamente un tenant corrente. `CatalogAdminService` parla con endpoint `/admin/**` protetti da ruolo di piattaforma, non da tenant — mescolarlo con i service tenant creerebbe un'illusione di omogeneità che non esiste a livello di autorizzazione e di dominio. La stessa separazione è già stata scelta per `AdminLayoutComponent` rispetto a `MainLayoutComponent` (ADR-010): un service admin colloca coerentemente questo confine anche nel layer di accesso ai dati.

**Alternative scartate:**

*Riutilizzare un service esistente aggiungendo metodi per il catalogo* — scartato per lo stesso motivo che ha portato a separare `AdminLayoutComponent`: mescolare responsabilità tenant-scoped e piattaforma-scoped nello stesso service rende ambiguo, a colpo d'occhio, quale endpoint richieda quale contesto di autorizzazione.

*Aggiungere subito azioni di modifica (toggle stato, form di modifica)* — scartato perché richiederebbe endpoint di scrittura non ancora progettati né approvati, in violazione del flusso "una feature alla volta" e del principio YAGNI (ADR-008).

*Ricerca lato server (query param su `GET /admin/catalog`)* — scartato per questa milestone: il catalogo moduli è un insieme piccolo e a bassa cardinalità (i moduli di un SaaS sono decine, non migliaia), per cui un filtro client-side su dati già caricati è sufficiente e non richiede di modificare il controller esistente, rispettando il vincolo "nessuna modifica al backend".

**Conseguenze:**
- `/admin/catalog` mostra l'elenco completo dei moduli con Nome, Module Key, Tipo, Stato; la ricerca filtra solo lato client sul nome già caricato
- `CatalogAdminService.getByKey()` è disponibile fin da ora (avvolge un endpoint già esistente) ma non è ancora utilizzato da nessun componente — verrà consumato dalla futura pagina di dettaglio modulo, senza richiedere modifiche al service
- Nessuna azione di scrittura è presente in UI: aggiungerla in futuro richiederà endpoint dedicati, una ADR propria e approvazione esplicita

---

## ADR-013 — Tenant List: ricerca server-side, `TenantAdminService` come unico punto di accesso, pagina operativa principale del backoffice

**Stato:** implementato

**Contesto:** Il backend espone già `GET /admin/tenants?q=` (`TenantAdminController.search`, `TenantAdminService.search` lato backend, protetto da `hasRole('ADMIN_APP')`), realizzato in P1 ma mai consumato da alcuna UI. La sidebar del backoffice ha una voce "Tenant" non ancora collegata.

**Problema:** Come esporre l'elenco e la ricerca dei tenant nel backoffice, riusando esclusivamente `GET /admin/tenants?q=`, senza introdurre paginazione, modifica o eliminazione, e gestendo correttamente la digitazione dell'utente durante la ricerca?

**Decisione:** Introdurre `TenantListComponent` su `/admin/tenants`, un `TenantAdminService` Angular dedicato (`search(q)`), e un modello `TenantSummary` che rispecchia `TenantSummaryResponse`. La ricerca è inviata al backend tramite il parametro `q` ad ogni digitazione, con `debounceTime` + `distinctUntilChanged` + `switchMap` per annullare automaticamente le richieste precedenti non ancora risolte. La tabella mostra Nome, Slug, Tipo, Capacità box, Data creazione, con un pulsante "Apri" per la futura navigazione al dettaglio.

**Motivazione:**

*Perché la ricerca tenant è lato backend, non client-side (a differenza del Catalog, ADR-012).* La cardinalità dei tenant cresce con il business (potenzialmente centinaia o migliaia in un SaaS multi-tenant maturo), a differenza del catalogo moduli che resta piccolo per natura. Caricare l'intero elenco tenant nel browser per poi filtrarlo localmente non scala e anticipa un problema di performance che il backend già risolve: l'endpoint `GET /admin/tenants?q=` esiste apposta per delegare la ricerca al database. Riusarlo lato server è quindi la scelta coerente con la cardinalità del dominio, non solo con l'API disponibile.

*Perché `debounceTime` + `distinctUntilChanged` + `switchMap`.* Una ricerca server-side ad ogni tasto digitato produrrebbe una richiesta HTTP per carattere. `debounceTime` limita la frequenza delle chiamate a quando l'utente fa una pausa nella digitazione; `distinctUntilChanged` evita di ripetere la stessa richiesta se il testo non è cambiato nella sostanza; `switchMap` garantisce che solo l'ultima richiesta in ordine di tempo determini il risultato mostrato, annullando le precedenti ancora in volo — evitando il classico bug di "race condition di rete" in cui una risposta lenta a una ricerca precedente sovrascrive quella più recente.

*Perché `TenantAdminService` è il punto unico di accesso alle API tenant di piattaforma.* Tutte le operazioni amministrative sui tenant (ricerca oggi, dettaglio e gestione moduli nelle milestone future P7+) condividono lo stesso `TenantAdminController` sul backend. Concentrarle in un solo service Angular, invece di crearne uno per pagina, evita la duplicazione di `baseUrl` e la dispersione dei contratti HTTP: quando P7 aggiungerà il dettaglio tenant, si aggiungerà un metodo a questo stesso service, non un service parallelo.

*Perché Tenant List è la pagina operativa principale del Platform Backoffice.* A differenza di Dashboard (punto di atterraggio, ADR-011) e Catalog (consultazione di configurazione, ADR-012), la gestione dei tenant è l'attività quotidiana per cui il backoffice viene usato: verificare chi è cliente, aprirne il dettaglio, intervenire sulle sue subscription. Le prossime milestone (dettaglio, gestione moduli, sospensione) si costruiscono tutte a partire da questa lista — è il punto di ingresso naturale verso il resto del dominio "gestione tenant".

**Alternative scartate:**

*Ricerca client-side come per il Catalog (ADR-012)* — scartato perché la cardinalità e le prospettive di crescita dei due domini sono diverse: il catalogo moduli resta piccolo per natura, l'elenco tenant no. Applicare la stessa scelta a entrambi ignorerebbe questa differenza.

*Debounce manuale con `setTimeout`* — scartato in favore degli operatori RxJS standard di Angular (`debounceTime`, `distinctUntilChanged`, `switchMap`), già disponibili in `rxjs` senza dipendenze aggiuntive e idiomatici per questo tipo di problema (ricerca reattiva) nell'ecosistema Angular.

*Un service per la lista e uno separato per il futuro dettaglio* — scartato perché entrambi parlano con lo stesso controller backend (`TenantAdminController`); frammentarli introdurrebbe due `baseUrl` identici da mantenere sincronizzati senza alcun beneficio.

*Implementare già ora paginazione o azioni di modifica/eliminazione* — scartato perché non richiesto in questa milestone e perché introdurrebbe parametri di query o endpoint non ancora concordati, in violazione del principio YAGNI (ADR-008) e del flusso "una feature alla volta".

**Conseguenze:**
- `/admin/tenants` mostra la lista tenant con ricerca server-side reattiva; digitare rapidamente non genera richieste multiple concorrenti grazie a `switchMap`
- Il pulsante "Apri" naviga già a `/admin/tenants/:id`, rotta non ancora implementata (verrà aggiunta in P7): comportamento accettato, coerente con il precedente della sidebar in P3 (link presenti prima della pagina di destinazione)
- `TenantAdminService` avrà nuovi metodi (`getById`, `getModules`) aggiunti in milestone future, senza necessità di un nuovo service né di duplicare `baseUrl`

---

## ADR-014 — Tenant Detail Shell: caricamento unico, stato condiviso via route-scoped provider, tab bar su child routes

**Stato:** implementato

**Contesto:** P6 ha introdotto Tenant List con un pulsante "Apri" che naviga a `/admin/tenants/:tenantId`, rotta finora inesistente. Il backend espone già `GET /admin/tenants/{tenantId}` (`TenantAdminService.findById` lato backend). Le future milestone P8 (tab Info) e P9 (tab Moduli) dovranno mostrare, in viste diverse, gli stessi dati del tenant già caricato.

**Problema:** Come strutturare la pagina di dettaglio tenant in modo che (a) il tenant venga caricato una sola volta all'ingresso nella pagina, (b) i futuri tab possano leggere quei dati senza rifare la chiamata HTTP, (c) la tab bar e il contenuto del tab attivo siano gestiti dal router in modo standard Angular?

**Decisione:** `TenantDetailComponent` è una shell: legge `tenantId` dalla route, chiama `TenantAdminService.getById()` una sola volta in `ngOnInit`, gestisce loading/error, e pubblica il risultato in `TenantDetailState` — un piccolo servizio Angular (`BehaviorSubject` + getter sincrono) registrato con `providers: [TenantDetailState]` a livello della route `tenants/:tenantId` (non `providedIn: 'root'`). La rotta ha `children` (ancora vuoti, popolati in P8/P9) e la shell renderizza la tab bar (Info, Moduli) più `<router-outlet>` per il tab attivo.

**Motivazione:**

*Perché `TenantDetailComponent` è una shell.* Il componente non contiene né conterrà logica specifica di un singolo tab (i campi anagrafici in P8, i moduli attivi in P9): la sua unica responsabilità è portare in vita i dati del tenant e la struttura di navigazione condivisa da tutti i tab. Separare "chi carica il dato" da "chi lo mostra" evita che ogni tab futuro debba ripetere la gestione di loading/error/route-param, e mantiene i tab liberi di concentrarsi solo sulla propria porzione di UI.

*Perché il caricamento del tenant avviene una sola volta.* `TenantDetailComponent` non viene distrutto e ricreato quando l'utente passa da un tab all'altro — solo il contenuto dentro `<router-outlet>` cambia. Chiamare `getById()` in `ngOnInit` della shell (anziché in ciascun tab) garantisce che il dato venga recuperato esattamente una volta per visita alla pagina di dettaglio, indipendentemente da quanti tab l'utente apre in sequenza: nessuna richiesta duplicata, nessun disallineamento tra tab che vedono versioni diverse dello stesso tenant.

*Perché i tab condividono lo stesso stato.* `TenantDetailState` è registrato nell'array `providers` della route `tenants/:tenantId`, non del singolo componente. In Angular, i `providers` di una route creano un injector condiviso da quella route e da tutte le sue route figlie: questo significa che ogni tab (child route) riceve automaticamente la stessa istanza di `TenantDetailState` popolata dalla shell, senza passaggi manuali di dati (`@Input`) attraverso il router-outlet — meccanismo che il router Angular non supporta nativamente per le route figlie. Usare `providedIn: 'root'` sarebbe stato scorretto: renderebbe lo stato globale all'intera applicazione anche dopo aver lasciato la pagina di dettaglio, con il rischio di mostrare dati di un tenant precedente alla prima apertura di un tenant successivo.

*Perché il router usa child routes.* La tab bar (Info/Moduli) rappresenta viste alternative all'interno dello stesso tenant, non pagine indipendenti: usare `children` sulla route `tenants/:tenantId` permette a ciascun tab di avere un proprio URL (`/admin/tenants/:id/info`, `/admin/tenants/:id/modules`), supportando bookmark diretti, back/forward del browser e lazy loading indipendente per tab — mantenendo al contempo un unico punto di ingresso (`TenantDetailComponent`) che monta la shell una sola volta per l'intera sessione di navigazione tra tab.

**Alternative scartate:**

*Passare il tenant come `@Input()` ai tab tramite binding manuale* — scartato perché il router Angular non inietta automaticamente `@Input()` nei componenti caricati da `<router-outlet>` per le route figlie nello stesso modo in cui lo fa con `withComponentInputBinding()` per i soli route param — e comunque richiederebbe che la shell conoscesse in anticipo l'interfaccia di ogni tab, accoppiandola alle implementazioni future.

*`TenantDetailState` con `providedIn: 'root'`* — scartato perché renderebbe lo stato del tenant visibile globalmente e persistente oltre il ciclo di vita della pagina di dettaglio, causando potenziali dati stantii se l'utente naviga da un tenant a un altro.

*Ricaricare il tenant in ciascun tab tramite `TenantAdminService.getById()` diretto* — scartato perché duplicherebbe la chiamata HTTP ad ogni cambio di tab, contraddicendo il requisito esplicito di un caricamento unico e introducendo la possibilità che due tab mostrino contemporaneamente due risposte diverse dello stesso endpoint.

*Tab come sezioni nella stessa pagina (`*ngIf` su una proprietà `activeTab`, senza routing)* — scartato perché elimina la possibilità di URL diretti ai singoli tab e non è coerente con il pattern "tab bar + router-outlet" già indicato nei requisiti della milestone.

**Conseguenze:**
- `TenantDetailComponent` non ha logica di dominio propria: aggiungere un nuovo tab in futuro richiede solo un nuovo componente che inietta `TenantDetailState` e una nuova child route, senza toccare la shell
- La tab bar è già navigabile verso `info` e `modules`, rotte non ancora implementate (verranno aggiunte in P8/P9): comportamento accettato, stesso precedente già adottato per Tenant List (ADR-013) e la sidebar (P3)
- Se in futuro un tab dovesse modificare il tenant (es. dopo un salvataggio), potrà chiamare `TenantDetailState.setTenant()` per aggiornare lo stato condiviso senza dover ricaricare l'intera shell

**Debito tecnico consapevole:** `TenantDetailState` è stato introdotto come state route-scoped per evitare future duplicazioni del caricamento del tenant tra i tab. Se il numero di tab dovesse rimanere limitato (Info + Modules), questa scelta dovrà essere rivalutata durante una futura Architecture Review per verificare che non rappresenti over-engineering.

---

## ADR-015 — Tenant Info Tab: vista read-only sul dato già caricato dalla shell, nessuna chiamata HTTP propria

**Stato:** implementato

**Contesto:** P7 ha introdotto `TenantDetailComponent` come shell che carica il tenant una sola volta e lo pubblica in `TenantDetailState` (ADR-014), con una tab bar che punta a `info` e `modules`, entrambe finora prive di componente. Il tab `info` è il primo consumatore reale di `TenantDetailState`.

**Problema:** Come mostrare i dati anagrafici del tenant (Nome, Slug, Tipo, Capacità box, Email billing, Data creazione) nel tab Info, senza duplicare la chiamata HTTP già effettuata dalla shell e senza introdurre logica che non sia di semplice presentazione?

**Decisione:** `TenantInfoTabComponent` inietta `TenantDetailState` (lo stesso provider registrato a livello della route `tenants/:tenantId`, ADR-014) e si limita a leggere `tenant$` nel template tramite l'`async` pipe. Non inietta `TenantAdminService`, non effettua alcuna chiamata `http`, non contiene metodi oltre al costruttore: è un componente di sola presentazione dei campi richiesti.

**Motivazione:**

*Perché il tab Info riutilizza il dato già caricato dalla shell.* `TenantDetailState` esiste esattamente per questo scopo (ADR-014): essere l'unico canale attraverso cui i tab accedono al tenant corrente, popolato una sola volta dalla shell. Il tab Info è il primo caso reale che dimostra perché quello stato condiviso è stato introdotto — leggerlo da lì, anziché richiederlo di nuovo, è l'applicazione diretta della decisione già presa in P7, non una scelta nuova.

*Perché non esegue chiamate HTTP autonome.* Se ogni tab richiamasse `TenantAdminService.getById()` per conto proprio, la garanzia di "caricamento unico" stabilita in ADR-014 diventerebbe solo teorica: il primo tab reale l'avrebbe già infranta. Vietare l'accesso diretto a `TenantAdminService` dal componente tab rende impossibile la duplicazione della richiesta anche per errore, ed è coerente con il requisito esplicito della milestone.

*Perché è una vista esclusivamente read-only.* Non esistono ancora endpoint di modifica dei dati anagrafici del tenant lato backend, e introdurli non è nello scope di questa milestone. Un form di modifica senza un'API di scrittura sottostante anticiperebbe sia il contratto di quell'endpoint sia le regole di validazione che dovrà applicare — decisioni da discutere e approvare a parte, non da dedurre implicitamente costruendo prima la UI.

**Alternative scartate:**

*Iniettare `TenantAdminService` anche nel tab, per coerenza con `TenantDetailComponent`* — scartato perché vanificherebbe il motivo stesso per cui `TenantDetailState` è stato introdotto in P7: se un tab può comunque richiamare il servizio direttamente, lo stato condiviso diventa un canale opzionale anziché l'unico punto di accesso al dato.

*Usare il getter sincrono `TenantDetailState.tenant` invece dell'observable `tenant$` nel template* — scartato perché il getter richiede un accesso imperativo (es. in `ngOnInit`) e non si aggiorna automaticamente se lo stato cambiasse in futuro (es. dopo un salvataggio in un tab di modifica); l'`async` pipe su `tenant$` mantiene il componente reattivo senza subscribe/unsubscribe manuali.

*Aggiungere già ora pulsanti "Modifica" disabilitati o placeholder visivi* — scartato per lo stesso motivo emerso in P4 (ADR-011): comunicherebbe funzionalità non ancora esistenti, in violazione del principio "nessun placeholder tecnico" applicato coerentemente in tutte le milestone del backoffice.

**Conseguenze:**
- `TenantInfoTabComponent` non ha dipendenze da `TenantAdminService`: qualunque futura richiesta di aggiungere una chiamata diretta andrà trattata come una deviazione dal pattern stabilito, non come la norma
- Se `TenantDetailState` venisse rimosso in una futura Architecture Review (nota di debito tecnico in ADR-014), il tab Info sarebbe il primo componente da aggiornare, insieme alla shell
- Il tab non prevede alcuno stato locale di editing: quando la modifica del tenant sarà approvata come milestone propria, sarà verosimilmente un componente separato (es. `TenantEditFormComponent`) o un'estensione esplicitamente approvata di questo tab

---

## ADR-016 — Tenant Modules Tab: join lato frontend tra catalogo e moduli del tenant, `SubscriptionAdminService` come unico punto di accesso HTTP per le sottoscrizioni

**Stato:** implementato

**Contesto:** P9 introduce il secondo tab della shell tenant (ADR-014), quello che mostra i moduli disponibili per il tenant e ne permette l'attivazione (`grant`) o la disattivazione (`cancel`). A differenza del tab Info (ADR-015), qui il dato non è già disponibile in `TenantDetailState`: servono due fonti distinte, il catalogo globale dei moduli (`GET /admin/catalog`, già esposto da `CatalogAdminService`) e lo stato dei moduli per il tenant specifico (`GET /admin/tenants/{tenantId}/modules`, finora non richiamato dal frontend).

**Decisione:**
1. `TenantModulesTabComponent` legge `tenantId` dalla route padre (`route.parent.snapshot.paramMap`), non da `TenantDetailState`: lo stato condiviso resta la fonte del *tenant* già caricato dalla shell (nome, slug, ecc. — usati dall'header comune), non delle informazioni sui moduli, che appartengono a un dominio diverso (sottoscrizioni) e non sono mai stati parte di quello stato.
2. Il componente carica in parallelo, con `forkJoin`, il catalogo (`CatalogAdminService.getAll()`, esistente, invariato) e i moduli del tenant (nuovo `SubscriptionAdminService.getTenantModules()`), poi unisce i due risultati con un mapping esplicito (`buildViewModel`) in un modello di vista (`TenantModuleViewModel`) consumato dal template.
3. `SubscriptionAdminService` (nuovo, `providedIn: 'root'`) è l'unico service che parla con gli endpoint `/admin/tenants/{tenantId}/modules`, `/admin/grants` e `/admin/subscriptions/{tenantId}/{moduleKey}/cancel`. Nessuna chiamata HTTP relativa a moduli/sottoscrizioni del tenant viene fatta da `TenantAdminService` o da `CatalogAdminService`.
4. Dopo `grant()` o `cancel()`, il componente richiama solo `SubscriptionAdminService.getTenantModules()` e ricostruisce il modello di vista riusando il catalogo già in memoria (`this.catalog`, popolato al primo caricamento): non richiama né `CatalogAdminService.getAll()` né `TenantAdminService.getById()`.

**Motivazione:**

*Perché Catalog e Tenant Modules vengono caricati separatamente.* Sono due risorse con ciclo di vita indipendente: il catalogo è globale e cambia raramente (nuovi moduli, deprecazioni), lo stato dei moduli di un tenant cambia a ogni grant/cancel. Il backend le espone già come due endpoint distinti su due controller distinti (`CatalogAdminController`, `TenantAdminController`) e non esiste, né viene richiesto in questa milestone, un endpoint che le unisca lato server. Caricarle separatamente rispetta il contratto REST esistente senza modificarlo, come richiesto esplicitamente dalla milestone.

*Perché il join avviene nel frontend.* Con il contratto attuale è l'unica opzione compatibile col vincolo "nessuna modifica al backend, al dominio o ai controller esistenti": introdurre un endpoint aggregato lato server sarebbe la soluzione più pulita a regime, ma richiederebbe un nuovo DTO e una nuova rotta, entrambi fuori scope. Il join è realizzato con una `Map` chiave-valore sui `moduleKey` del catalogo e uno `stream`/`map` esplicito in `buildViewModel()`, non nel template: l'HTML resta binding puro e condizioni su campi già calcolati (`m.tenantStatus`), senza logica di trasformazione.

*Perché dopo grant/cancel viene ricaricata solo la lista dei moduli del tenant.* Un'operazione di grant o cancel modifica esclusivamente lo stato di sottoscrizione del tenant, non il catalogo (i moduli disponibili restano gli stessi) né i dati anagrafici del tenant (nome, slug, ecc., già in `TenantDetailState` e non toccati da questa azione). Ricaricare anche catalogo e dettaglio tenant sarebbe una chiamata di rete inutile ad ogni azione, e per il dettaglio tenant significherebbe anche violare la garanzia di caricamento unico stabilita in ADR-014. Il catalogo resta quindi in cache locale (`this.catalog`) tra un caricamento e l'altro, e solo i moduli del tenant vengono richiesti di nuovo.

*Perché `SubscriptionAdminService` contiene tutta la logica HTTP relativa alle sottoscrizioni.* `GET /admin/tenants/{tenantId}/modules`, `POST /admin/grants` e `POST /admin/subscriptions/{tenantId}/{moduleKey}/cancel` sono, dal punto di vista del backend, esposti da due controller diversi (`TenantAdminController` per la lettura, `AdminGrantController` e `SubscriptionAdminController` per le mutazioni), ma rappresentano concettualmente un unico dominio applicativo: "quali moduli sono attivi per questo tenant, e come cambiano". Raggruppare le tre chiamate in un solo service frontend allinea il confine del service al dominio di business (le sottoscrizioni), non alla frammentazione dei controller REST lato server, evitando che la stessa informazione (stato modulo/tenant) sia raggiungibile da più service diversi nel frontend.

**Alternative scartate:**

*Esporre `getTenantModules()` da `TenantAdminService`, dato che l'URL è sotto `/admin/tenants/{tenantId}`* — scartato perché avrebbe sparso la logica delle sottoscrizioni su due service (`TenantAdminService` per la lettura, `SubscriptionAdminService` per grant/cancel), rendendo meno ovvio dove cercare "tutta la logica di sottoscrizione" e contraddicendo il requisito esplicito della milestone di concentrarla in un unico service.

*Fare il merge di catalogo e moduli del tenant nel template con un pipe custom* — scartato perché sposterebbe la logica di join fuori da un punto testabile e centralizzato (`buildViewModel`), in violazione del requisito esplicito "nessuna trasformazione complessa nell'HTML".

*Ricaricare l'intero tab (`loadAll()`, incluso il catalogo) dopo ogni grant/cancel, per semplicità implementativa* — scartato perché contraddice esplicitamente il requisito della milestone e introduce una chiamata di rete ridondante ad ogni azione, senza alcun beneficio: il catalogo non cambia mai come effetto di un grant o di una cancel.

**Conseguenze:**
- `SubscriptionAdminService` diventa il punto di estensione naturale per qualunque futura operazione sulle sottoscrizioni (es. Grant History in una milestone futura): andrà aggiunta lì, non in un nuovo service parallelo
- Il modello di vista `TenantModuleViewModel` è locale al tab (`tenants/tenant-module-view.model.ts`): se in futuro altri componenti avessero bisogno della stessa vista aggregata, andrà valutato lo spostamento in una posizione condivisa, non duplicato
- La cache locale del catalogo (`this.catalog`) vive solo per la durata di vita del componente tab: alla navigazione fuori e dentro il tab, il catalogo viene ricaricato da capo: comportamento accettato, coerente con l'assenza di uno stato condiviso per i moduli (a differenza del tenant, che resta in `TenantDetailState`)

---

## ADR-017 — Platform Admin Bootstrap: creazione idempotente dell'unico ADMIN_APP, tramite un tenant tecnico di sistema

**Stato:** implementato

**Contesto:** Il Platform Backoffice (P3–P9) richiede un utente con ruolo `ADMIN_APP` per essere utilizzabile, ma non esiste oggi alcun modo di crearne uno: `AuthService.register()` crea sempre un tenant cliente con utente `TENANT_OWNER`; non esiste un endpoint né un seed dedicato all'amministratore di piattaforma. Il `DevDataSeeder` esistente crea un tenant demo con dati applicativi, cosa esplicitamente da evitare per questa milestone.

Esplorando il dominio per implementare il bootstrap è emerso un vincolo esistente non aggirabile senza modificare il dominio: `AppUser.tenant` è una relazione `@ManyToOne(optional = false)` con colonna `tenant_id NOT NULL` e unique constraint `(tenant_id, username)`; inoltre l'unico flusso di login esistente (`AuthService.login()`) risolve prima il tenant per `slug` e poi l'utente per `tenantId + username`. Non esiste un percorso di login "senza tenant": il metodo `AppUserRepository.findByUsername()` esiste nel repository ma non è usato da alcun controller.

**Problema:** Come creare un unico utente `ADMIN_APP`, in modo idempotente, utilizzabile da subito per accedere al Platform Backoffice con il login esistente, senza modificare il dominio (`AppUser`, `AuthService`, lo schema) e senza introdurre alcun dato applicativo demo (clienti, cani, prenotazioni, moduli)?

**Decisione:** Un nuovo `PlatformAdminBootstrap` (`@Configuration` con `ApplicationRunner`, eseguito ad ogni avvio dell'applicazione, in tutti i profili) esegue, in una singola transazione:
1. Cerca un `Tenant` per lo slug configurato (`platform-admin.tenant.slug`, default `platform`). Se non esiste, ne crea uno nuovo — un tenant **tecnico di sistema**, non un tenant demo: nome/slug configurabili, `capacityBoxes=0`, nessun cliente, cane, prenotazione o modulo mai creato o associato ad esso. Esiste esclusivamente per soddisfare il vincolo `NOT NULL` di `AppUser.tenant` e il flusso di login tenant-scoped già esistente, non introduce alcuna modifica al dominio (l'entità `Tenant` e il suo utilizzo restano quelli già previsti dal modello attuale).
2. Verifica se esiste già un `AppUser` con quello username in quel tenant (`AppUserRepository.existsByTenantIdAndUsername`). Se sì, non fa nulla. Se no, crea l'unico `AppUser` con ruolo `ADMIN_APP`, `enabled=true`, `emailVerified=true`, password codificata con lo stesso `PasswordEncoder` (`BCryptPasswordEncoder`) già usato da `AuthService`.
3. Username, email, password dell'admin e nome/slug del tenant tecnico sono esternalizzati in configurazione (`platform-admin.*`), nessun valore hardcoded nel codice. Le credenziali (`username`, `email`, `password`) non hanno default in `application.yml`: come `jwt.secret`, l'applicazione si rifiuta di avviarsi se non sono configurate esplicitamente (env var o profilo). Valori di comodo per lo sviluppo locale sono definiti solo in `application-dev.yml`.
4. Riusa `TenantRepository` e `AppUserRepository` esistenti così come sono: nessuna query nativa, nessun bypass della validazione JPA o della logica di dominio.

**Motivazione:**

*Perché viene creato automaticamente un solo ADMIN_APP.* È l'unico account necessario per iniziare a usare il Platform Backoffice; da lì in poi, la gestione di eventuali altri operatori di piattaforma è fuori scope di questa milestone di stabilizzazione (FASE 2 prevede "Gestione utenti staff" più avanti, ma per i tenant, non per la piattaforma). Creare più admin per default aumenterebbe la superficie di attacco senza alcun beneficio a questo stadio.

*Perché non vengono creati dati demo.* Il `DevDataSeeder` esistente già copre l'esigenza di dati di sviluppo (tenant demo, owner, ecc.) dietro il profilo `dev`; questa milestone ha uno scope volutamente più stretto e infrastrutturale: rendere login-abile il backoffice in **qualsiasi** ambiente (incluso, in prospettiva, la produzione), dove dati demo non devono mai esistere. Tenere le due responsabilità separate (bootstrap admin vs seed dati di sviluppo) evita che l'una dipenda o interferisca con l'altra.

*Perché il tenant tecnico non è una violazione del vincolo "nessun tenant demo".* Un tenant "demo" è caratterizzato dai dati applicativi che lo accompagnano (clienti, cani, prenotazioni) pensati per simulare un centro reale a scopo dimostrativo/di sviluppo. Il tenant qui creato non ha e non avrà mai nessuno di questi dati: è un artefatto puramente tecnico, necessario solo perché il modello dati attuale non consente un `AppUser` senza tenant. La distinzione è stata esplicitamente confermata dall'utente prima dell'implementazione.

*Perché il bootstrap è idempotente.* Viene eseguito ad ogni avvio dell'applicazione (non solo alla prima installazione): deve poter girare ripetutamente — in dev ad ogni riavvio, in produzione ad ogni deploy — senza duplicare il tenant tecnico né l'utente admin, né sovrascrivere una password eventualmente già cambiata manualmente dall'amministratore reale dopo il primo login. Il controllo di esistenza (`findBySlug` per il tenant, `existsByTenantIdAndUsername` per l'utente) precede ogni creazione, ed è verificato con un test dedicato che simula due esecuzioni consecutive.

*Perché le credenziali sono esternalizzate nella configurazione.* Nessun valore hardcoded nel codice sorgente evita che credenziali (anche solo di sviluppo) finiscano in git in chiaro nel codice Java, e permette a ogni ambiente di avere credenziali diverse senza ricompilare. Il pattern (nessun default in `application.yml`, default solo in `application-dev.yml`) è lo stesso già adottato per `JWT_SECRET`: è già un precedente consolidato nel progetto per distinguere "segreto di sviluppo, va bene un default noto" da "segreto che l'ambiente deve fornire esplicitamente".

**Alternative scartate:**

*Rendere `AppUser.tenant` nullable e adattare `AuthService.login()` a un percorso senza tenant* — scartato perché modifica il dominio e il flusso di autenticazione esistente, esplicitamente vietato per questa milestone; impatto anche su unique constraint, generazione JWT e `TenantContext`, complessità e rischio sproporzionati per una milestone di stabilizzazione.

*Estendere `DevDataSeeder` per includere anche l'ADMIN_APP* — scartato perché `DevDataSeeder` è vincolato al profilo `dev` e crea deliberatamente dati demo: il bootstrap dell'admin di piattaforma deve invece poter girare in ogni ambiente, con uno scope (nessun dato demo) diverso e non sovrapponibile.

*Usare una migrazione Flyway per inserire l'utente admin* — scartato perché la password dovrebbe essere già hashata al momento della migrazione (senza poter usare il `PasswordEncoder` Spring configurato a runtime, violando il requisito esplicito "stesso PasswordEncoder usato dal resto dell'applicazione"), e perché una migrazione applicata una sola volta non è il meccanismo naturale per un'operazione che deve essere ri-verificata (anche se no-op) ad ogni avvio.

**Conseguenze:**
- Ogni ambiente (dev, staging, produzione) deve configurare `PLATFORM_ADMIN_USERNAME`, `PLATFORM_ADMIN_EMAIL`, `PLATFORM_ADMIN_PASSWORD` (e opzionalmente `PLATFORM_ADMIN_TENANT_NAME`/`_SLUG`) prima del primo avvio, esattamente come già avviene per `JWT_SECRET`
- Il tenant tecnico "platform" comparirà nelle liste tenant del backoffice stesso (es. Tenant List, P6) come un tenant a tutti gli effetti: non è nascosto o filtrato. Un eventuale filtro per escluderlo dalle viste cliente-facing non è nello scope di questa milestone e andrà valutato solo se diventa un problema concreto d'uso
- Il bootstrap non gestisce la rotazione della password: se cambiata manualmente dall'admin dopo il primo login, resta tale ad ogni riavvio successivo, perché il controllo di esistenza impedisce qualunque sovrascrittura
