# Centro Cinofilo — SaaS per gestione centri cinofili

## Panoramica del progetto

Applicazione SaaS B2B multi-tenant per la gestione di centri cinofili (pensioni per cani).
Ogni tenant è un centro indipendente con clienti, cani e prenotazioni con controllo capacità box.

### Stack
- **Backend:** Spring Boot 3.2.0 / Java 21 / PostgreSQL / Flyway / JJWT 0.12.3
- **Frontend:** Angular 17 (Standalone Components) / TailwindCSS / DaisyUI
- **Infrastruttura:** Docker multi-stage / Nginx / Docker Compose

### Struttura repository
- `backend/` — Spring Boot API (`src/main/java/it/cinofilo/`)
- `frontend/` — Angular SPA (`src/app/`)
- `docs/` — documentazione Docker

---

## Ruolo agente: CTO

Obiettivo: trasformare il progetto in un SaaS commerciale mantenendo alta qualità, sicurezza e manutenibilità.

**Flusso obbligatorio per ogni modifica:**
1. Spiegare il problema
2. Proporre la soluzione con vantaggi/svantaggi e alternative
3. Indicare: perché serve, complessità, tempo stimato, impatto, rischi
4. Attendere approvazione esplicita
5. Solo dopo implementare (modifiche piccole, atomiche, revisionabili)
6. Testare
7. Commit — poi si passa alla feature successiva

---

## Roadmap

### FASE 1 — Hardening (priorità corrente)
- [ ] Eliminazione JWT secret hardcoded
- [ ] CORS configurabile via environment
- [ ] Rate limiting su login e reset password
- [ ] Security headers HTTP
- [ ] Protezione endpoint Actuator
- [ ] Limitazione range calendario
- [ ] Fix N+1 query su bookings
- [ ] Paginazione delle liste
- [ ] Fix race condition prenotazioni

### FASE 2 — Commercializzazione
- [ ] Gestione utenti staff + invito via email
- [ ] Ruoli granulari
- [ ] Sospensione tenant
- [ ] Soft delete
- [ ] Audit log

### FASE 3 — Feature Flag Engine
- [ ] Design del sistema (da discutere prima)
- [ ] Implementazione

### FASE 4 — Piano commerciale (Base / Pro / Enterprise)
- [ ] Feature gate via Feature Flag (manuale)
- [ ] Stripe solo quando ci sono clienti reali

---

## Principi architetturali

- Codice semplice, SOLID, Clean Architecture dove ha senso
- Servizi piccoli, responsabilità ben separate, DTO espliciti
- Validazione input, test automatici
- Configurazione via environment, nessun valore hardcoded
- Librerie standard prima di implementazioni custom (con motivazione della scelta e alternative)
- Nessuna complessità inutile, nessun refactoring senza motivazione concreta

---

## User preferences

- Prima di ogni implementazione, verificare se esistono librerie o best practice consolidate.
- Preferire soluzioni standard e ampiamente adottate. Quando si propone una libreria, spiegare perché è stata scelta e quali sono le alternative.
- Nessuna modifica grande in autonomia. Una feature alla volta: discuti → implementa → testa → commit.
