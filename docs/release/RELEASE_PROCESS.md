# Release Process — Centro Cinofilo

Processo end-to-end, dalla feature alla produzione, con criteri di "fatto" per ogni step. Nessuno step qui descritto è ancora implementato (niente workflow, niente script) — questo documento descrive il design da approvare.

```
feature
  │  (branch da dev, sviluppo, vedi GITFLOW.md)
  ▼
pull request
  │  (verso dev; CI: build + unit test backend/frontend)
  ▼
merge su dev
  │  (squash merge; dev resta sempre in stato "verde")
  ▼
build
  │  (immagini Docker backend/frontend taggate :sha e :dev, push su registry — vedi DOCKER_IMAGES.md)
  ▼
test
  │  (suite di integration test completa, con Docker/Testcontainers realmente disponibile — CI runner, non l'ambiente Replit)
  ▼
release
  │  (decisione: quali feature accumulate su dev formano la prossima release; PR dev → main)
  ▼
tag
  │  (vX.Y.Z su main, secondo VERSIONING.md)
  ▼
deploy
  │  (pull immagini vX.Y.Z sul server, docker compose up -d, healthcheck — vedi SERVER.md)
  ▼
rollback (se necessario)
     (ripristino immagine/tag precedente, vedi sotto)
```

## 1. Feature

Vedi `GITFLOW.md`. Ogni feature è isolata in `feature/*`, sviluppata seguendo il flusso CTO già stabilito nel progetto.

**Definizione di "fatto" per questo step:** codice compilato, unit test relativi alla feature scritti e verdi, nessuna modifica a `main`.

## 2. Pull Request

Apertura PR `feature/*` → `dev`. Trigga la CI di validazione (vedi `CICD.md`, workflow "Pull Request"): build e unit test di backend e frontend, senza bisogno di Docker/Testcontainers (grazie alla separazione Surefire/Failsafe già in essere, ADR-018).

**Definizione di "fatto":** CI verde, revisione completata, nessun conflitto con `dev`.

## 3. Merge su dev

Squash merge della PR su `dev`. Il branch `feature/*` viene eliminato.

**Definizione di "fatto":** `dev` compila, tutti gli unit test passano, la nuova feature è integrata con il resto del codice.

## 4. Build

Ad ogni push su `dev` (post-merge), la pipeline "Push su dev" (vedi `CICD.md`) costruisce le immagini Docker di backend e frontend usando i Dockerfile esistenti (nessuna modifica), le tagga con lo short SHA del commit e con `:dev`, e le pubblica sul registry container (vedi `DOCKER_IMAGES.md`).

**Perché costruire le immagini già a questo punto e non solo al momento della release:** permette di individuare problemi di build/immagine (non solo di codice) il prima possibile, e rende disponibile un'immagine `:dev` deployabile su un eventuale ambiente di staging, senza aspettare una release formale.

**Definizione di "fatto":** immagini pubblicate sul registry, taggate correttamente.

## 5. Test (suite di integrazione completa)

Eseguita in CI (dove Docker è realmente disponibile, a differenza del sandbox di sviluppo Replit — vedi `.agents/memory/replit-testcontainers-sandbox.md`): `mvn verify` (Surefire + Failsafe, quindi anche i 101 Integration Test con Testcontainers), più eventuali test end-to-end del frontend.

**Definizione di "fatto":** suite di integrazione completamente verde. Se fallisce, il ciclo di release si ferma qui — non si procede con una release che non supera i test di integrazione.

## 6. Release

Decisione (umana, non automatica) su quali commit accumulati su `dev` compongono la prossima release. Si apre una PR `dev` → `main` (o si passa da un branch `release/*` se si è scelto di stabilizzare, vedi `GITFLOW.md`).

**Definizione di "fatto":** PR `dev` → `main` approvata, CI verde (rieseguita sul merge commit).

## 7. Tag

Al merge su `main`, si crea il tag `vX.Y.Z` secondo `VERSIONING.md`. Questo tag è l'evento che identifica in modo univoco "questa è la release X.Y.Z" ed è il trigger della pipeline "Release" (vedi `CICD.md`): build delle immagini definitive taggate `vX.Y.Z` + `latest`, generazione release notes.

## 8. Deploy

Esecuzione della pipeline "Manual Deploy" (trigger manuale, non automatico — scelta deliberata, vedi `CICD.md`): pull delle immagini `vX.Y.Z` sul server di produzione, aggiornamento dello stack Docker Compose, healthcheck, conferma.

**Perché il deploy in produzione è manuale e non automatico su ogni tag:** con un solo VPS di produzione e nessun ambiente di staging separato ancora esistente, un deploy automatico al tagging rimuoverebbe l'ultimo controllo umano prima di toccare i dati reali dei tenant. Il deploy automatico su un ambiente di staging (se introdotto in futuro) è invece ragionevole.

**Definizione di "fatto":** healthcheck del backend e del frontend verdi post-deploy, smoke test manuale (o automatizzato, in futuro) delle funzionalità critiche (login, prenotazione).

## 9. Rollback

Se il deploy introduce un problema:

1. **Rollback applicativo (il caso comune):** ripuntare lo stack Docker Compose all'immagine del tag precedente (`vX.Y.(Z-1)` o l'ultima release stabile nota) e riavviare. Non richiede rebuild: l'immagine precedente è già sul registry.
2. **Rollback di schema (il caso raro e delicato):** possibile **solo se le migration Flyway sono state scritte in modo additivo/retrocompatibile** (principio "expand/contract": si aggiunge una colonna/tabella in una release, si rimuove il vecchio riferimento solo due release dopo, mai nello stesso deploy). Questo è un vincolo di disegno per le migration future, non un tool: va rispettato a ogni migration che tocca dati esistenti.
3. Se il rollback applicativo non basta (dati corrotti, migration distruttiva già eseguita), si passa a **disaster recovery** da backup — vedi `docs/operations/CHECKLISTS.md`.

**Nota importante:** il rollback del *codice* è quasi sempre sicuro e veloce (repuntare a un'immagine precedente). Il rollback dello *schema DB* è il punto delicato del processo — per questo la disciplina delle migration additive è un prerequisito per un rollback affidabile, non un dettaglio opzionale.
