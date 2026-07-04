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

## Backup

- **Cosa:** `pg_dump` del database Postgres (unico dato realmente critico e stateful dello stack — backend e frontend sono stateless, ricostruibili da immagine).
- **Quando:** giornaliero via cron, eseguito da `scripts/backup.sh` dentro/verso il container Postgres.
- **Retention proposta:** 7 backup giornalieri + 4 settimanali (rotazione automatica nello script, cancellando i più vecchi).
- **Dove:** primariamente su disco del VPS (`backups/postgres/`), con copia periodica su storage esterno (es. Hetzner Object Storage, compatibile S3) per la disaster recovery — un backup che vive solo sullo stesso disco del server che dovrebbe proteggere non copre lo scenario di perdita totale del VPS.

## Restore

Procedura (manuale, documentata in `docs/operations/CHECKLISTS.md`): stop del backend (per evitare scritture concorrenti durante il restore), `pg_restore` del backup scelto in un database pulito, verifica di integrità (conteggio righe per tabella chiave, controllo Flyway `schema_history`), riavvio del backend.

**Principio operativo:** un backup va testato periodicamente con un restore reale (es. su un ambiente locale/di test), non solo prodotto e archiviato — un backup mai ripristinato è solo un'ipotesi, non una garanzia.

## Logging

- Nel breve termine: `docker compose logs`, con rotazione configurata a livello di driver di logging Docker (`max-size`, `max-file`) per evitare che i log riempiano il disco del VPS nel tempo.
- Evoluzione futura (non necessaria ora, da valutare solo se il volume di log lo giustifica): centralizzazione con stack leggero tipo Loki + Grafana, o invio a un servizio gestito.

## Monitoring

Approccio minimo e proporzionato alla scala attuale (un VPS, pochi tenant):
1. **Healthcheck Docker** già previsti nei Compose esistenti (`depends_on: condition: service_healthy`).
2. **Endpoint Actuator** del backend, protetto (vedi FASE 1 Roadmap — "Protezione endpoint Actuator", ancora da verificare/implementare), usato come sonda di salute applicativa.
3. **Monitoraggio esterno di uptime** (es. UptimeRobot o simili, gratuito per un singolo endpoint) come primo livello di allerta se il sito non risponde — indipendente dal server stesso, quindi rileva anche un VPS completamente giù.

**Evoluzione futura (non necessaria ora):** Prometheus + Grafana per metriche applicative dettagliate, da introdurre solo quando la scala o i requisiti di SLA lo richiedono davvero — introdurlo ora significherebbe mantenere un'infrastruttura di monitoring più complessa del sistema che monitora.
