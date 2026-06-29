# Guida Docker – Centro Cinofilo

Questa guida spiega come funziona Docker nel progetto, partendo dai concetti base fino alla struttura concreta dei file.

---

## 1. Cos'è Docker e perché lo usiamo

Docker risolve il classico problema *"funziona sul mio PC"*. Permette di impacchettare un'applicazione insieme a tutto ciò di cui ha bisogno (runtime, dipendenze, configurazione) in un'unità chiamata **container**, che si comporta in modo identico su qualsiasi macchina.

Concetti chiave:

| Termine | Analogia | Spiegazione |
|---------|----------|-------------|
| **Image** | Ricetta di cucina | Snapshot immutabile e riproducibile di un sistema — definisce *cosa* c'è dentro il container |
| **Container** | Piatto cucinato | Istanza in esecuzione di un'image — può essere avviato, fermato, eliminato |
| **Dockerfile** | Istruzioni della ricetta | File di testo che descrive passo per passo come costruire un'image |
| **Registry** | Supermercato di ricette | Repository da cui scaricare image già pronte (es. Docker Hub) |
| **Volume** | Hard disk esterno | Cartella persistente fuori dal container — i dati sopravvivono al riavvio |
| **Network** | Rete locale virtuale | Canale di comunicazione isolato tra container |

---

## 2. Il Dockerfile – come si costruisce un'image

Un Dockerfile è una sequenza di istruzioni che Docker esegue in ordine per costruire l'image. Ogni istruzione crea uno **strato** (layer) che viene cachato — se un layer non cambia, Docker lo riutilizza accelerando i build successivi.

### Istruzioni principali

```dockerfile
FROM    # image di partenza (base)
WORKDIR # imposta la cartella di lavoro corrente
COPY    # copia file dall'host nel filesystem dell'image
RUN     # esegue un comando durante il build (es. mvn package, npm install)
EXPOSE  # documenta la porta su cui il processo ascolterà
ENV     # imposta variabili d'ambiente
USER    # cambia l'utente con cui girano i processi successivi
ENTRYPOINT / CMD  # comando da eseguire quando il container si avvia
```

### Multi-stage build

Tecnica che usa più blocchi `FROM` nello stesso Dockerfile per separare la fase di build da quella di runtime. Il risultato finale contiene **solo** ciò che serve in produzione, senza compilatori, SDK o file temporanei.

```
Stage 1 (build)   →   compila il codice   →   produce un artefatto
Stage 2 (runtime) →   copia solo l'artefatto →   image finale leggera e sicura
```

---

## 3. I Dockerfile del progetto

### 3.1 Backend – `backend/Dockerfile`

```dockerfile
# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build
# Partiamo da un'image che ha già Maven + JDK 21 installati.
# Il tag "alpine" indica una versione minimale di Linux (~5 MB vs ~200 MB).

WORKDIR /build
# Tutte le istruzioni successive operano in questa cartella.

COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copiamo prima solo il pom.xml e scarichiamo le dipendenze.
# PERCHÉ: se il codice cambia ma il pom.xml no, Docker riusa questo layer
# dalla cache → il download delle dipendenze (~200 MB) avviene una volta sola.

COPY src ./src
RUN mvn package -DskipTests -B && mv target/*.jar target/app.jar
# Ora copiamo il sorgente e compiliamo. I test vengono saltati perché
# devono girare in CI, non dentro Docker.

# ── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
# Partiamo da un'image con solo il JRE (Java Runtime), non il JDK.
# Non abbiamo più bisogno di Maven o del compilatore → image ~3x più piccola.

RUN addgroup -S app && adduser -S app -G app
# Creiamo un utente non-root dedicato. Motivo di sicurezza: se il container
# venisse compromesso, l'attaccante non avrebbe privilegi root sull'host.

WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
# Copiamo SOLO il jar dallo stage di build. Tutto il resto (Maven, sorgenti,
# file temporanei) non entra nell'image finale.

RUN chown -R app:app /app
USER app
# Il processo Java gira come utente "app", non come root.

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# Quando il container parte, avvia la JVM con il nostro jar.
```

**Risultato**: image finale ~180 MB (vs ~600 MB con JDK + Maven).

---

### 3.2 Frontend – `frontend/Dockerfile`

