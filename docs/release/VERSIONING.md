# Versioning — Centro Cinofilo

## Stato attuale (rilevato dall'analisi del repository)

- Tag Git esistenti: `v0.1.0`, `v0.2.0`.
- `backend/pom.xml` → `<version>1.0.0</version>`.
- `frontend/package.json` → `"version": "1.0.0"`.

**Incoerenza da correggere in una futura milestone di implementazione** (non in questa, che è solo di analisi): i numeri di versione nei manifest (`pom.xml`, `package.json`) non corrispondono ai tag Git più recenti. Con la strategia descritta sotto, la versione nei manifest deve sempre coincidere con l'ultimo tag di release stabile.

## Strategia: Semantic Versioning (SemVer)

Formato: `MAJOR.MINOR.PATCH` (es. `1.4.2`), con prerelease opzionale (`1.4.2-rc.1`).

| Incremento | Quando | Esempi di modifica in questo progetto |
|---|---|---|
| **MAJOR** | Breaking change per i tenant o per l'API pubblica | Cambio del formato di autenticazione (JWT claims), rimozione/rinomina di un endpoint API senza retrocompatibilità, migrazione DB non additiva che richiede downtime o coordinamento manuale |
| **MINOR** | Nuove funzionalità retrocompatibili | Nuovo modulo (es. gestione staff), nuovo endpoint, nuova entità che non modifica il comportamento esistente |
| **PATCH** | Bugfix e hotfix retrocompatibili | Fix di un bug, fix di sicurezza, correzione di validazione |

### Fase `0.y.z` (attuale)

Il progetto è oggi in fase `0.x` (pre-1.0), coerente con la semantica SemVer: **API e schema possono ancora cambiare in modo incompatibile anche in un MINOR**, perché il prodotto non è ancora considerato stabile/commerciale. Questo è coerente con lo stato della Roadmap (FASE 1 — Hardening, in corso).

**Regola di passaggio a `1.0.0`:** si taglia la prima release `1.0.0` quando:
1. FASE 1 (Hardening) è completata, e
2. FASE 2 (Commercializzazione: ruoli, staff, sospensione tenant, audit log) è completata, e
3. il prodotto è pronto per il primo cliente pagante reale.

Da quel momento, MAJOR/MINOR/PATCH seguono la disciplina SemVer completa (niente più breaking change in MINOR).

## Versionamento a livello di repository (monorepo)

Backend e frontend **non vengono versionati in modo indipendente**. Si usa un'unica versione di repository (`vX.Y.Z`), applicata sia a `backend/pom.xml` sia a `frontend/package.json`, per un motivo preciso: **backend e frontend non sono librerie pubblicate e consumate separatamente**, sono due metà della stessa unità deployabile e vengono sempre rilasciati e deployati insieme (stesso tag → stessa coppia di immagini Docker). Versioni indipendenti (es. `backend v2.3.0` + `frontend v1.8.0`) avrebbero senso solo se i due componenti fossero rilasciabili in modo disaccoppiato, il che non è il caso qui (il frontend dipende da un contratto API specifico del backend in quel momento).

**Alternativa scartata:** versioning indipendente per componente (stile microservizi con release train separati). Scartata perché aggiunge complessità di tracciamento ("quale versione frontend è compatibile con quale versione backend?") senza benefici concreti per un'architettura a due componenti sempre co-deployati.

## Quando creare un tag Git

Un tag `vX.Y.Z` si crea **una sola volta**, nel momento in cui una release diventa stabile su `main` (vedi `GITFLOW.md` e `RELEASE_PROCESS.md` per il flusso completo). Non si taggano commit su `dev` o su branch `feature/*`.

## Convenzione dei tag

```
v<MAJOR>.<MINOR>.<PATCH>            → release stabile          es. v1.4.0
v<MAJOR>.<MINOR>.<PATCH>-rc.<N>     → release candidate         es. v1.4.0-rc.1
v<MAJOR>.<MINOR>.<PATCH>-beta.<N>   → prerelease (se necessario) es. v1.4.0-beta.1
```

- Il prefisso `v` è obbligatorio (convenzione universale, riconosciuta da GitHub Releases e dalla maggior parte dei tool di changelog automatico).
- Le release candidate si taggano da un branch `release/*` (se usato, vedi `GITFLOW.md`) o direttamente da `dev` quando il progetto è troppo piccolo per giustificare un branch di stabilizzazione dedicato.
- Una RC che supera i test diventa la release stabile: si ritagga lo stesso commit come `vX.Y.Z` (senza suffisso), non se ne crea uno nuovo con modifiche.

## Milestone e fasi

Le fasi della Roadmap (`docs/ai/ROADMAP.md`) si mappano naturalmente sulle versioni:

| Fase Roadmap | Range di versione previsto |
|---|---|
| FASE 1 — Hardening | `0.x.y` (in corso) |
| FASE 2 — Commercializzazione | `0.x.y` → chiusura fase = trigger per `1.0.0` |
| FASE 3 — Feature Flag Engine | `1.x.y` |
| FASE 4 — Piano commerciale (Base/Pro/Enterprise) | `1.x.y` o `2.0.0` se introduce breaking change (es. cambio del modello di autorizzazione) |

## Convenzione dei messaggi di commit

Si raccomanda **Conventional Commits** (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`, `BREAKING CHANGE:` nel footer) come standard per i messaggi di commit su `feature/*` e `hotfix/*`.

**Perché questa scelta e non un formato libero:**
- È uno standard ampiamente adottato, non un'invenzione custom.
- Permette in futuro (non ora) la generazione automatica del changelog e persino il calcolo automatico del prossimo numero di versione (tool come `semantic-release` o `commitizen`), senza dover implementare nulla di proprietario.
- Rende leggibile la history anche senza tool aggiuntivi.

**Alternativa scartata:** messaggi di commit liberi con changelog scritto a mano ad ogni release. Scartata perché il progetto è già cresciuto oltre la dimensione in cui un changelog manuale rimane accurato e aggiornato nel tempo.
