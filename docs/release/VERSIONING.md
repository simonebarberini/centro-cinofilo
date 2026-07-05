# Versioning — Centro Cinofilo

## Stato attuale (rilevato dall'analisi del repository)

- Tag Git esistenti: `v0.1.0`, `v0.2.0`.
- `backend/pom.xml` → `<version>1.0.0</version>`.
- `frontend/package.json` → `"version": "1.0.0"`.

**Incoerenza da correggere:** i numeri di versione nei manifest (`pom.xml`, `package.json`) non corrispondono ancora ai tag Git più recenti, e non sono ancora nel formato `-SNAPSHOT` deciso in ADR-027 — correzione manuale da fare come milestone dedicata, prima del prossimo tag di release, non eseguita da questa milestone (che ha introdotto solo la policy documentata, non la correzione dei valori attuali). Con la strategia descritta sotto, la versione nei manifest deve sempre essere `X.Y.Z-SNAPSHOT` durante lo sviluppo, dove `X.Y.Z` è la prossima milestone pianificata.

**Verifica automatica (implementata, vedi ADR-026):** il workflow `release.yml` (job `verify-tag`) verifica, ad ogni push di un tag `vX.Y.Z`, che `project.version` in `backend/pom.xml` e `version` in `frontend/package.json` corrispondano esattamente al tag (senza il prefisso `v`) — se non corrispondono, la pipeline fallisce **prima** di costruire o pubblicare qualunque immagine, con un messaggio che indica quale manifest va corretto. È un controllo di sola verifica (fail-fast), non un bump automatico: l'aggiornamento dei manifest resta un passo manuale nel processo di release (vedi `RELEASE_PROCESS.md`, step "Release").

## Strategia: Semantic Versioning (SemVer)

Formato: `MAJOR.MINOR.PATCH` (es. `1.4.2`), con prerelease opzionale (`1.4.2-rc.1`).

| Incremento | Quando | Esempi di modifica in questo progetto |
|---|---|---|
| **MAJOR** | Breaking change per i tenant o per l'API pubblica | Cambio del formato di autenticazione (JWT claims), rimozione/rinomina di un endpoint API senza retrocompatibilità, migrazione DB non additiva che richiede downtime o coordinamento manuale |
| **MINOR** | Nuove funzionalità retrocompatibili | Nuovo modulo (es. gestione staff), nuovo endpoint, nuova entità che non modifica il comportamento esistente |
| **PATCH** | Bugfix e hotfix retrocompatibili | Fix di un bug, fix di sicurezza, correzione di validazione |

### Fase `0.y.z` (attuale e confermata per un periodo più lungo)

Il progetto è oggi in fase `0.x` (pre-1.0), coerente con la semantica SemVer: **API e schema possono ancora cambiare in modo incompatibile anche in un MINOR**, perché il prodotto non è ancora considerato stabile/commerciale.

**Decisione esplicita (revisione):** il progetto resta in `0.y.z` più a lungo di quanto inizialmente ipotizzato. Non basta il completamento delle fasi funzionali (Hardening + Commercializzazione) per arrivare a `1.0.0`: **finché non esistono anche una pipeline CI/CD funzionante, un processo di deploy verificato e una minima operatività (backup, rollback, monitoring) effettivamente in uso, il progetto non può dirsi "1.0" in senso pieno** — `1.0.0` comunica una promessa di stabilità che riguarda tanto il prodotto quanto il modo in cui viene rilasciato e gestito in produzione.

**Regola di passaggio a `1.0.0` (aggiornata):** si taglia la prima release `1.0.0` solo quando **tutte** le condizioni seguenti sono vere:
1. FASE 1 (Hardening) è completata;
2. FASE 2 (Commercializzazione: ruoli, staff, sospensione tenant, audit log) è completata;
3. **la milestone Release & Deployment è implementata e operativa**: pipeline CI/CD attive, almeno un deploy in produzione eseguito con il processo descritto in `RELEASE_PROCESS.md`, backup verificati con almeno un restore di prova riuscito (vedi `docs/deployment/SERVER.md`), rollback documentato ed eseguibile;
4. il prodotto è pronto per il primo cliente pagante reale.

Da quel momento, MAJOR/MINOR/PATCH seguono la disciplina SemVer completa (niente più breaking change in MINOR).

### Percorso di versioni fino a `1.0.0` (coerente con i tag già esistenti, ordinato per maturità del prodotto — vedi ADR-027)

