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

## Contribuzione

Per maggiori informazioni sulla struttura, vedi la documentazione in `docs/`.
