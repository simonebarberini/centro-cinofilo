# Git Flow — Centro Cinofilo

## Analisi del branching attuale

Rilevato dal repository:

| Branch | Natura | Ruolo nel flusso proposto |
|---|---|---|
| `main` | remoto (`origin/main`) | Branch di **produzione**, sempre deployabile, protetto |
| `develop` | remoto (rinominato definitivamente da `dev`, vedi ADR-024) | Branch di **integrazione**, dove confluiscono le feature |
| `replit-agent` | locale, artefatto dell'ambiente Replit | **Non fa parte del Git Flow applicativo** — è un branch di servizio dell'agente, va ignorato in questa analisi e non deve essere confuso con un `feature/*` |
| `subrepl-*` | locale, artefatto dell'ambiente Replit | Idem, artefatto tecnico dell'ambiente, non del progetto |

Non esistono oggi branch `feature/*` o `hotfix/*`: tutto il lavoro finora è confluito direttamente su `develop` (in precedenza `dev`) tramite commit diretti. Questo è accettabile nella fase attuale (sviluppatore singolo, nessun collaboratore), ma non scala quando si introduce CI/CD e revisione del codice.

> **Nota storica:** fino a questa milestone il branch di integrazione si chiamava `dev`. È stato rinominato definitivamente in `develop` (vedi ADR-024 in `docs/ai/DECISIONS.md`). I riferimenti a `dev` nelle ADR precedenti a ADR-024 riflettono il nome in vigore al momento in cui furono scritte e non vengono modificati retroattivamente.

## `release/*`: valutazione esplicita e decisione di non adottarlo

**Domanda:** un branch `release/*` porta benefici concreti per un progetto mantenuto da un solo sviluppatore?

**Analisi.** Il branch `release/*` esiste, nei modelli Git Flow classici, per risolvere un problema preciso: permettere a un team di **continuare a sviluppare nuove feature su `develop`** mentre **un'altra persona (o sottogruppo) stabilizza** una release già "congelata" in parallelo, senza che le due attività si intralcino. È uno strumento di **coordinamento tra persone che lavorano in parallelo su obiettivi diversi**.

Con un solo sviluppatore questo problema non esiste:
- Non c'è nessun secondo flusso di lavoro da isolare — chi stabilizza la release è la stessa persona che svilupperebbe la prossima feature, quindi la "protezione" del branch `release/*` non previene alcun conflitto reale, semplicemente perché non c'è un altro attore concorrente.
- Il "congelamento" delle feature durante la stabilizzazione può essere ottenuto semplicemente **decidendo di non iniziare una nuova feature** finché la release corrente non è fuori — una decisione organizzativa, non qualcosa che richiede un branch dedicato per essere applicata.
- Le release candidate (`vX.Y.Z-rc.N`, vedi `VERSIONING.md`) possono essere taggate direttamente su `develop`: se un problema emerge durante la stabilizzazione, si corregge con un commit su `develop` e si ritagga una nuova RC, senza bisogno di un branch separato in cui applicare lo stesso fix due volte.
- Un branch in più significa un passaggio in più da ricordare, documentare e (in futuro) automatizzare nella CI — costo di processo reale, a fronte di un beneficio che in questo contesto è nullo.

**Decisione: `release/*` viene eliminato dal modello.** Il flusso di rilascio passa direttamente `develop` → `main` (via PR, vedi `RELEASE_PROCESS.md`), con le RC tagate su `develop` quando serve un ciclo di stabilizzazione più lungo.

**Quando riconsiderarlo:** se in futuro si aggiungono collaboratori che lavorano in parallelo su feature diverse mentre un rilascio è in stabilizzazione, il problema che `release/*` risolve torna reale — a quel punto la sua reintroduzione va rivalutata esplicitamente (non va aggiunto preventivamente ora "perché potrebbe servire").

## Flusso proposto

```
main        ●───────────────●───────────────●──────────▶   (produzione, sempre stabile)
             \  v0.3.0      / \  v0.4.0     /
              \            /   \           /
hotfix/*       ●──────────       (solo se serve un fix urgente su main)
                                  
develop     ●──●──●──●──●──●──●──●──●──●──●──●──▶   (integrazione continua; le RC si taggano qui)
             \    \    \    \
feature/*     ●────●    ●────●   (una feature alla volta, come da flusso CTO in replit.md)
```

