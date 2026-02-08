# Centro Cinofilo

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

## Prerequisiti

- Node.js (v16 o superiore)
- Java (v11 o superiore)
- Docker & Docker Compose
- Git

## Comandi Principali

### Setup iniziale
```bash
# Installare dipendenze frontend
cd frontend && npm install

# Il backend (Maven) scaricherà le dipendenze automaticamente al primo build
cd backend && mvn clean install
```

### Sviluppo
```bash
# Avviare backend
cd backend && mvn spring-boot:run

# Avviare frontend
cd frontend && npm run dev

# Avviare con Docker Compose
docker-compose -f infra/docker/docker-compose.yml up
```

### Build
```bash
# Build backend
cd backend && mvn clean package

# Build frontend
cd frontend && npm run build
```

### Testing
```bash
# Test backend
cd backend && mvn test

# Test frontend
cd frontend && npm test
```

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

## Avvio DB locale

### Prerequisiti
- Docker e Docker Compose installati
- File `.env` configurato (copia da `.env.example` se necessario)

### Avvio del database PostgreSQL

```bash
# Avviare il container PostgreSQL
docker-compose -f infra/docker/docker-compose.yml up -d

# Verificare lo stato del container
docker-compose -f infra/docker/docker-compose.yml ps

# Visualizzare i log
docker-compose -f infra/docker/docker-compose.yml logs -f postgres

# Fermare il database
docker-compose -f infra/docker/docker-compose.yml down

# Fermare e rimuovere il volume (reset completo)
docker-compose -f infra/docker/docker-compose.yml down -v
```

### Connessione al database

**Host**: localhost  
**Porta**: 5432  
**Database**: cinofilo  
**Username**: cinofilo  
**Password**: cinofilo

Esempio con `psql`:
```bash
psql -h localhost -U cinofilo -d cinofilo
```

## Avvio Backend

### Prerequisiti
- Java 21 o superiore
- Maven
- PostgreSQL in esecuzione (opzionale per lo sviluppo iniziale)

### Avvio dell'applicazione Spring Boot

```bash
# Posizionarsi nella cartella backend
cd backend

# Installare dipendenze Maven (opzionale, verrà fatto automaticamente)
mvn clean install

# Avviare l'applicazione
mvn spring-boot:run

# Oppure compilare e lanciare il JAR
mvn clean package
java -jar target/backend-1.0.0.jar
```

### Configurazione database

L'applicazione legge le configurazioni del database da variabili di ambiente:

```bash
# Con Docker Postgres in esecuzione
mvn spring-boot:run

# Oppure con configurazione personalizzata
mvn spring-boot:run -Dspring-boot.run.arguments="--DB_HOST=localhost --DB_PORT=5432 --DB_NAME=cinofilo --DB_USER=cinofilo --DB_PASSWORD=cinofilo"
```

### Endpoint di prova

Una volta avviato, l'applicazione sarà disponibile su `http://localhost:8080/api`

- **Health Check**: `GET http://localhost:8080/api/health` → `{"status": "ok"}`
- **Actuator**: `GET http://localhost:8080/api/actuator/health`

### Flyway Migrations

Le migrazioni del database si trovano in `src/main/resources/db/migration/`. Flyway si esegue automaticamente all'avvio dell'applicazione:
- La baseline iniziale si trova in `V1__init.sql`
- Per aggiungere nuove migrazioni, creare file nominati come `V<numero>__<descrizione>.sql`

## Script di Avvio Rapidi

### Avvio completo dello stack (Database + Backend + Frontend)

```bash
# 1. Avviare il database PostgreSQL
docker-compose -f infra/docker/docker-compose.yml up -d

# 2. Attendere che il database sia pronto (~5 secondi)
# 3. Avviare il backend in un terminale
cd backend && mvn spring-boot:run

# 4. Avviare il frontend in un altro terminale
cd frontend && npm run dev
```

L'applicazione sarà disponibile su:
- **Backend API**: `http://localhost:8080/api`
- **Frontend**: `http://localhost:4200`

### Comandi di Pulizia

```bash
# Fermare e rimuovere i container Docker
docker-compose -f infra/docker/docker-compose.yml down

# Reset completo del database (rimuove il volume)
docker-compose -f infra/docker/docker-compose.yml down -v

# Pulire i build locali
cd backend && mvn clean
cd frontend && npm run clean 2>/dev/null || echo "No build folder"
```

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

#### 1. Setup Iniziale

```bash
# Clonare il repository
git clone <repo-url>
cd centro-cinofilo

# Copiare le variabili di ambiente
cp .env.example .env
```

#### 2. Avviare PostgreSQL

```bash
# Nella radice del progetto
docker-compose -f infra/docker/docker-compose.yml up -d

# Verificare che sia in esecuzione
docker-compose -f infra/docker/docker-compose.yml ps
```

Credenziali default:
- Host: `localhost`
- Port: `5432`
- Database: `cinofilo`
- Username: `cinofilo`
- Password: `cinofilo`

#### 3. Avviare il Backend

```bash
# Nella directory backend
cd backend

# Build e avvio
mvn clean install
mvn spring-boot:run

# L'applicazione sarà disponibile su http://localhost:8080/api
```

Test endpoint:
```bash
curl http://localhost:8080/api/health
# Risposta: {"status":"ok"}
```

#### 4. Avviare il Frontend

```bash
# Nella directory frontend
cd frontend

# Installare dipendenze (prima volta)
npm install

# Avviare il dev server
npm run dev

# L'applicazione sarà disponibile su http://localhost:4200
```

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
cp .env.prod.example .env.prod
#    ✏️  modifica .env.prod con password e JWT_SECRET reali

# 2. Build & avvio
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod up -d --build

# 3. Verifica
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod ps
```

### URL

| Servizio  | URL                         |
|-----------|-----------------------------|
| Frontend  | http://localhost             |
| API       | http://localhost/api         |
| Health    | http://localhost/api/actuator/health |

### Logs

```bash
# Tutti i servizi
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod logs -f

# Solo backend
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod logs -f backend
```

### Stop & pulizia

```bash
# Stop (preserva volumi)
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod down

# Stop + cancella volumi (⚠️  dati persi)
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod down -v
```

---

## Contribuzione

Per maggiori informazioni sulla struttura, vedi la documentazione in `docs/`.