```dockerfile
# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM node:22-alpine AS build
# Node.js per compilare Angular. Alpine = ~50 MB vs ~300 MB.

WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci
# Copiamo prima solo i file delle dipendenze e le installiamo.
# npm ci è più veloce e riproducibile di npm install (usa esattamente
# le versioni in package-lock.json). Se i file non cambiano → layer cachato.

COPY . .
RUN npx ng build --configuration production
# Copiamo il sorgente Angular e lo compiliamo in modalità production:
# tree-shaking, minificazione, bundle ottimizzati.
# Output: /build/dist/frontend/ (HTML + CSS + JS statici)

# ── Stage 2: runtime ────────────────────────────────────────────────────────
FROM nginx:alpine
# Nginx è un web server leggero (~25 MB). Servirà i file statici
# e farà da reverse proxy verso il backend.

RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Sostituiamo la configurazione di default con la nostra.

COPY --from=build /build/dist/frontend /usr/share/nginx/html
# Copiamo i file compilati da Angular nella cartella pubblica di Nginx.
# Nessun Node.js, nessun npm nell'image finale.

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
# Nginx parte in foreground (daemon off) così Docker può gestirne il lifecycle.
```

**Risultato**: image finale ~45 MB (vs ~500 MB con Node.js + Angular CLI).

---

### 3.3 Nginx – `frontend/nginx.conf`

Nginx ha due ruoli in questo progetto:

```nginx
server {
    listen 80;

    # 1. Serve i file statici Angular (SPA)
    location / {
        try_files $uri $uri/ /index.html;
        # Se il file non esiste (es. /bookings non è un file fisico),
        # ritorna sempre index.html → Angular gestisce il routing lato client.
    }

    # 2. Proxy verso il backend Spring Boot
    location /api/ {
        proxy_pass http://backend:8080/api/;
        # "backend" è il nome del container nella rete Docker.
        # Docker risolve automaticamente questo hostname tramite DNS interno.
        # Il browser non sa che esiste un backend separato:
        # chiama /api/auth/login → Nginx lo gira a backend:8080/api/auth/login.
    }
}
```

Grazie a questo, il frontend fa richieste a `/api/...` (stesso dominio) ed evita problemi CORS. Nginx fa tutto il bridging internamente.

---

## 4. Docker Compose – orchestrare più container

Un Dockerfile descrive **un singolo** container. Docker Compose descrive **come far girare insieme più container** e come farli comunicare.

### Struttura base di un compose file

```yaml
services:          # lista dei container da avviare
  nome-servizio:
    image: ...     # usa un'image già pronta dal registry
    # OPPURE
    build: ...     # costruisci l'image dal Dockerfile
    ports: ...     # mappa porte host:container
    expose: ...    # espone porte SOLO sulla rete interna Docker
    environment:   # variabili d'ambiente
    volumes: ...   # monta cartelle o volumi
    depends_on:    # ordine di avvio
    networks: ...  # reti a cui appartiene
    restart: ...   # politica di riavvio automatico
    healthcheck:   # come verificare che il servizio sia pronto

volumes:           # volumi persistenti nominati
networks:          # reti Docker custom
```

### `image` vs `build`

| | `image` | `build` |
|--|---------|---------|
| **Cosa fa** | Scarica un'image già pronta da Docker Hub | Costruisce l'image localmente dal Dockerfile |
| **Quando usare** | Servizi standard (postgres, mailhog, nginx) | Nostro codice (backend, frontend) |
| **Esempio** | `image: postgres:16-alpine` | `build: { context: ./backend }` |
| **Prima volta** | Download (~pochi secondi) | Compilazione (~minuti) |
| **Con `--build`** | Ignora cache e ri-scarica | Ignora cache e ricompila |

---

## 5. I compose file del progetto

Il progetto ha tre ambienti, ognuno con i suoi compose file:

```
infra/docker/
├── dev/            → Solo DB. BE e FE girano in locale (npm start, mvn spring-boot:run)
├── prod/           → DB + BE + FE containerizzati. Tre file separati per flessibilità
└── local-prod/     → Stack completo in un file solo + MailHog per test email
```

### 5.1 Dev – `infra/docker/dev/docker-compose.yml`

Avvia solo PostgreSQL. Il backend e il frontend girano direttamente sulla macchina così si può sviluppare con hot reload e debug.

```
HOST
├── npm start (FE) → :4200 → proxy → :8080
├── mvn spring-boot:run (BE) → :8080 → jdbc → :5432
└── Docker
    └── postgres → :5432
```

### 5.2 Prod – `infra/docker/prod/*.yml`

Tre file separati che si possono comporre:

- `db.yml` → PostgreSQL con volume persistente
- `backend.yml` → Spring Boot (dipende da `db`)
- `frontend.yml` → Angular + Nginx (dipende da `backend`)

La separazione permette di riavviare o aggiornare un solo componente senza toccare gli altri.