I tag già presenti in repository sono un fatto storico da cui partire, non da rinumerare. **Decisione esplicita (vedi ADR-027):** la mappa versione→milestone riflette la **maturità del prodotto per chi legge i tag dall'esterno**, non l'ordine cronologico con cui le singole milestone sono state effettivamente implementate. Questo ha una conseguenza concreta: la milestone CI/CD (Validate, Build Images, Publish Images, Release Pipeline — vedi ADR-021/023/025/026) è già stata completata nella pratica, ma il numero di versione che le corrisponde (`v0.4.0`) viene comunque dopo quello riservato al completamento dell'Hardening (`v0.3.0`), perché un cliente o un collaboratore che confronta i numeri di versione deve poter leggere "quanto il **prodotto** è vicino alla produzione stabile", non "in che ordine lo sviluppatore ha scelto di lavorare".

| Versione | Stato | Contenuto |
|---|---|---|
| `v0.1.0` | già taggato | Versione iniziale del progetto |
| `v0.2.0` | già taggato | Base attuale (hardening in corso: JWT, CORS, rate limiting, security header, fix race condition — vedi ADR-001 e ADR successive) |
| `v0.3.0` | proposto, futuro | Completamento FASE 1 — Hardening (Actuator protetto, limitazione range calendario, fix N+1, paginazione) |
| `v0.4.0` | proposto, futuro (implementazione già completata, resta da tagliare) | CI/CD + Release completa: pipeline Validate, Build Images, Publish Images, Release (vedi ADR-021, ADR-023, ADR-025, ADR-026) |
| `v0.5.0` | proposto, futuro | Deployment operativo: pipeline "Manual Deploy" implementata, almeno un deploy reale in produzione eseguito, backup con restore di prova riuscito, rollback documentato ed eseguibile |
| `v0.6.0` | proposto, futuro | Completamento FASE 2 — Commercializzazione (staff, ruoli granulari, sospensione tenant, soft delete, audit log) |
| `v1.0.0` | non prima di quanto sopra | Primo rilascio stabile in produzione con cliente reale: tutte le condizioni della regola di passaggio sopra, verificate |

Questo percorso non è rigido: se durante l'implementazione emergono modifiche incompatibili con l'API o lo schema (lecite in `0.x`), possono giustificare comunque un incremento di MINOR dedicato, mantenendo l'ordine logico sopra.

## Versione di sviluppo: convenzione `-SNAPSHOT` (vedi ADR-027)

Tra una release e la successiva, `backend/pom.xml` e `frontend/package.json` **non** riportano mai il numero esatto dell'ultima release taggata: riportano il numero della **prossima** milestone pianificata, con suffisso `-SNAPSHOT` (es. dopo aver taggato `v0.3.0`, entrambi i manifest passano a `0.4.0-SNAPSHOT`).

**Perché anche `package.json`, che non ha una convenzione nativa `-SNAPSHOT`:** il progetto usa un'**unica versione di repository** per backend e frontend (vedi sezione successiva) — introdurre due convenzioni diverse (`-SNAPSHOT` solo lato Maven, un numero "pulito" o un altro suffisso lato npm) romperebbe questa unicità e renderebbe impossibile, guardando i due file, capire a colpo d'occhio se si è allineati. Si adotta quindi la stessa stringa `X.Y.Z-SNAPSHOT` su entrambi, come valore convenzionale del progetto (non richiesto da npm, ma non in conflitto con esso: resta una stringa di versione valida).

**Ciclo di vita della versione, ad ogni release:**
1. **Bump a versione finale** — commit dedicato su `develop`, immediatamente prima di aprire la PR di release verso `main`: si rimuove il suffisso `-SNAPSHOT`, il manifest riporta il numero esatto che sta per essere taggato (es. `0.4.0-SNAPSHOT` → `0.4.0`).
2. **Merge e tag** — la PR `develop` → `main` porta questo commit; al merge si tagga `vX.Y.Z` su `main` (vedi `RELEASE_PROCESS.md`). Il job `verify-tag` di `release.yml` (ADR-026) verifica che questo valore coincida esattamente col tag — la sua logica non cambia con questa policy, perché al momento del tag il manifest è già "pulito", mai in `-SNAPSHOT`.
3. **Creazione della release** — pipeline "Release" pubblica le immagini `vX.Y.Z`/`latest` e la GitHub Release (invariato, vedi ADR-026).
4. **Bump immediato alla versione di sviluppo successiva** — subito dopo, un nuovo commit **su `develop`** imposta il manifest alla **prossima milestone pianificata** in questa tabella con suffisso `-SNAPSHOT` (es. dopo `v0.4.0`, si passa a `0.5.0-SNAPSHOT`, non a un generico `0.4.1-SNAPSHOT`) — coerente con la scelta di questo progetto di usare MINOR per ogni salto di maturità, non PATCH.

