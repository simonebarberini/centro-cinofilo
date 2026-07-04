# Git Flow — Centro Cinofilo

## Analisi del branching attuale

Rilevato dal repository:

| Branch | Natura | Ruolo nel flusso proposto |
|---|---|---|
| `main` | remoto (`origin/main`) | Diventa il branch di **produzione**, sempre deployabile, protetto |
| `dev` | remoto, branch corrente di lavoro | Diventa il branch di **integrazione**, dove confluiscono le feature |
| `replit-agent` | locale, artefatto dell'ambiente Replit | **Non fa parte del Git Flow applicativo** — è un branch di servizio dell'agente, va ignorato in questa analisi e non deve essere confuso con un `feature/*` |
| `subrepl-*` | locale, artefatto dell'ambiente Replit | Idem, artefatto tecnico dell'ambiente, non del progetto |

Non esistono oggi branch `feature/*`, `hotfix/*` o `release/*`: tutto il lavoro finora è confluito direttamente su `dev` tramite commit diretti. Questo è accettabile nella fase attuale (sviluppatore singolo, nessun collaboratore), ma non scala quando si introduce CI/CD e revisione del codice.

## Flusso proposto

```
main        ●───────────────●───────────────●──────────▶   (produzione, sempre stabile)
             \  v1.2.0      / \  v1.3.0     /
              \            /   \           /
hotfix/*       ●──────────       (solo se serve un fix urgente su main)
                                  
dev         ●──●──●──●──●──●──●──●──●──●──●──●──▶   (integrazione continua)
             \    \    \    \
feature/*     ●────●    ●────●   (una feature alla volta, come da flusso CTO in replit.md)
```

### `main`

- Riflette sempre e solo ciò che è **in produzione o pronto per andarci**.
- Protetto: nessun push diretto (da abilitare come branch protection su GitHub quando si introduce CI/CD — vedi `docs/deployment/CICD.md`).
- Ogni merge su `main` corrisponde a un tag di release (vedi `VERSIONING.md`).

### `dev`

- Branch di integrazione. Ogni `feature/*` completata confluisce qui prima di finire in una release.
- Deve restare in stato "buildabile e testabile" in ogni momento (CI verde su ogni push, vedi `CICD.md`).

### `feature/*`

- Creato da `dev`, naming: `feature/<slug-breve-descrittivo>` (es. `feature/staff-invite`, `feature/rate-limit-headers`). Se in futuro si adotta un issue tracker, si può anteporre l'id: `feature/CENT-142-staff-invite`.
- Vive solo per la durata della singola feature — coerente con il principio già in `replit.md`: "una feature alla volta".
- Confluisce in `dev` tramite Pull Request, mai con push diretto.

### `hotfix/*`

- Creato da `main` (non da `dev`), per correggere un bug critico già in produzione senza aspettare il prossimo ciclo di rilascio da `dev`.
- Naming: `hotfix/<slug>` (es. `hotfix/jwt-validation-npe`).
- Merge in **due direzioni**, entrambe obbligatorie:
  1. `hotfix/*` → `main` (PR, poi tag `vX.Y.(Z+1)`, poi deploy immediato)
  2. `hotfix/*` → `dev` (back-merge, per non perdere il fix nelle prossime release regolari)
- Questo evita il classico bug "il hotfix ha risolto il problema in produzione ma è ricomparso alla release successiva perché non era su `dev`".

### `release/*` (facoltativo, da usare solo quando serve)

Non è un branch da usare per ogni release. Con un solo sviluppatore e un ritmo di rilascio non frenetico, la maggior parte delle release può andare direttamente `dev` → `main` (via PR) e taggata da lì.

Un branch `release/*` (es. `release/1.4.0`, creato da `dev`) diventa utile solo quando:
- si vogliono congelare le feature (feature freeze) mentre si stabilizza una release con più modifiche accumulate;
- servono più cicli di test/QA con tag `-rc.N` prima del rilascio definitivo, senza bloccare `dev` per le feature successive.

In quel caso: `release/*` riceve solo bugfix di stabilizzazione (mai nuove feature), si taggano le RC da lì, e a fine stabilizzazione si merge sia su `main` (con tag stabile) sia back su `dev`.

## Ciclo di vita completo di una feature

1. **Checkout** da `dev` aggiornato: `git checkout -b feature/<slug> dev`
2. **Sviluppo**, commit atomici e frequenti in stile Conventional Commits (vedi `VERSIONING.md`), seguendo il flusso CTO già in vigore nel progetto (spiega → proponi → approva → implementa → testa → committa) per ogni sotto-modifica significativa.
3. **Push** del branch e apertura **Pull Request** verso `dev`.
4. **CI automatica sulla PR** (vedi `CICD.md` — workflow "Pull Request"): build + unit test backend e frontend. Nessun merge possibile se la CI è rossa (branch protection).
5. **Code review**: oggi auto-revisione strutturata (checklist), in futuro (con collaboratori) revisione di terzi obbligatoria prima del merge.
6. **Merge su `dev`**: si raccomanda **squash merge** per i `feature/*` (storia di `dev` pulita, un commit logico per feature) mentre `dev` → `main` usa un merge commit (o un tag diretto) per marcare chiaramente i confini di ogni release.
7. **Cancellazione del branch** `feature/*` dopo il merge (locale e remoto).
8. Il codice resta su `dev` fino al prossimo ciclo di release (vedi `RELEASE_PROCESS.md`), quando confluisce in `main` e viene taggato.

## Branch protection consigliate (da configurare quando si introduce CI/CD, non ora)

- `main`: richiede PR, richiede CI verde, richiede almeno 1 approvazione (quando ci sono collaboratori), nessun force-push.
- `dev`: richiede PR (niente push diretto), richiede CI verde.
