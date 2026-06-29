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

## User preferences

- **Librerie standard prima di custom:** prima di ogni implementazione, verificare se esistono librerie, pattern o best practice consolidate che risolvono il problema in modo affidabile. Preferire soluzioni standard e ampiamente adottate rispetto a implementazioni custom, salvo che ci sia una motivazione tecnica o commerciale specifica. Quando si propone una libreria, spiegare perché è stata scelta e quali sono le alternative.
