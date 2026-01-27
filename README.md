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

## Contribuzione

Per maggiori informazioni sulla struttura, vedi la documentazione in `docs/`.