**Cosa NON è ancora automatizzato (deliberatamente, vedi ADR-027):** i quattro passi sopra sono oggi **manuali**. Nessun job di CI/CD esegue bump automatico dei manifest, apertura di PR o commit diretti. L'automazione (es. Maven Release Plugin, `npm version`, o uno script dedicato invocato da un workflow) è una possibile milestone futura, da valutare solo dopo aver eseguito manualmente questo ciclo almeno una volta.

## Versionamento a livello di repository (monorepo)

Backend e frontend **non vengono versionati in modo indipendente**. Si usa un'unica versione di repository (`vX.Y.Z`), applicata sia a `backend/pom.xml` sia a `frontend/package.json`, per un motivo preciso: **backend e frontend non sono librerie pubblicate e consumate separatamente**, sono due metà della stessa unità deployabile e vengono sempre rilasciati e deployati insieme (stesso tag → stessa coppia di immagini Docker). Versioni indipendenti (es. `backend v2.3.0` + `frontend v1.8.0`) avrebbero senso solo se i due componenti fossero rilasciabili in modo disaccoppiato, il che non è il caso qui (il frontend dipende da un contratto API specifico del backend in quel momento).

**Alternativa scartata:** versioning indipendente per componente (stile microservizi con release train separati). Scartata perché aggiunge complessità di tracciamento ("quale versione frontend è compatibile con quale versione backend?") senza benefici concreti per un'architettura a due componenti sempre co-deployati.

## Quando creare un tag Git

Un tag `vX.Y.Z` si crea **una sola volta**, nel momento in cui una release diventa stabile su `main` (vedi `GITFLOW.md` e `RELEASE_PROCESS.md` per il flusso completo). Non si taggano commit su `develop` o su branch `feature/*`.

## Convenzione dei tag

```
v<MAJOR>.<MINOR>.<PATCH>            → release stabile          es. v1.4.0
v<MAJOR>.<MINOR>.<PATCH>-rc.<N>     → release candidate         es. v1.4.0-rc.1
v<MAJOR>.<MINOR>.<PATCH>-beta.<N>   → prerelease (se necessario) es. v1.4.0-beta.1
```

- Il prefisso `v` è obbligatorio (convenzione universale, riconosciuta da GitHub Releases e dalla maggior parte dei tool di changelog automatico).
- Le release candidate si taggano **direttamente da `develop`** (non esiste più un branch `release/*` dedicato — vedi `GITFLOW.md` per la motivazione della rimozione).
- Una RC che supera i test diventa la release stabile: si ritagga lo stesso commit come `vX.Y.Z` (senza suffisso), non se ne crea uno nuovo con modifiche.

## Milestone e fasi

Le fasi della Roadmap (`docs/ai/ROADMAP.md`) e la milestone Release & Deployment si mappano sulle versioni così:

| Fase | Range di versione previsto |
|---|---|
| FASE 1 — Hardening | `0.2.x` → `0.3.0` alla chiusura |
| CI/CD + Release (Validate, Build Images, Publish Images, Release Pipeline) | `0.4.0` alla chiusura |
| Deployment operativo (Manual Deploy, primo deploy reale, backup/rollback verificati) | `0.5.0` alla chiusura — **prerequisito per `1.0.0`, vedi sopra** |
| FASE 2 — Commercializzazione | `0.6.0` alla chiusura — **prerequisito per `1.0.0`, vedi sopra** |
| FASE 3 — Feature Flag Engine | `1.x.y` (dopo `1.0.0`) |
| FASE 4 — Piano commerciale (Base/Pro/Enterprise) | `1.x.y` o `2.0.0` se introduce breaking change (es. cambio del modello di autorizzazione) |

## Convenzione dei messaggi di commit

Si raccomanda **Conventional Commits** (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`, `BREAKING CHANGE:` nel footer) come standard per i messaggi di commit su `feature/*` e `hotfix/*`.

**Perché questa scelta e non un formato libero:**
- È uno standard ampiamente adottato, non un'invenzione custom.
- Permette in futuro (non ora) la generazione automatica del changelog e persino il calcolo automatico del prossimo numero di versione (tool come `semantic-release` o `commitizen`), senza dover implementare nulla di proprietario.
- Rende leggibile la history anche senza tool aggiuntivi.

**Alternativa scartata:** messaggi di commit liberi con changelog scritto a mano ad ogni release. Scartata perché il progetto è già cresciuto oltre la dimensione in cui un changelog manuale rimane accurato e aggiornato nel tempo.
