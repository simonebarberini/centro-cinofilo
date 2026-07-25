# Centro Cinofilo

[![Validate](https://github.com/simonebarberini/centro-cinofilo/actions/workflows/validate.yml/badge.svg)](https://github.com/simonebarberini/centro-cinofilo/actions/workflows/validate.yml)

Monorepo per la gestione e amministrazione di un centro di addestramento cinofilo.

## Struttura del Progetto

```
centro-cinofilo/
├── backend/          # API server e logica di business
├── frontend/         # Applicazione web frontend
├── infra/
│   └── docker/       # Configurazioni Docker e docker-compose
└── docs/             # Documentazione del progetto
```

## Documentazione

| Documento | Contenuto |
|-----------|-----------|
| [docs/docker-guide.md](docs/docker-guide.md) | Spiegazione completa di Docker, Dockerfile, Docker Compose e come sono usati nel progetto |
| [docs/release/VERSIONING.md](docs/release/VERSIONING.md) | Strategia SemVer, tag, milestone, prerelease |
| [docs/release/GITFLOW.md](docs/release/GITFLOW.md) | Branching model: main/dev/feature/hotfix/release |
| [docs/release/RELEASE_PROCESS.md](docs/release/RELEASE_PROCESS.md) | Processo completo di rilascio, dalla feature al deploy |
| [docs/deployment/CICD.md](docs/deployment/CICD.md) | Design delle pipeline CI/CD (non ancora implementate) |
| [docs/deployment/DOCKER_IMAGES.md](docs/deployment/DOCKER_IMAGES.md) | Naming, versioning e registry delle immagini Docker |
| [docs/deployment/SERVER.md](docs/deployment/SERVER.md) | Design del server di produzione (VPS, Caddy, backup, monitoring) |
| [docs/operations/CHECKLISTS.md](docs/operations/CHECKLISTS.md) | Checklist pre/post-release, rollback, disaster recovery, segreti |
| [docs/ai/DECISIONS.md](docs/ai/DECISIONS.md) | Architectural Decision Records (ADR-020: strategia di Release & Deployment) |

## Prerequisiti

- Java 21+
- Maven 3.9+ (su macOS: incluso in IntelliJ IDEA CE sotto `Contents/plugins/maven/lib/maven3/bin/mvn`)
- Node.js 22+ e npm
- Docker e Docker Compose v2+
- Git

## Postman Smoke Tests

Questa cartella contiene una collection Postman e un environment per effettuare smoke test manuale delle API Auth e Customers.

### Setup di Postman

1. **Importare la collection e l'environment**:
   - Aprire Postman
   - Cliccare su "Import" (in alto a sinistra)
   - Selezionare `postman/CentroCinofilo.postman_collection.json`
   - Ripetere per `postman/local-dev.postman_environment.json`

2. **Selezionare l'environment**:
   - In alto a destra, selezionare "local-dev" dal dropdown degli environment

3. **Verificare le variabili**:
   - Cliccare su "local-dev" → "Edit"
   - Verificare che le variabili siano correttamente configurate:
     - `baseUrl`: `http://localhost:8080`
     - `tenantSlug`: `demo-centro`
     - `username`: `owner`
     - `password`: `owner123!`
     - **Variabili runtime** (devono restare vuote):
       - `accessToken`: vuoto (riempito automaticamente da Login)
       - `customerId`: vuoto (riempito automaticamente da Create Customer)
       - `dogId`: vuoto (riempito automaticamente da Create Dog)
       - `bookingId`: vuoto (riempito automaticamente da Create Booking)

### Ordine di Esecuzione

Eseguire le richieste **in questo ordine** per un corretto smoke test:

1. **Login** (folder Auth)
   - Autentica l'utente con il tenant specificato
   - Salva automaticamente il token JWT in `accessToken`

2. **Create Customer** (folder Customers)
   - Crea un nuovo customer
   - Salva automaticamente l'ID del customer in `customerId`

3. **Create Dog** (folder Dogs)
   - Crea un nuovo cane per il customer
   - Salva automaticamente l'ID del cane in `dogId`

4. **Create Booking** (folder Bookings)
   - Crea una prenotazione per il cane
   - Salva automaticamente l'ID della prenotazione in `bookingId`

5. **List Bookings** (folder Bookings)
   - Elenca tutte le prenotazioni per il tenant

6. **Get Booking** (folder Bookings)
   - Recupera il dettaglio della prenotazione creata

7. **Update Booking** (folder Bookings)
   - Aggiorna le date della prenotazione

8. **Cancel Booking** (folder Bookings)
   - Annulla la prenotazione

9. **List Customers** (folder Customers)
   - Elenca tutti i customer per il tenant corrente

10. **Get Customer** (folder Customers)
    - Recupera il dettaglio del customer

11. **Update Customer** (folder Customers)
    - Aggiorna il customer con nuovi dati

12. **Get Dog** (folder Dogs)
    - Recupera il dettaglio del cane

13. **Update Dog** (folder Dogs)
    - Aggiorna il cane con nuovi dati

14. **Delete Dog** (folder Dogs)
    - Elimina il cane

15. **Delete Customer** (folder Customers)
    - Elimina il customer

### Versioning & Import

**Importante**: La cartella `/postman` nel repository è la **fonte di verità**. Per mantenere la sincronizzazione con il team:

**Workflow locale:**
1. `git pull` per ottenere i file più recenti dal repo
2. In Postman, scegli **Import** → seleziona file → **Replace** (non merge)
3. I tuoi token e ID runtime rimangono in memoria Postman durante la sessione
4. Al termine della sessione, Postman non salva i token nei file locali

**Regole Git:**
- **Non committare mai**: `accessToken`, `customerId`, `dogId`, `bookingId` con valori reali
- Questi campi devono **restare vuoti** (`""`) nel file di repo
- I token/ID sono solo runtime, generati dai test script al login/create
- Le variabili di ambiente (`baseUrl`, `tenantSlug`, `username`, `password`) possono restare nel repo

**Esportazione corretta:**
Se devi esportare l'environment dopo una sessione di test:
1. In Postman, clicca sull'environment → Download
2. **Prima di committare**, verifica che i campi sensibili siano vuoti:
   - `"value": ""` per accessToken, customerId, dogId, bookingId
   - Solo `initialValue` e `value` devono essere vuoti

### Note Importanti

- **Token e ID automatici**: I test script di Postman estraggono automaticamente `accessToken` dalla response di Login e `customerId` dalla response di Create Customer. Non è necessario copiarli manualmente.
- **Environment setup**: L'environment `local-dev` contiene i dati di default per lo sviluppo locale. Se si usa un'altra configurazione (prod, staging), creare un nuovo environment.
- **Backend in esecuzione**: Assicurarsi che il backend Spring Boot sia in esecuzione su `http://localhost:8080` prima di eseguire i test.
- **Team sync**: Usa sempre "Import Replace" e non "Import Merge" per evitare conflitti nell'environment.

## Avvio in locale (sviluppo)

In locale si avviano **tre processi** separati: il database e Mailhog via Docker, il backend con Maven, il frontend con npm. Non è necessario buildare container per BE o FE.

---

### 1. Configurare le variabili d'ambiente

Il file `.env.dev` (nella root del progetto, già tracciato in git) contiene già valori funzionanti per lo sviluppo — non è necessario modificarlo salvo esigenze particolari.

---

### 2. Avviare il database PostgreSQL

```bash
docker compose --env-file .env.dev -f infra/docker/dev/docker-compose.yml up -d
```

Verifica:
```bash
docker compose -f infra/docker/dev/docker-compose.yml ps
# cinofilo-dev-db   Up (healthy)
```

Connessione diretta (opzionale):
```bash
psql -h localhost -U cinofilo -d cinofilo
```

Stop:
```bash
docker compose -f infra/docker/dev/docker-compose.yml down

# Reset completo (cancella i dati)
docker compose -f infra/docker/dev/docker-compose.yml down -v
```

---

### 3. Avviare Mailhog (per la verifica email e il reset password)

Mailhog intercetta le email inviate dal backend senza consegnarle realmente. È necessario per testare la registrazione e il recupero password.

```bash
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

| Servizio | URL |
|----------|-----|
| UI email (browser) | http://localhost:8025 |
| SMTP (backend) | localhost:1025 |

Stop:
```bash
docker stop mailhog && docker rm mailhog
```

> Se Mailhog non è in esecuzione il backend non va in errore — le email vengono solo loggate in console. Ma non potrai testare i link di verifica.

---

### 4. Avviare il backend Spring Boot

In un terminale separato:

```bash
cd backend

# Se mvn è nel PATH
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Su macOS con IntelliJ IDEA CE (mvn non nel PATH di default)
"/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn" \
  spring-boot:run -Dspring-boot.run.profiles=dev
```

> Aggiungi l'alias una volta sola per non riscriverlo:
> ```bash
> echo 'alias mvn="/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn"' >> ~/.zshrc && source ~/.zshrc
> ```

Al primo avvio Maven scarica le dipendenze (~1-2 minuti). Flyway esegue automaticamente le migration e il `DevDataSeeder` inserisce un utente demo.

Credenziali demo (già verificate, pronte per il login):
- **Slug centro**: `demo-centro`
- **Username**: `owner`
- **Password**: `owner123!`

Verifica:
```bash
curl http://localhost:8080/api/actuator/health
# {"status":"UP"}
```

---

### 5. Avviare il frontend Angular

In un altro terminale separato:

```bash
cd frontend
npm install        # solo la prima volta
npm start
```

Il proxy di sviluppo (`proxy.conf.json`) instrada automaticamente `/api/*` verso `http://localhost:8080`.

---

### Riepilogo URL in sviluppo

| Servizio | URL |
|----------|-----|
| Applicazione web | http://localhost:4200 |
| API backend | http://localhost:8080/api |
| Health check | http://localhost:8080/api/actuator/health |
| Mailhog (email) | http://localhost:8025 |
| PostgreSQL | localhost:5432 |

---

### Test del flusso email in locale

1. Aprire http://localhost:8025 (Mailhog)
2. Registrare un nuovo account su http://localhost:4200/register
3. L'email di verifica appare in Mailhog — copiare il link e aprirlo nel browser
4. Dopo la verifica, fare login con le credenziali scelte
5. Per testare il reset password: login → "Password dimenticata?" → inserire email e slug → controllare Mailhog

---

### Comandi di pulizia

```bash
# Fermare DB (preserva i dati)
docker compose -f infra/docker/dev/docker-compose.yml down

# Fermare DB e cancellare i dati
docker compose -f infra/docker/dev/docker-compose.yml down -v

# Fermare Mailhog
docker stop mailhog && docker rm mailhog

# Pulire build Maven
cd backend && mvn clean
```

---

## Produzione

### Accesso remoto al database

In produzione Postgres pubblica la porta solo su `127.0.0.1` del server (vedi `infra/docker/prod/docker-compose.yml`), quindi non è mai raggiungibile direttamente da internet. Per connettersi da remoto (es. con DBeaver, pgAdmin o `psql`) si apre un tunnel SSH verso il server:

```bash
ssh -L 5432:localhost:5432 <utente>@<host-produzione>
```

Poi ci si connette al DB come se fosse locale, usando le credenziali in `.env` (server):

```
Host:     localhost
Porta:    5432
Database: ${POSTGRES_DB}
Utente:   ${POSTGRES_USER}
Password: ${POSTGRES_PASSWORD}
```

La porta pubblicata sul server è configurabile con la variabile opzionale `POSTGRES_PORT` in `.env` (default `5432`), utile se sulla propria macchina è già in uso una porta 5432 locale.

## Convenzioni di Sviluppo

### Conventional Commits

Questo progetto segue lo standard [Conventional Commits](https://www.conventionalcommits.org/).

Formato del commit:
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types principali:**
- `feat`: Nuova feature
- `fix`: Bug fix
- `docs`: Documentazione
- `style`: Cambiamenti di formattazione (senza impatto funzionale)
- `refactor`: Refactoring del codice
- `perf`: Miglioramenti di performance
- `test`: Aggiunta o modifica di test
- `chore`: Aggiornamenti dipendenze, configurazioni, build
- `ci`: Cambiamenti CI/CD

**Scopes comuni:**
- `backend`: Modifiche al backend
- `frontend`: Modifiche al frontend
- `infra`: Modifiche all'infrastruttura
- `docs`: Documentazione del progetto

**Esempi:**
```
feat(backend): add user authentication endpoint
fix(frontend): resolve navbar styling issue
docs: add API documentation
chore: update dependencies
```

## Branch Strategy

Il progetto utilizza il seguente modello di branching:

### Main Branches

- **`main`**: Branch di produzione. Contiene codice stabile e testato. Ogni commit rappresenta una release.
  - Merge permesso solo tramite Pull Request
  - Richiede almeno una review

- **`dev`**: Branch di staging/sviluppo. Integrazione di feature completate.
  - Branch di lavoro principale per integrare feature
  - Merge da feature branches tramite Pull Request

### Feature Branches

- **`feature/<feature-name>`**: Branch per lo sviluppo di nuove feature
  - Create da: `dev`
  - Merge verso: `dev`
  - Naming: `feature/user-authentication`, `feature/add-dog-profile`

### Workflow di Sviluppo

```bash
# 1. Creare una feature branch da dev
git checkout dev
git pull origin dev
git checkout -b feature/mia-feature

# 2. Sviluppare e committare (seguendo Conventional Commits)
git add .
git commit -m "feat(backend): add new endpoint"

# 3. Push della feature branch
git push origin feature/mia-feature

# 4. Creare Pull Request su GitHub verso dev

# 5. Dopo merge, eliminare la feature branch
git branch -d feature/mia-feature
git push origin --delete feature/mia-feature
```

### Merge verso Main

Quando il codice in `dev` è stabile e pronto per il deploy:

```bash
# 1. Creare Pull Request da dev verso main
# 2. Completare la review
# 3. Merge con commit message: chore(release): vX.Y.Z

# Il codice in main deve sempre essere pronto per il deploy in produzione
```

## Project Context (for AI/Copilot)

Questa sezione fornisce il contesto completo del progetto per assistenti AI come GitHub Copilot. Consultare questa sezione quando si sviluppa nuove feature o si modificano il codice.

### Stack Tecnologico

| Componente | Tecnologia | Versione |
|-----------|-----------|---------|
| **Backend API** | Spring Boot | 3.2.0 |
| **Linguaggio Backend** | Java | 21 |
| **Build Backend** | Maven | Latest |
| **Database** | PostgreSQL | 16 |
| **Frontend** | Angular | 17 |
| **Linguaggio Frontend** | TypeScript | 5.2 |
| **Package Manager Frontend** | npm | Latest |
| **Container** | Docker & Docker Compose | Latest |
| **Version Control** | Git | Latest |

### Struttura delle Cartelle Dettagliata

```
centro-cinofilo/
├── backend/                          # Spring Boot Java Application
│   ├── pom.xml                       # Maven configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/it/cinofilo/     # Package principale
│   │   │   │   ├── CinofiloApplication.java  # Spring Boot main class
│   │   │   │   └── controller/       # REST Controllers
│   │   │   │       └── HealthController.java
│   │   │   └── resources/
│   │   │       ├── application.yml   # Spring configuration
│   │   │       └── db/migration/     # Flyway migrations
│   │   │           └── V1__init.sql
│   │   └── test/                     # Unit tests
│   └── target/                       # Build output (Maven)
│
├── frontend/                         # Angular Application
│   ├── package.json                  # npm configuration
│   ├── angular.json                  # Angular CLI config
│   ├── tsconfig.json                 # TypeScript configuration
│   ├── .eslintrc.js                  # ESLint rules
│   ├── .prettierrc.json              # Prettier formatting
│   ├── src/
│   │   ├── index.html                # HTML entry point
│   │   ├── main.ts                   # TypeScript entry point
│   │   ├── styles.css                # Global styles
│   │   └── app/
│   │       ├── app.module.ts         # Main module
│   │       ├── app-routing.module.ts # Routing configuration
│   │       ├── app.component.*       # Root component
│   │       └── pages/
│   │           └── home/             # Home page component
│   └── dist/                         # Build output (Angular)
│
├── infra/
│   └── docker/
│       └── docker-compose.yml        # Docker services (PostgreSQL)
│
├── docs/                             # Project documentation
│
├── .gitattributes                    # Git line ending normalization
├── .gitignore                        # Git ignore rules
├── .editorconfig                     # Editor configuration
├── .env.example                      # Environment variables example
└── README.md                         # This file
```

### Come Avviare l'Ambiente Locale

Vedere la sezione **[Avvio in locale (sviluppo)](#avvio-in-locale-sviluppo)** per le istruzioni complete e aggiornate.

### Regole Base di Sviluppo

#### 1. Naming Conventions

**Backend (Java):**
- Packages: `it.cinofilo.<dominio>` (es: `it.cinofilo.controller`, `it.cinofilo.service`)
- Classes: PascalCase (es: `UserController`, `AuthenticationService`)
- Methods: camelCase (es: `getUserById()`, `createNewUser()`)
- Constants: UPPER_SNAKE_CASE

**Frontend (TypeScript/Angular):**
- Components: PascalCase + `.component.ts` (es: `HomeComponent`, `UserListComponent`)
- Services: PascalCase + `.service.ts` (es: `UserService`, `AuthService`)
- Methods: camelCase
- Constants: UPPER_SNAKE_CASE

#### 2. Multi-Tenancy (Future Implementation)

Il progetto è progettato per supportare multi-tenancy nel futuro. Le seguenti linee guida devono essere seguite:

- **Ogni entità che sarà tenant-aware deve avere un campo `tenant_id`**: (UUID o Long)
- **Nel backend**:
  - Il `tenant_id` viene estratto automaticamente dal token JWT dell'utente autenticato
  - Tutte le query JPA devono essere filtrate automaticamente per `tenant_id` tramite JPA filters o aspect
  - Non fidarsi mai del `tenant_id` passato dal client - usare sempre quello del JWT
- **Nel frontend**:
  - NON inviare `tenant_id` nelle richieste - il backend lo deduce dal JWT
  - Il frontend invia solo il token JWT nell'header Authorization
- **Nelle migrazioni SQL**: Includere colonna `tenant_id` in tutte le future tabelle di business logic

Esempio futura entità:
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;  // Multi-tenancy support - popolato dal backend via JWT

    private String name;
    // ...
}
```

#### 3. Conventional Commits

Tutti i commit devono seguire il formato Conventional Commits:

```
<type>(<scope>): <subject>
```

Esempi:
```
feat(backend): add user authentication endpoint
fix(frontend): resolve navbar styling issue on mobile
docs: update database migration guide
chore(backend): update spring boot to 3.3.0
test(backend): add unit tests for UserService
```

#### 4. Branching Strategy

- **Lavoro su feature locali**: Creare branch `feature/<nome-feature>` da `dev`
- **Prima di fare commit**: Assicurarsi di essere su branch `feature/*`
- **Pull Request**: Sempre verso `dev`, mai direttamente a `main`
- **Main**: Reserved per release stabili

```bash
# Flusso tipico
git checkout dev
git pull origin dev
git checkout -b feature/mia-feature
# ... fare il lavoro ...
git add .
git commit -m "feat(backend): add dog profile endpoint"
git push origin feature/mia-feature
# Creare PR su GitHub verso dev
```

#### 5. Code Quality

**Backend:**
- Usare Lombok per ridurre boilerplate
- Aggiungere sempre `@Slf4j` ai controller e service
- Scrivere test unitari per la logica di business
- Utilizzare validazioni `@Valid` nei controller

**Frontend:**
- Eseguire ESLint prima del commit: `npm run lint`
- Formattare il codice: `npm run format`
- Usare reactive programming con RxJS dove appropriato
- Tipare sempre le variabili e i return dei metodi

#### 6. Database Migrations (Flyway)

- Nuove migration vanno in `backend/src/main/resources/db/migration/`
- Naming: `V<numero_progressivo>__<descrizione>.sql`
- Esempio: `V2__add_users_table.sql`
- **Importante**: Non modificare file di migration già committati!

### Development Tools

**Backend Development:**
- IDE consigliato: IntelliJ IDEA o VS Code con Extension Pack for Java
- Linter: Built-in Java linter
- Formatter: IntelliJ IDEA formatter o VS Code

**Frontend Development:**
- IDE consigliato: VS Code con Angular Language Service
- Linter: ESLint (`npm run lint`)
- Formatter: Prettier (`npm run format`)

### Debugging

**Backend:**
- Health check: `GET http://localhost:8080/api/health`
- Actuator metrics: `GET http://localhost:8080/api/actuator/metrics`
- Logs: Visibili in console quando si esegue `mvn spring-boot:run`

**Frontend:**
- Browser DevTools (F12)
- Angular DevTools extension per Chrome
- Logs: Console del browser

### Contatti e Supporto

Per dubbi sulla struttura o sulle convenzioni, consultare la documentazione in `docs/` o il README.

---

## Deploy V1 (Docker)

Stack di produzione: **PostgreSQL + Spring Boot + Angular/Nginx** orchestrati con Docker Compose.

### Prerequisiti

- Docker ≥ 24 e Docker Compose ≥ 2
- Porta **80** libera (frontend) e **5432** disponibile per il container

### Quick start

```bash
# 1. Crea il file di environment (una sola volta)
cp .env.example .env
#    ✏️  modifica .env: imposta POSTGRES_PASSWORD, JWT_SECRET, PLATFORM_ADMIN_*,
#        MAIL_HOST/PORT/USERNAME/PASSWORD/FROM, APP_BASE_URL, CORS_ALLOWED_ORIGINS

# 2. Build & avvio di DB + Backend + Frontend
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml up -d --build

# 3. Verifica
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml ps
```

### Configurazione email (produzione)

Il backend usa SMTP per inviare email di verifica e reset password. La soluzione più semplice è un account Gmail dedicato.

**1. Creare un account Gmail** (es. `noreply.miocentro@gmail.com`)

**2. Abilitare l'autenticazione a due fattori**
- Vai su [myaccount.google.com](https://myaccount.google.com) → Sicurezza → Verifica in due passaggi → Attiva

**3. Creare una App Password**
- Sempre in Sicurezza → cerca "App password" → seleziona "Posta" → Genera
- Copia i 16 caratteri generati (es. `abcd efgh ijkl mnop`)

**4. Aggiungere le variabili in `.env`**
```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply.miocentro@gmail.com
MAIL_PASSWORD=abcdefghijklmnop
MAIL_FROM=noreply.miocentro@gmail.com
```

> Limite Gmail: ~500 email/giorno. Per volumi maggiori usare Brevo, Resend o SendGrid (tutti con piano gratuito).

---

### URL

| Servizio  | URL                         |
|-----------|-----------------------------|
| Frontend  | http://localhost             |
| API       | http://localhost/api         |
| Health    | http://localhost/api/actuator/health |

### Logs

```bash
# Tutti i servizi
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml logs -f

# Solo backend
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml logs -f backend
```

### Avviare/fermare un singolo servizio

```bash
# Solo database (es. per manutenzione)
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml up -d postgres

# Riavviare solo il backend dopo un deploy
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml up -d --build backend
```

### Stop & pulizia

```bash
# Stop (preserva volumi)
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml down

# Stop + cancella volumi (⚠️  dati persi)
docker compose --env-file .env -f infra/docker/prod/docker-compose.yml down -v
```

---

## Contribuzione

Per maggiori informazioni sulla struttura, vedi la documentazione in `docs/`.
