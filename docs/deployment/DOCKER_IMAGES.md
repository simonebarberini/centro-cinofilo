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

Nota terminologica: il branch Git si chiama `dev`, ma il tag Docker corrispondente si chiama **`develop`** — scelta deliberata per evitare ambiguità sul registry tra "il branch `dev`" e "l'ambiente di sviluppo/`development`"; `develop` è inequivocabile anche per chi guarda solo la lista dei tag pubblicati, senza conoscere i nomi dei branch del repository.

Entrambe le immagini (`backend`, `frontend`) seguono esattamente la stessa convenzione di tag, pubblicati dalla stessa pipeline nello stesso momento (build sempre accoppiata backend+frontend):

| Tag | Immagine/i | Generato da | Quando viene pubblicato | Mutabilità | Uso |
|---|---|---|---|---|---|
| `sha-<shortsha>` | backend, frontend | Ogni commit pushato su `dev` | Workflow "Push su dev", ad ogni push | Immutabile (un tag = un commit preciso, mai sovrascritto) | Tracciabilità esatta: risalire dall'immagine al commit sorgente |
| `develop` | backend, frontend | Ultimo commit di `dev` | Workflow "Push su dev", ad ogni push (sovrascrive il precedente `develop`) | Mutabile (si sposta ad ogni push su `dev`) | Ultima build di integrazione continua, utilizzabile per un eventuale ambiente di staging futuro |
| `vX.Y.Z-rc.N` | backend, frontend | Tag Git di release candidate, creato su `dev` (vedi `GITFLOW.md`) | Workflow "Release" (o una sua variante), al push del tag `-rc.N` | Immutabile | Verifica pre-release in un ambiente di stabilizzazione, prima del rilascio definitivo |
| `vX.Y.Z` | backend, frontend | Tag Git di release stabile, creato su `main` | Workflow "Release", al push del tag `vX.Y.Z` | Immutabile — **mai ripubblicato con contenuto diverso** | **Il tag da usare in produzione**, sempre esplicito nei file Compose |
| `latest` | backend, frontend | Stesso build del tag `vX.Y.Z` più recente su `main` | Workflow "Release", subito dopo la pubblicazione di `vX.Y.Z` (ri-taggata sulla stessa immagine, non ricostruita) | Mutabile (si sposta ad ogni nuova release stabile) | Solo comodità per chi esplora il registry manualmente (`docker pull ...:latest` per provare l'ultima versione) — **da non usare nei file di Compose di produzione** |

**Regola importante:** i file Docker Compose di produzione devono sempre puntare a un tag esplicito e immutabile (`vX.Y.Z`), mai a `latest` o `develop`. Usare un tag mutabile in produzione rende il deploy non deterministico (un `docker compose pull` in due momenti diversi potrebbe scaricare immagini diverse senza che nessuno abbia cambiato la configurazione) e complica il rollback (non si sa più con certezza quale versione era in esecuzione prima).

**Nota su `vX.Y.Z-rc.N`:** con l'eliminazione del branch `release/*` (vedi `GITFLOW.md`), le immagini per una release candidate vengono comunque costruite e pubblicate — cambia solo il branch di origine (`dev` invece di un branch `release/*` dedicato), non il meccanismo di tagging.
