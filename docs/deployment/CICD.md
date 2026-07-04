# CI/CD Pipeline Design — Centro Cinofilo

**Stato implementazione:** la pipeline **"Pull Request"** (sezione 1) è **implementata** in `.github/workflows/validate.yml` (vedi ADR-021), con una differenza rispetto al design originale: oltre a `pull_request` si attiva anche su `push` verso `main`/`dev` (scelta transitoria, vedi ADR-021). Le pipeline **"Push su dev"**, **"Release"** e **"Manual Deploy"** (sezioni 2-4) restano solo design, non ancora implementate: richiedono build/push di immagini Docker e deploy, esplicitamente fuori scope della milestone corrente.
## Perché GitHub Actions (scelta per l'implementazione futura, solo indicata qui)

Il repository è già ospitato su GitHub (`origin` → `github.com/simonebarberini/centro-cinofilo`). GitHub Actions è la scelta naturale perché nativa alla piattaforma già in uso (nessun account/servizio terzo da collegare), con integrazione diretta al Container Registry di GitHub (GHCR) tramite `GITHUB_TOKEN` senza credenziali aggiuntive da gestire (vedi `DOCKER_IMAGES.md`). Alternative come GitLab CI, CircleCI o Jenkins richiederebbero uno spostamento del repository o un servizio esterno aggiuntivo, senza un vantaggio concreto per questo progetto.

## Le quattro pipeline

### 1. Workflow "Pull Request" — **implementato** come `validate.yml`

| | |
|---|---|
| **Trigger** | `pull_request` verso `dev` o `main`, **più `push`** verso `dev` o `main` (differenza rispetto al design originale, vedi ADR-021) |
| **Job 1 — Backend** | `mvn -B test` (solo Surefire/unit test, nessun Docker richiesto — coerente con ADR-018), con cache Maven nativa (`actions/setup-java`) |
| **Job 2 — Frontend** | `npm ci` + `npm run build` + `ng test --watch=false --browsers=ChromeHeadless`, con cache npm nativa (`actions/setup-node`) |
| **Ordine di esecuzione** | Job 1 e Job 2 **in parallelo** (indipendenti, nessuna dipendenza tra backend e frontend a livello di build/unit test) |
| **Dipendenze** | Nessuna tra i due job |
| **Artifact prodotti** | Nessuno (report di test/coverage allegati come artifact è un possibile miglioramento futuro, non incluso ora) |
| **Esito** | Check di stato su PR/push; con branch protection attiva in futuro, blocca il merge se rosso |
| **File** | `.github/workflows/validate.yml` |

### 2. Workflow "Push su dev"

| | |
|---|---|
| **Trigger** | `push` su `dev` (tipicamente conseguenza del merge di una PR) |
| **Job 1 — Backend: build + unit test** | Come sopra |
| **Job 2 — Frontend: build + unit test** | Come sopra |
| **Job 3 — Build immagine backend** | Solo se Job 1 passa. Build Dockerfile esistente, tag `sha-<shortsha>` e `develop` |
| **Job 4 — Build immagine frontend** | Solo se Job 2 passa. Build Dockerfile esistente, tag `sha-<shortsha>` e `develop` |
| **Job 5 — Push immagini su registry** | Dopo Job 3 e Job 4, push su GHCR |
| **Ordine di esecuzione** | (Job 1 → Job 3) e (Job 2 → Job 4) in parallelo tra loro, poi Job 5 dopo entrambi |
| **Dipendenze** | Job 3 dipende da Job 1, Job 4 dipende da Job 2, Job 5 dipende da Job 3 + Job 4 |
| **Artifact prodotti** | Immagini Docker `backend:develop` / `:sha-<shortsha>`, `frontend:develop` / `:sha-<shortsha>` su GHCR (vedi convenzione completa in `DOCKER_IMAGES.md`) |

### 3. Workflow "Release"

| | |
|---|---|
| **Trigger** | `push` di un tag `v*.*.*` o `v*.*.*-rc.*` (le RC si taggano direttamente su `dev`, le release stabili al merge `dev` → `main` — non esiste più un branch `release/*`, vedi `GITFLOW.md`) |
| **Job 1 — Suite completa** | `mvn verify` (Surefire + Failsafe, quindi anche i 101 Integration Test con Testcontainers — richiede un runner con Docker disponibile, es. i runner standard `ubuntu-latest` di GitHub Actions, che supportano Docker-in-Docker nativamente, a differenza del sandbox Replit) |
| **Job 2 — Build immagini versionate** | Solo se Job 1 passa. Tag `vX.Y.Z` + `latest` per entrambe le immagini |
| **Job 3 — Push su registry** | Dopo Job 2 |
| **Job 4 — GitHub Release** | Dopo Job 3. Genera le release notes (da Conventional Commits, vedi `VERSIONING.md`) e crea la GitHub Release associata al tag |
| **Ordine di esecuzione** | Sequenziale: 1 → 2 → 3 → 4 (ogni job blocca il successivo: non si pubblica un'immagine se i test falliscono) |
| **Dipendenze** | Ognuno dal precedente |
| **Artifact prodotti** | Immagini Docker `vX.Y.Z` + `latest` su GHCR, GitHub Release con changelog |

### 4. Workflow "Manual Deploy"

| | |
|---|---|
| **Trigger** | Manuale (`workflow_dispatch`), con input: `versione` (tag da deployare, es. `v1.4.0`) e `ambiente` (es. `production`, o `staging` se introdotto in futuro) |
| **Job 1 — Verifica immagine esistente** | Controlla che le immagini per il tag richiesto esistano su GHCR |
| **Job 2 — Deploy** | Connessione SSH al VPS, aggiornamento delle variabili di versione nel Compose (o file `.env` con `IMAGE_TAG=vX.Y.Z`), `docker compose pull` + `docker compose up -d` |
| **Job 3 — Healthcheck** | Verifica endpoint di salute backend/frontend post-deploy |
| **Job 4 — Notifica esito** | Successo/fallimento (canale da definire: email, chat — non ancora deciso, fuori scope di questa analisi) |
| **Ordine di esecuzione** | Sequenziale: 1 → 2 → 3 → 4. Se Job 3 (healthcheck) fallisce, il job segnala l'esigenza di rollback manuale (vedi `RELEASE_PROCESS.md`) — il rollback automatico non è nello scope iniziale, per mantenere un controllo umano esplicito su un'azione così critica |
| **Dipendenze** | Ognuno dal precedente |
| **Artifact prodotti** | Nessun nuovo artifact; consuma le immagini già pubblicate dal workflow "Release" |

**Perché il deploy è manuale e non automatico ad ogni tag:** vedi motivazione in `RELEASE_PROCESS.md` (step 8) — con un solo ambiente di produzione e nessuno staging, un controllo umano esplicito prima del deploy in produzione riduce il rischio, senza rallentare eccessivamente il ciclo (un click su un workflow manuale non è un processo lento).

## Alternative scartate

- **Un unico workflow monolitico per tutto (PR + push + release + deploy in un solo file):** scartato perché mischia trigger e responsabilità diverse, rendendo il file difficile da mantenere e i log difficili da leggere ("perché la pipeline di una PR ha provato a fare il deploy?").
- **Deploy automatico su ogni push a `main`:** scartato per il motivo di controllo umano già spiegato.
- **CD tramite polling da parte del server (es. Watchtower che aggiorna automaticamente ai nuovi tag):** scartato perché toglie tracciabilità esplicita di *chi* e *quando* ha deployato una versione, e rende più complesso il rollback controllato.
