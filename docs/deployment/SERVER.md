# Server & Deployment — Centro Cinofilo

Design del deploy in produzione. Ambiente assunto: VPS Hetzner, Ubuntu LTS, Docker + Docker Compose, Caddy come reverse proxy, PostgreSQL containerizzato. **Nessuna configurazione viene creata in questa milestone** — solo il disegno da approvare.

## Architettura a due livelli di proxy

Lo stack esistente ha già Nginx **dentro** il container frontend, con due ruoli: servire i file statici Angular e fare da proxy verso il backend per `/api/*` (vedi `docs/docker-guide.md`). Questo resta invariato.

Si propone di aggiungere **Caddy come reverse proxy "edge"**, davanti a tutto lo stack, con un ruolo diverso e complementare:

```
Internet
   │
   ▼
Caddy (porta 443/80, host)
   │  - Certificati TLS automatici (Let's Encrypt), rinnovo automatico
   │  - Terminazione TLS
   │  - Routing per dominio (utile se in futuro si aggiungono sottodomini, es. api.dominio.it)
   ▼
frontend (nginx, rete Docker interna)
   │  - Serve la SPA Angular
   │  - Proxy /api/* → backend
   ▼
backend (Spring Boot, rete Docker interna, nessuna porta esposta sull'host)
   ▼
postgres (rete Docker interna, nessuna porta esposta sull'host in produzione)
```

**Perché Caddy e non gestire TLS direttamente in Nginx:** Caddy ottiene e rinnova automaticamente i certificati Let's Encrypt senza configurazione manuale di cron/certbot, riducendo un'intera categoria di problemi operativi (certificati scaduti). Nginx nel container frontend resta come oggi, senza modifiche, e continua a occuparsi solo di SPA + proxy interno verso il backend — nessuna sovrapposizione di responsabilità.

## Struttura del server

```
/opt/centro-cinofilo/
├── docker-compose.yml          # (o i tre file prod/db.yml, backend.yml, frontend.yml, invariati)
├── Caddyfile                   # configurazione del reverse proxy edge
├── .env                        # variabili d'ambiente reali, MAI in Git
├── backups/
│   └── postgres/
│       ├── daily/
│       └── weekly/
├── scripts/
│   ├── deploy.sh                # pull immagini + up -d + healthcheck
│   ├── backup.sh                # pg_dump programmato
│   ├── restore.sh                # pg_restore da un backup specifico
│   └── rollback.sh               # repuntamento a IMAGE_TAG precedente
└── data/
    └── postgres/                # volume dati Postgres persistente
```

Questa struttura riusa i tre file Compose di produzione già esistenti in `infra/docker/prod/` (nessuna modifica proposta alla loro struttura), aggiungendo solo ciò che serve per operare il server (script, backup, Caddy).

## Gestione delle variabili d'ambiente

Il progetto ha già la convenzione corretta: file `.env` non versionato, con `*.example` come template (`.env.prod.example` già esistente e completo). Sul server:

- Il file `.env` reale vive solo su `/opt/centro-cinofilo/.env`, con permessi ristretti (`chmod 600`, proprietario l'utente di deploy).
- Non viene mai copiato via Git; viene creato/aggiornato manualmente al primo setup del server, e aggiornato solo quando cambia un segreto (non ad ogni deploy).
- Il workflow "Manual Deploy" (vedi `CICD.md`) aggiorna solo la variabile di tag immagine (`IMAGE_TAG=vX.Y.Z`), non l'intero file `.env`.

**Evoluzione futura (non necessaria ora):** un secret manager dedicato (es. Docker Secrets, HashiCorp Vault) diventa utile solo quando ci sono più server o più persone che devono accedere ai segreti in modo controllato. Per un singolo VPS con un solo operatore, un file `.env` con permessi ristretti è proporzionato e sufficiente.

## Backup — procedura completa

Il database PostgreSQL è l'unico dato realmente critico e stateful dello stack (backend e frontend sono stateless, ricostruibili da immagine in qualunque momento). La procedura di backup copre quattro aspetti: dump, frequenza, retention, restore e verifica periodica.

### Dump

- Comando: `pg_dump` in **formato custom** (`-F c`), non SQL testuale semplice — il formato custom è compresso, supporta il restore selettivo (singola tabella) e il restore parallelo (`pg_restore -j`), a differenza di un dump SQL piatto.
  ```
  pg_dump -F c -d "$POSTGRES_DB" -U "$POSTGRES_USER" -f /backups/postgres/daily/cinofilo_<timestamp>.dump
  ```
- Eseguito da `scripts/backup.sh`, lanciato **da dentro la rete Docker** (o via `docker compose exec db pg_dump ...`), senza esporre la porta Postgres sull'host.
- Il dump include schema e dati (comportamento di default di `pg_dump`): non serve gestire Flyway separatamente, `schema_history` è parte del dump come qualunque altra tabella.

### Frequenza

- **Giornaliero**, via cron, in un orario a basso traffico (es. 03:00 locale del server).
- Non è necessario un backup più frequente (es. orario) nella fase attuale: il volume di scrittura di un centro cinofilo (prenotazioni, non transazioni finanziarie ad alto volume) rende accettabile un RPO (Recovery Point Objective) di 24 ore — vedi anche `docs/operations/CHECKLISTS.md`.

### Retention

| Tipo | Frequenza di creazione | Quanti se ne tengono | Dove |
|---|---|---|---|
| Giornaliero | 1/giorno | 7 (una settimana) | `backups/postgres/daily/` |
| Settimanale | 1/settimana (es. il dump della domenica, promosso) | 4 (un mese) | `backups/postgres/weekly/` |

Rotazione automatica nello script: cancellazione dei backup giornalieri oltre i 7 più recenti; ogni domenica, copia (non spostamento) dell'ultimo dump giornaliero in `weekly/`, con purge di quelli oltre i 4 più recenti.

### Copia off-site

- Dopo ogni dump riuscito, copia su storage esterno S3-compatibile (es. Hetzner Object Storage), via `rclone` o `aws s3 cp` puntato all'endpoint Hetzner.
- **Motivazione:** un backup che vive solo sullo stesso disco del server che dovrebbe proteggere non copre lo scenario di perdita totale del VPS (guasto hardware, cancellazione accidentale, compromissione) — è lo scenario che la disaster recovery deve coprire (vedi `docs/operations/CHECKLISTS.md`).
- Retention sullo storage esterno: stessa politica (7 giornalieri + 4 settimanali), eventualmente estesa (es. + 1 mensile) dato il costo marginale molto basso dello storage object rispetto al disco del VPS.

### Restore

Procedura manuale (documentata passo-passo in `docs/operations/CHECKLISTS.md`):
1. **Stop del backend** (`docker compose stop backend`) — evita scritture concorrenti durante il restore.
2. Creazione di un database vuoto di appoggio (o drop/recreate del database esistente, a seconda dello scenario: restore di verifica vs restore reale post-incidente).
3. `pg_restore` del dump scelto:
   ```
   pg_restore -d "$POSTGRES_DB" --clean --if-exists /backups/postgres/daily/cinofilo_<timestamp>.dump
   ```
4. **Verifica di integrità:** conteggio righe sulle tabelle chiave (tenant, booking, dog, customer), controllo che `flyway_schema_history` risulti coerente con la versione applicativa che si sta per riavviare.
5. Riavvio del backend, smoke test applicativo (login, lettura di una prenotazione nota).

### Verifica periodica dei backup

**Principio operativo:** un backup mai ripristinato è solo un'ipotesi, non una garanzia. La verifica va pianificata, non lasciata a "se un giorno servirà":
- **Cadenza proposta:** mensile.
- **Procedura:** restore dell'ultimo backup giornaliero disponibile in un ambiente separato (es. un database Postgres temporaneo, anche locale/di test — mai sull'istanza di produzione), seguito dai controlli di integrità del punto 4 sopra.
- **Esito registrato:** un log minimo (anche solo una riga in un file o in una nota operativa) con data del test e esito, per avere evidenza storica che i backup sono effettivamente utilizzabili, non solo generati.
- Se il restore di verifica fallisce, è un incidente da trattare con priorità immediata: un backup non ripristinabile equivale, in pratica, a non avere backup.

