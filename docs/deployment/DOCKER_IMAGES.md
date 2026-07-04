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

## Versioning delle immagini

| Tag | Quando viene creato | Uso |
|---|---|---|
| `sha-<shortsha>` | Ad ogni push su `dev` (workflow "Push su dev") | Tracciabilità: ogni build è riconducibile esattamente a un commit |
| `dev` | Ad ogni push su `dev`, sovrascrive il precedente | Ultima build di integrazione, utilizzabile per un eventuale ambiente di staging |
| `vX.Y.Z` | Alla creazione del tag Git di release (workflow "Release") | Immagine immutabile della release — **questo è il tag da usare in produzione** |
| `vX.Y.Z-rc.N` | Se si usa un ciclo di release candidate (vedi `GITFLOW.md`) | Test pre-release su un ambiente di stabilizzazione |
| `latest` | Sempre allineato all'ultima release stabile su `main` | Solo come riferimento/comodo per chi esplora il registry — **da non usare nei file di Compose di produzione** |

**Regola importante:** i file Docker Compose di produzione devono sempre puntare a un tag esplicito (`vX.Y.Z`), mai a `latest` o `dev`. Usare `latest` in produzione rende il deploy non deterministico (un `docker compose pull` in due momenti diversi potrebbe scaricare immagini diverse senza che nessuno abbia cambiato la configurazione) e complica il rollback (non si sa più con certezza quale versione era in esecuzione prima).