### `main`

- Riflette sempre e solo ciò che è **in produzione o pronto per andarci**.
- Protetto: nessun push diretto (da abilitare come branch protection su GitHub quando si introduce CI/CD — vedi `docs/deployment/CICD.md`).
- Ogni merge su `main` corrisponde a un tag di release (vedi `VERSIONING.md`).

### `develop`

- Branch di integrazione. Ogni `feature/*` completata confluisce qui prima di finire in una release.
- Deve restare in stato "buildabile e testabile" in ogni momento (CI verde su ogni push, vedi `CICD.md`).

### `feature/*`

- Creato da `develop`, naming: `feature/<slug-breve-descrittivo>` (es. `feature/staff-invite`, `feature/rate-limit-headers`). Se in futuro si adotta un issue tracker, si può anteporre l'id: `feature/CENT-142-staff-invite`.
- Vive solo per la durata della singola feature — coerente con il principio già in `replit.md`: "una feature alla volta".
- Confluisce in `develop` tramite Pull Request, mai con push diretto.

### `hotfix/*`

- Creato da `main` (non da `develop`), per correggere un bug critico già in produzione senza aspettare il prossimo ciclo di rilascio da `develop`.
- Naming: `hotfix/<slug>` (es. `hotfix/jwt-validation-npe`).
- Merge in **due direzioni**, entrambe obbligatorie:
  1. `hotfix/*` → `main` (PR, poi tag `vX.Y.(Z+1)`, poi deploy immediato)
  2. `hotfix/*` → `develop` (back-merge, per non perdere il fix nelle prossime release regolari)
- Questo evita il classico bug "il hotfix ha risolto il problema in produzione ma è ricomparso alla release successiva perché non era su `develop`".

### Stabilizzazione di una release (senza branch dedicato)

Quando una release accumula più modifiche e serve un periodo di stabilizzazione prima del merge su `main`, questo avviene **direttamente su `develop`**, senza aprire `release/*` (vedi motivazione sopra):
1. Si smette di aprire nuove `feature/*` verso `develop` finché la release non è fuori (decisione organizzativa, non un vincolo tecnico del branch).
2. Si tagga una RC direttamente sul commit di `develop` (`vX.Y.Z-rc.1`).
3. Eventuali bugfix di stabilizzazione vanno come commit diretti su `develop`, poi si ritagga una nuova RC (`-rc.2`, `-rc.3`, ...).
4. Quando una RC è verificata, si apre la PR `develop` → `main` e si tagga la release stabile (`vX.Y.Z`, stesso commit dell'ultima RC).

## Ciclo di vita completo di una feature

1. **Checkout** da `develop` aggiornato: `git checkout -b feature/<slug> develop`
2. **Sviluppo**, commit atomici e frequenti in stile Conventional Commits (vedi `VERSIONING.md`), seguendo il flusso CTO già in vigore nel progetto (spiega → proponi → approva → implementa → testa → committa) per ogni sotto-modifica significativa.
3. **Push** del branch e apertura **Pull Request** verso `develop`.
4. **CI automatica sulla PR** (vedi `CICD.md` — workflow "Pull Request"): build + unit test backend e frontend. Nessun merge possibile se la CI è rossa (branch protection).
5. **Code review**: oggi auto-revisione strutturata (checklist), in futuro (con collaboratori) revisione di terzi obbligatoria prima del merge.
6. **Merge su `develop`**: si raccomanda **squash merge** per i `feature/*` (storia di `develop` pulita, un commit logico per feature) mentre `develop` → `main` usa un merge commit (o un tag diretto) per marcare chiaramente i confini di ogni release.
7. **Cancellazione del branch** `feature/*` dopo il merge (locale e remoto).
8. Il codice resta su `develop` fino al prossimo ciclo di release (vedi `RELEASE_PROCESS.md`), quando confluisce in `main` e viene taggato.

## Branch protection consigliate (da configurare quando si introduce CI/CD, non ora)

- `main`: richiede PR, richiede CI verde, richiede almeno 1 approvazione (quando ci sono collaboratori), nessun force-push.
- `develop`: richiede PR (niente push diretto), richiede CI verde.