### 5.3 Local-prod – `infra/docker/local-prod/docker-compose.yml`

Stack completo in un file unico, pensato per simulare la produzione in locale:

```
HOST
└── Docker (rete interna: app-net)
    ├── postgres    → porta 5433 (esterna, per DBeaver)
    ├── mailhog     → porta 8025 (Web UI email), 1025 (SMTP interno)
    ├── backend     → nessuna porta esterna (raggiungibile solo da frontend)
    └── frontend    → porta 80 (esterna) → nginx → /api/* → backend:8080
```

MailHog intercetta tutte le email inviate dal backend senza recapitarle davvero. Puoi aprire `http://localhost:8025` e leggere le email come se fossi il destinatario.

---

## 6. Reti e DNS interno

Quando due container sono sulla stessa rete Docker, possono parlarsi usando il **nome del servizio** come hostname. Docker ha un DNS interno che risolve automaticamente questi nomi.

```yaml
# docker-compose.yml
services:
  backend:
    networks: [app-net]
  frontend:
    networks: [app-net]
```

Il container `frontend` può raggiungere il backend con `http://backend:8080` — Docker risolve `backend` all'IP interno del container. Questo è esattamente quello che fa la configurazione nginx:

```nginx
proxy_pass http://backend:8080/api/;
```

Dall'esterno (il tuo browser), invece, puoi raggiungere solo le porte dichiarate in `ports`. Le porte in `expose` sono visibili solo agli altri container sulla stessa rete.

---

## 7. Volumi – persistere i dati

I container sono **efimeri**: quando vengono eliminati, tutti i dati al loro interno spariscono. Per i database questo è un problema. I volumi risolvono tutto.

```yaml
services:
  postgres:
    volumes:
      - postgres_data:/var/lib/postgresql/data
      # nome-volume : percorso-dentro-il-container

volumes:
  postgres_data:   # Docker gestisce questo volume fuori dal container
    driver: local
```

Il volume `postgres_data` esiste indipendentemente dal container. Puoi fare `docker compose down` (rimuove il container) e quando fai `up` di nuovo i dati sono ancora lì.

Con `docker compose down -v` invece elimini anche i volumi → reset completo del database.

---

## 8. Health check e `depends_on`

`depends_on` con `condition: service_healthy` garantisce che il backend parta solo dopo che il database è effettivamente pronto ad accettare connessioni (non solo avviato).

```yaml
services:
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U cinofilo"]
      interval: 10s   # controlla ogni 10s
      timeout: 5s     # timeout del controllo
      retries: 5      # quante volte riprovare prima di dichiararlo unhealthy

  backend:
    depends_on:
      postgres:
        condition: service_healthy   # aspetta che postgres sia healthy
```

Senza questo, il backend potrebbe partire prima che PostgreSQL sia pronto e andare in crash al primo tentativo di connessione.

---

## 9. Variabili d'ambiente e file `.env`

Le variabili sensibili (password, chiavi JWT, configurazioni d'ambiente) non vengono scritte direttamente nel compose file ma in un file `.env` separato che non viene committato su Git.

```bash
# .env.local-prod
POSTGRES_PASSWORD=mia-password-sicura
JWT_SECRET=chiave-super-segreta-256-bit
```

```yaml
# docker-compose.yml
services:
  postgres:
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}   # letto dal file .env
```

Il file `*.example` nel repo mostra la struttura senza valori reali — serve come template da copiare.

---

## 10. Comandi utili

```bash
# Avvia tutti i servizi (con build se necessario)
docker compose --env-file .env.local-prod -f infra/docker/local-prod/docker-compose.yml up -d --build

# Vedi lo stato dei container
docker compose -f infra/docker/local-prod/docker-compose.yml ps

# Segui i log in tempo reale
docker compose -f infra/docker/local-prod/docker-compose.yml logs -f

# Log di un singolo servizio
docker compose -f infra/docker/local-prod/docker-compose.yml logs -f backend

# Ferma tutto (i dati rimangono nel volume)
docker compose -f infra/docker/local-prod/docker-compose.yml down

# Ferma tutto e cancella i volumi (reset DB)
docker compose -f infra/docker/local-prod/docker-compose.yml down -v

# Ricostruisce solo il backend senza toccare DB e frontend
docker compose --env-file .env.local-prod -f infra/docker/local-prod/docker-compose.yml up -d --build backend

# Apri una shell dentro un container in esecuzione
docker exec -it cinofilo-local-backend sh

# Connessione diretta al DB dal terminale
docker exec -it cinofilo-local-db psql -U cinofilo -d cinofilo
```
