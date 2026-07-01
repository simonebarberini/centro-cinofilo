# ROADMAP — Centro Cinofilo SaaS

Stato ricostruito dall'analisi del codice sorgente e dalla roadmap in `replit.md`.

**Legenda:**
- ✅ Completato — evidenza diretta nel codice
- ⬜ Non verificato — assenza di evidenza nell'analisi statica (può essere implementato; verificare nel codice)
- 🚫 Non implementato — confermato assente

---

## FASE 1 — Hardening (priorità corrente)

| Item | Stato | Note |
|------|-------|------|
| Eliminazione JWT secret hardcoded | ✅ Completato | `JwtService` valida lunghezza minima al bootstrap; secret sempre via env var `JWT_SECRET` |
| CORS configurabile via environment | ✅ Completato | `CorsProperties` + env var `CORS_ALLOWED_ORIGINS` — lista separata da virgole |
| Rate limiting su login e reset password | ✅ Completato | Package `ratelimit` con Bucket4j; doppio bucket su login (`IP_TENANT` + `TENANT_USERNAME`) |
| Security headers HTTP | ✅ Completato | Header API configurati in `SecurityConfig`; HSTS e CSP deliberatamente demandati a Nginx |
| Fix race condition prenotazioni | ✅ Completato | `TenantCapacityGuard` con lock pessimistico (`SELECT FOR UPDATE`) |
| Protezione endpoint Actuator | ⬜ Non verificato | Non confermato da analisi statica — verificare `SecurityConfig` e `application.yml` |
| Limitazione range calendario | ⬜ Non verificato | Non confermato da analisi statica — verificare `BookingService` e frontend |
| Fix N+1 query su bookings | ⬜ Non verificato | Non confermato da analisi statica — verificare query nei repository e `BookingService` |
| Paginazione delle liste | ⬜ Non verificato | Non confermato da analisi statica — verificare controller e repository |

---

## FASE 2 — Commercializzazione

| Item | Stato | Note |
|------|-------|------|
| Gestione utenti staff + invito via email | 🚫 Non implementato | Il ruolo `TENANT_STAFF` esiste nell'enum `Role` ma non c'è gestione inviti |
| Ruoli granulari | 🚫 Non implementato | `Role` ha 3 valori ma non ci sono permessi granulari per funzione |
| Sospensione tenant | 🚫 Non implementato | Nessun campo `suspended` o `status` su `Tenant` |
| Soft delete | 🚫 Non implementato | Nessun campo `deletedAt` o `deleted` sulle entità |
| Audit log | 🚫 Non implementato | Nessuna tabella o meccanismo di audit log |

---

## FASE 3 — Feature Flag Engine

| Item | Stato | Note |
|------|-------|------|
| Design del sistema | 🚫 Non iniziato | Infrastruttura entitlement parzialmente presente (`BooleanEntitlement`, `QuotaEntitlement`, tabella `module`) ma il motore di feature flag non è implementato |
| Implementazione | 🚫 Non iniziato | — |

---

## FASE 4 — Piano commerciale (Base / Pro / Enterprise)

| Item | Stato | Note |
|------|-------|------|
| Feature gate via Feature Flag | 🚫 Non implementato | Dipende dalla Fase 3 |
| Stripe (solo con clienti reali) | 🚫 Non implementato | Decisione deliberata: integrare solo quando ci sono clienti reali |

---

## Nota metodologica

Gli stati "Non verificato" nella Fase 1 indicano che l'analisi statica del codice non ha trovato evidenza diretta dell'implementazione, ma non escludono che essa esista. Per quegli item, verificare direttamente nel codice sorgente prima di implementare.

Gli stati "Non implementato" nelle Fasi 2–4 sono basati sull'assenza di strutture dati, endpoint e logica corrispondente nel codice analizzato.
