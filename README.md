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
# Installare dipendenze
npm install

# Installare dipendenze backend
cd backend && npm install

# Installare dipendenze frontend
cd frontend && npm install
```

### Sviluppo
```bash
# Avviare backend
cd backend && npm run dev

# Avviare frontend
cd frontend && npm run dev

# Avviare con Docker Compose
docker-compose -f infra/docker/docker-compose.yml up
```

### Build
```bash
# Build backend
cd backend && npm run build

# Build frontend
cd frontend && npm run build
```

### Testing
```bash
# Test backend
cd backend && npm test

# Test frontend
cd frontend && npm test
```

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

## Contribuzione

Per maggiori informazioni sulla struttura, vedi la documentazione in `docs/`.
