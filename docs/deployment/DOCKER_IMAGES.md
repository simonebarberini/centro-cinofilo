# Docker Images — Centro Cinofilo

**Analisi della struttura esistente e proposta di naming/versioning/registry. Nessun Dockerfile viene creato o modificato in questa milestone.**

## Struttura esistente (invariata)

- `backend/Dockerfile` — multi-stage (Maven build → JRE Alpine runtime), immagine finale ~180 MB, utente non-root.
- `frontend/Dockerfile` — multi-stage (Node build → Nginx Alpine runtime), immagine finale ~45 MB.

Entrambi già seguono le best practice principali (multi-stage, immagini Alpine, nessun tool di build nell'immagine finale, utente non privilegiato per il backend). Non si propone alcuna modifica strutturale ai Dockerfile in questa milestone — solo la formalizzazione di naming e versionamento delle immagini che ne derivano.

## Immagini da produrre

Due immagini, una per componente, coerenti con l'architettura a due container già esistente:

| Immagine | Contenuto |
|---|---|
| `centro-cinofilo-backend` | JAR Spring Boot + JRE 21 |
| `centro-cinofilo-frontend` | Build statica Angular + Nginx |

Non si propongono immagini aggiuntive (es. immagine "all-in-one"): l'architettura a due container separati è corretta e va mantenuta, permette di scalare/aggiornare i due componenti in modo indipendente.

## Naming

```
ghcr.io/simonebarberini/centro-cinofilo/backend
ghcr.io/simonebarberini/centro-cinofilo/frontend
```

Namespace basato sul repository GitHub già esistente (`simonebarberini/centro-cinofilo`), per coerenza diretta tra codice sorgente e immagini pubblicate.

## Registry consigliato: GitHub Container Registry (GHCR)

**Motivazione:**
- Il repository è già su GitHub: nessun account o servizio terzo aggiuntivo da collegare.
- Integrazione nativa con GitHub Actions: autenticazione tramite `GITHUB_TOKEN`, generato automaticamente ad ogni run, senza dover creare e gestire un secret separato (a differenza di Docker Hub, che richiederebbe un access token dedicato salvato nei secret del repo).
- Permessi di visibilità (pubblico/privato) gestiti insieme a quelli del repository stesso.
- Nessun limite di pull rate stringente per immagini private come quello imposto da Docker Hub sugli account gratuiti (rilevante per un VPS che fa `pull` regolarmente in fase di deploy).

**Alternative scartate:**
- **Docker Hub:** più universalmente noto, ma richiede un secret di autenticazione separato da mantenere e ha rate limit sui pull più stringenti sul piano gratuito. Nessun vantaggio concreto rispetto a GHCR per questo progetto.
- **AWS ECR / Google Artifact Registry:** richiederebbero un account cloud aggiuntivo (AWS/GCP) con relative credenziali da gestire, quando l'infrastruttura di deploy prevista è un singolo VPS Hetzner indipendente da quei provider — complessità non giustificata.
- **Registry self-hosted:** aggiunge un servizio in più da mantenere e mettere in sicurezza (autenticazione, TLS, backup del registry stesso) per un beneficio marginale rispetto a un registry gestito gratuito come GHCR.

## Convenzione completa dei tag Docker

Nota terminologica: dal rinominamento del branch Git `dev` → `develop` (vedi ADR-024), il branch di integrazione e il tag Docker mobile che ne segue la punta condividono lo stesso nome, `develop` — non c'è più bisogno di distinguere un "nome branch" da un "nome tag": entrambi si riferiscono in modo inequivocabile allo stesso concetto (l'ultimo stato integrato).