## Logging — soluzione minimale per la V1

Nessuno stack di monitoring/logging complesso in questa fase (niente Loki/Grafana/ELK). Tre fonti, tutte già presenti o ottenibili senza nuovi servizi:

1. **Docker logs applicativi** (backend, frontend): `docker compose logs -f <servizio>` per consultazione diretta; rotazione configurata a livello di driver di logging Docker (`max-size`, `max-file` nel Compose) per evitare che i log riempiano il disco del VPS nel tempo — questa è l'unica configurazione da introdurre, non un servizio aggiuntivo.
2. **Log di Caddy**: Caddy produce log di accesso strutturati (JSON) per ogni richiesta HTTP che attraversa l'edge — utili per individuare errori 5xx, pattern di traffico anomalo, tentativi di accesso a path inesistenti. Stessa politica di rotazione (dimensione/numero file) applicata al file di log di Caddy sul disco del VPS.
3. **Endpoint Actuator Health** del backend (protetto — vedi FASE 1 Roadmap, "Protezione endpoint Actuator"): non è un log ma una sonda di stato, consultabile manualmente o da un controllo esterno.

**Evoluzione futura (esplicitamente non necessaria per la V1):** centralizzazione dei log (Loki+Grafana o simili) diventa giustificata solo quando il volume o il numero di servizi rende la consultazione diretta via `docker compose logs` impraticabile — non è il caso con due container applicativi e un solo VPS.

## Monitoring — soluzione minimale per la V1

Per la prima versione operativa, niente stack di monitoring complesso (niente Prometheus/Grafana): tre elementi, tutti a costo/complessità marginale:

1. **Actuator Health** (`/actuator/health`, protetto): sonda di salute applicativa del backend, verificata dal workflow "Manual Deploy" subito dopo ogni deploy (vedi `CICD.md`) e utilizzabile per un controllo esterno periodico.
2. **Docker logs**: consultazione diretta (`docker compose logs`) come prima linea di debug in caso di anomalia; gli healthcheck già previsti nei Compose esistenti (`depends_on: condition: service_healthy`) offrono un primo segnale automatico di container non sano.
3. **Caddy logs**: oltre all'uso per il debug (vedi Logging sopra), i log di accesso di Caddy sono anche la fonte più semplice per capire "il sito ha ricevuto traffico ed è stato raggiungibile" — un controllo grezzo ma sufficiente per la scala attuale, senza bisogno di un tool di monitoring dedicato.

Nessun monitoraggio esterno di uptime (es. UptimeRobot) è incluso come requisito in questa fase: **può essere aggiunto in autonomia in qualunque momento senza impatto sul resto del disegno** (è un servizio esterno che fa polling di un URL pubblico, indipendente dallo stack), ma non è trattato come parte della V1 minimale per restare fedeli al perimetro "Actuator Health + Docker logs + Caddy logs" richiesto.

**Evoluzione futura (non necessaria ora):** Prometheus + Grafana per metriche applicative dettagliate, da introdurre solo quando la scala o i requisiti di SLA lo richiedono davvero — introdurlo ora significherebbe mantenere un'infrastruttura di monitoring più complessa del sistema che monitora.
