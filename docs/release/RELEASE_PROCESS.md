# Release Process — Centro Cinofilo

Processo end-to-end, dalla feature alla produzione, con criteri di "fatto" per ogni step. Nessuno step qui descritto è ancora implementato (niente workflow, niente script) — questo documento descrive il design da approvare.

```
feature
  │  (branch da develop, sviluppo, vedi GITFLOW.md)
  ▼
pull request
  │  (verso develop; CI: build + unit test backend/frontend)
  ▼
merge su develop
  │  (squash merge; develop resta sempre in stato "verde")
  ▼
build
  │  (immagini Docker backend/frontend taggate :sha e :develop, push su registry — vedi DOCKER_IMAGES.md)
  ▼
test
  │  (suite di integration test completa, con Docker/Testcontainers realmente disponibile — CI runner, non l'ambiente Replit)
  ▼
release
  │  (decisione: quali feature accumulate su develop formano la prossima release; PR develop → main)
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

Apertura PR `feature/*` → `develop`. Trigga la CI di validazione (vedi `CICD.md`, workflow "Pull Request"): build e unit test di backend e frontend, senza bisogno di Docker/Testcontainers (grazie alla separazione Surefire/Failsafe già in essere, ADR-018).

**Definizione di "fatto":** CI verde, revisione completata, nessun conflitto con `develop`.

## 3. Merge su develop

Squash merge della PR su `develop`. Il branch `feature/*` viene eliminato.

**Definizione di "fatto":** `develop` compila, tutti gli unit test passano, la nuova feature è integrata con il resto del codice.

## 4. Build

Ad ogni push su `develop` (post-merge), la pipeline "Build Images" (vedi `CICD.md`) costruisce le immagini Docker di backend e frontend usando i Dockerfile esistenti (nessuna modifica), le tagga con lo short SHA del commit e con `:develop`, e le pubblica sul registry container (vedi `DOCKER_IMAGES.md`).

**Perché costruire le immagini già a questo punto e non solo al momento della release:** permette di individuare problemi di build/immagine (non solo di codice) il prima possibile, e rende disponibile un'immagine `:develop` deployabile su un eventuale ambiente di staging, senza aspettare una release formale.

**Definizione di "fatto":** immagini pubblicate sul registry, taggate correttamente.

## 5. Test (suite di integrazione completa)

Eseguita in CI (dove Docker è realmente disponibile, a differenza del sandbox di sviluppo Replit — vedi `.agents/memory/replit-testcontainers-sandbox.md`): `mvn verify` (Surefire + Failsafe, quindi anche i 101 Integration Test con Testcontainers), più eventuali test end-to-end del frontend.

**Definizione di "fatto":** suite di integrazione completamente verde. Se fallisce, il ciclo di release si ferma qui — non si procede con una release che non supera i test di integrazione.

## 6. Release

Decisione (umana, non automatica) su quali commit accumulati su `develop` compongono la prossima release. Si apre una PR `develop` → `main` (o si passa da un branch `release/*` se si è scelto di stabilizzare, vedi `GITFLOW.md`). **Nella stessa PR**, si aggiorna la versione in `backend/pom.xml` e `frontend/package.json` al valore che si sta per rilasciare (vedi `VERSIONING.md`, versionamento a livello di repository) — questo allineamento è un prerequisito: il workflow "Release" (step 7) verifica automaticamente questa coerenza e **fallisce se dimenticato**, prima di costruire qualunque immagine.

**Definizione di "fatto":** PR `develop` → `main` approvata, CI verde (rieseguita sul merge commit), versione nei manifest allineata alla release da taggare.

## 7. Tag

Al merge su `main`, si crea il tag `vX.Y.Z` (**senza suffisso** — le release candidate non sono gestite da questa pipeline, vedi ADR-026) secondo `VERSIONING.md`. Questo tag è l'evento che identifica in modo univoco "questa è la release X.Y.Z" ed è il trigger della pipeline "Release" implementata in `.github/workflows/release.yml` (vedi `CICD.md`): verifica che il tag discenda da `main`, che non esista già una release con lo stesso nome e che la versione nei manifest sia coerente; poi build delle immagini definitive taggate `vX.Y.Z` + `latest` e creazione della GitHub Release con note generate automaticamente. **Non esegue la suite di integration test** — la pipeline assume che il codice sia già stato validato dalle pipeline CI (Validate, Build Images, Publish Images); l'esecuzione automatica della suite completa in questo punto del processo è deferita a una futura milestone dedicata alla qualità della release.

## 8. Deploy

Esecuzione della pipeline "Manual Deploy" (trigger manuale, non automatico — scelta deliberata, vedi `CICD.md`): pull delle immagini `vX.Y.Z` sul server di produzione, aggiornamento dello stack Docker Compose, healthcheck, conferma.

**Perché il deploy in produzione è manuale e non automatico su ogni tag:** con un solo VPS di produzione e nessun ambiente di staging separato ancora esistente, un deploy automatico al tagging rimuoverebbe l'ultimo controllo umano prima di toccare i dati reali dei tenant. Il deploy automatico su un ambiente di staging (se introdotto in futuro) è invece ragionevole.

**Definizione di "fatto":** healthcheck del backend e del frontend verdi post-deploy, smoke test manuale (o automatizzato, in futuro) delle funzionalità critiche (login, prenotazione).

## 9. Rollback — procedura passo-passo di una release Docker

Se il deploy introduce un problema, il rollback applicativo (il caso comune, senza corruzione di schema) segue questi passi, nell'ordine:

1. **Individuare la versione target del rollback.** Verificare quale tag `vX.Y.Z` era effettivamente in esecuzione prima del deploy problematico (non assumerlo: controllare, es. via `docker compose images` o l'endpoint `/actuator/info` se espone la versione buildata) e confermare che sia l'ultima release nota-funzionante.
2. **Congelare nuovi deploy.** Evitare che parta un altro deploy (manuale o automatico) mentre il rollback è in corso, per non sovrapporre due operazioni sullo stesso stack.
3. **Aggiornare il riferimento di versione.** Cambiare `IMAGE_TAG` (o equivalente) nel file `.env` del server dal tag problematico al tag target individuato al punto 1.
4. **Recuperare le immagini precedenti.**
   ```
   docker compose pull
   ```
   Non è un rebuild: l'immagine del tag precedente è già pubblicata sul registry (GHCR) da quando è stata rilasciata — il pull la recupera (o la trova già in cache locale sul VPS, se non è stata rimossa).
5. **Ricreare i container con l'immagine precedente.**
   ```
   docker compose up -d --no-build
   ```
   `--no-build` è deliberato: si vuole avere la certezza che venga usata l'immagine già pubblicata per quel tag, non una ricostruzione locale.
6. **Verificare che il rollback sia effettivo.** `docker compose ps` per lo stato dei container, `docker inspect` (o l'immagine mostrata da `docker compose images`) per confermare che l'image ID/tag in esecuzione corrisponda davvero alla versione target — un rollback "creduto fatto" ma non verificato è un rischio, non una soluzione.
7. **Healthcheck e smoke test.** Stessi controlli della checklist post-release (`docs/operations/CHECKLISTS.md`): Actuator Health, raggiungibilità del frontend, un flusso critico (login, lettura di una prenotazione).
8. **Valutare l'impatto sullo schema DB.** Se la release da cui si torna indietro ha eseguito una migration Flyway: il rollback del solo codice è sicuro **soltanto se la migration era additiva/retrocompatibile** (principio "expand/contract" — si aggiunge una colonna/tabella in una release, si rimuove il vecchio riferimento solo due release dopo, mai nello stesso deploy). Se la migration ha modificato o rimosso qualcosa di cui il codice precedente ha ancora bisogno, il rollback del solo codice non basta: valutare un fix-forward (nuova release correttiva rapida) invece di un rollback, oppure procedere a **disaster recovery** da backup se i dati sono già stati corrotti (vedi `docs/operations/CHECKLISTS.md`).
9. **Documentare l'incidente.** Cosa è andato storto, quale versione era rotta, quale versione è stata ripristinata, e se ha rivelato un problema strutturale da correggere (es. una migration non abbastanza additiva, un test che non ha intercettato il problema) — anche solo una nota, per non ripetere lo stesso errore.

**Nota importante:** il rollback del *codice* (passi 1–7) è quasi sempre sicuro e veloce — ripuntare a un'immagine precedente già pubblicata. Il rollback dello *schema DB* (passo 8) è il punto delicato del processo: per questo la disciplina delle migration additive è un prerequisito per un rollback affidabile, non un dettaglio opzionale.