**Stato implementazione (CI-03, vedi ADR-025):** solo i tag di **integrazione** (`sha-<shortsha>`, `develop`) sono oggi pubblicati realmente su GHCR, dal workflow `build-images.yml`. I tag di **release** (`vX.Y.Z`, `vX.Y.Z-rc.N`, `latest`) restano puro design in questa tabella: la loro pubblicazione effettiva è rimandata, per scelta architetturale, alla futura pipeline "Release" (vedi `CICD.md`, sezione 3), non ancora implementata — insieme a GitHub Release, changelog ed eventuale deploy. La separazione è intenzionale: tiene distinta la Continuous Integration (build/pubblicazione continua di sviluppo) dalla Release Management (produzione di artefatti ufficiali), per non dover rifattorizzare `build-images.yml` quando il progetto introdurrà release vere e proprie.

Entrambe le immagini (`backend`, `frontend`) seguono esattamente la stessa convenzione di tag:

| Tag | Immagine/i | Generato da | Quando viene pubblicato | Mutabilità | Uso | Stato |
|---|---|---|---|---|---|---|
| `sha-<shortsha>` | backend, frontend | Ogni commit pushato su `develop` **o** `main` | Workflow "Build Images" (job `publish`), ad ogni push | Immutabile (un tag = un commit preciso, mai sovrascritto) | Tracciabilità esatta: risalire dall'immagine al commit sorgente | **Implementato** |
| `develop` | backend, frontend | Ultimo commit di `develop` — **solo** per push su `develop`, mai per push su `main` | Workflow "Build Images" (job `publish`), ad ogni push su `develop` (sovrascrive il precedente `develop`) | Mutabile (si sposta ad ogni push su `develop`) | Ultima build di integrazione continua, utilizzabile per un eventuale ambiente di staging futuro | **Implementato** |
| `vX.Y.Z-rc.N` | backend, frontend | Tag Git di release candidate, creato su `develop` (vedi `GITFLOW.md`) | Workflow "Release" (o una sua variante), al push del tag `-rc.N` | Immutabile | Verifica pre-release in un ambiente di stabilizzazione, prima del rilascio definitivo | Design, non implementato |
| `vX.Y.Z` | backend, frontend | Tag Git di release stabile, creato su `main` | Workflow "Release", al push del tag `vX.Y.Z` | Immutabile — **mai ripubblicato con contenuto diverso** | **Il tag da usare in produzione**, sempre esplicito nei file Compose | Design, non implementato |
| `latest` | backend, frontend | Stesso build del tag `vX.Y.Z` più recente su `main` | Workflow "Release", subito dopo la pubblicazione di `vX.Y.Z` (ri-taggata sulla stessa immagine, non ricostruita) | Mutabile (si sposta ad ogni nuova release stabile) | Solo comodità per chi esplora il registry manualmente (`docker pull ...:latest` per provare l'ultima versione) — **da non usare nei file di Compose di produzione** | Design, non implementato |

**Regola importante:** i file Docker Compose di produzione devono sempre puntare a un tag esplicito e immutabile (`vX.Y.Z`), mai a `latest` o `develop`. Usare un tag mutabile in produzione rende il deploy non deterministico (un `docker compose pull` in due momenti diversi potrebbe scaricare immagini diverse senza che nessuno abbia cambiato la configurazione) e complica il rollback (non si sa più con certezza quale versione era in esecuzione prima).

**Nota su `vX.Y.Z-rc.N`:** con l'eliminazione del branch `release/*` (vedi `GITFLOW.md`), le immagini per una release candidate verranno comunque costruite e pubblicate quando la pipeline "Release" sarà implementata — cambia solo il branch di origine (`develop` invece di un branch `release/*` dedicato), non il meccanismo di tagging.

**Nota sulla visibilità del package GHCR:** al primo push, GHCR crea il package come privato e non lo collega automaticamente al repository — è richiesta un'azione manuale una tantum su GitHub (Package settings → collegamento al repository e scelta della visibilità) dopo il primo publish. Se il package resta privato, il futuro deploy sul VPS dovrà autenticarsi con un Personal Access Token `read:packages` (nuovo secret, fuori scope della milestone CI-03) invece che con `GITHUB_TOKEN`.
