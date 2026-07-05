# Checklist Operative — Centro Cinofilo

## Checklist pre-release

- [ ] Tutte le feature previste per la release sono mergiate su `dev`.
- [ ] `dev` è verde: unit test backend (`mvn test`) e frontend (`ng test`) passano.
- [ ] Suite di integrazione completa (`mvn verify`, con Docker disponibile in CI) verde.
- [ ] Nessuna migration Flyway distruttiva senza il pattern "expand/contract" (vedi `docs/release/RELEASE_PROCESS.md`).
- [ ] Versione in `backend/pom.xml` e `frontend/package.json` allineata al prossimo tag.
- [ ] `docs/ai/DECISIONS.md` aggiornato con eventuali nuove ADR della release.
- [ ] `docs/ai/ROADMAP.md` aggiornato se la release chiude o avanza item della Roadmap.
- [ ] Backup del database di produzione verificato recente e integro (prerequisito per poter fare rollback in sicurezza).

## Checklist post-release (dopo il merge su `main` e il tag)

- [ ] Pipeline "Release" completata con successo (test + build immagini + push registry + GitHub Release).
- [ ] Release notes generate e coerenti con le modifiche incluse.
- [ ] Deploy eseguito (workflow "Manual Deploy") sull'ambiente di produzione.
- [ ] Healthcheck backend e frontend verdi post-deploy.
- [ ] Smoke test manuale delle funzionalità critiche: login, creazione prenotazione, controllo capacità box.
- [ ] Nessun errore anomalo nei log applicativi nei primi minuti post-deploy.
- [ ] Tag e versione effettivamente in esecuzione sul server corrispondono a quanto atteso (verifica esplicita, non per assunzione).

## Rollback

Procedura dettagliata passo-passo in `docs/release/RELEASE_PROCESS.md` (sezione 9). Riepilogo operativo:

- [ ] Versione target del rollback individuata e confermata (non assunta).
- [ ] Nuovi deploy congelati per la durata dell'operazione.
- [ ] `IMAGE_TAG` nel file `.env` del server aggiornato al tag precedente.
- [ ] `docker compose pull` eseguito (nessun rebuild: l'immagine precedente è già sul registry).
- [ ] `docker compose up -d --no-build` eseguito.
- [ ] Image ID/tag in esecuzione verificato esplicitamente (non assunto) come corrispondente al target.
- [ ] Healthcheck e smoke test verdi come nella checklist post-release.
- [ ] Impatto sullo schema DB valutato: rollback del solo codice sicuro solo se le migration della release erano additive/retrocompatibili; altrimenti fix-forward o disaster recovery.
- [ ] Incidente documentato (causa, versione rotta, versione ripristinata, eventuale problema strutturale emerso).

## Disaster Recovery (perdita totale o grave corruzione del server)

1. Provisioning di un nuovo VPS (Hetzner, stessa immagine Ubuntu LTS).
2. Installazione Docker + Docker Compose.
3. Ripristino della struttura server (`docs/deployment/SERVER.md`): directory, `Caddyfile`, script.
4. Ripristino del file `.env` da copia sicura (vedi "Gestione dei segreti" sotto — **non esiste altrove se non lì**, va conservato con la stessa cura dei backup del database).
5. Restore del database dall'ultimo backup disponibile (preferibilmente quello su storage esterno, se il VPS originale non è più raggiungibile).
6. Deploy dell'ultima immagine di release nota-buona (workflow "Manual Deploy" puntato al nuovo host).
7. Verifica completa: healthcheck, smoke test, controllo che i dati ripristinati siano coerenti (conteggi, ultima prenotazione nota, ecc.).
8. Aggiornamento DNS se l'IP del nuovo VPS è cambiato.

**Obiettivo di Recovery Point (RPO) atteso con backup giornalieri:** fino a 24 ore di dati potenzialmente persi nello scenario peggiore. Se questo non fosse accettabile in futuro (più tenant, dati più critici), la mitigazione è aumentare la frequenza dei backup (es. ogni 6 ore) — non richiede un cambio di strategia, solo di frequenza.

## Gestione dei segreti

- I segreti (password DB, `JWT_SECRET`, credenziali platform-admin, eventuali chiavi API future) vivono **solo** nel file `.env` del server di produzione e nei secret di GitHub Actions (per la pipeline CI/CD) — mai nel codice, mai in Git, mai nei log applicativi.
- Rotazione: quando un segreto viene cambiato (es. rotazione periodica di `JWT_SECRET`, o sospetto di compromissione), va aggiornato in entrambi i posti (server + GitHub Secrets se usato in pipeline) e il servizio riavviato. La rotazione di `JWT_SECRET` invalida tutte le sessioni attive — da pianificare comunicandolo, non da fare a sorpresa.
- Il file `.env` di produzione va incluso nel perimetro di backup/disaster recovery (punto 4 sopra) con lo stesso livello di attenzione riservato ai dati — la sua perdita equivale a dover reimpostare da zero tutte le credenziali del sistema.
- Nessun segreto reale deve mai comparire nei file `.example` già presenti nel repository (`*.env.*.example`) — oggi rispettato, va mantenuto come regola per ogni nuova variabile introdotta.
