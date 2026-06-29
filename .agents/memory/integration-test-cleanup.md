---
name: Integration-test database cleanup strategy
description: How and why DB teardown is centralized for the backend integration tests, and the constraints that keep it correct.
---

# Integration-test DB cleanup

All `*IT` classes share a single **static** Testcontainers PostgreSQL (in `AbstractPostgresIT`). The `@SpringBootTest`+MockMvc tests are **not transactional** — they COMMIT, so rows written by one test class stay visible to the next. `@DataJpaTest` repository tests are transactional and roll back.

**Decision:** Exactly ONE teardown strategy lives in the base class: a superclass `@BeforeEach` that wipes every table in foreign-key-safe (child→parent) order. Individual test classes must NOT clean up themselves.

**Why:** The original failure was `fk_customer_tenant` violations — each class did its own *partial, inconsistent* `deleteAll()`, so `tenant` was deleted while `customer` rows left by another class still referenced it. Surefire's default run order is filesystem-dependent, so a customer-creating class can run before one that only cleaned tenants/users. A single uniform FK-safe wipe removes the ordering dependency.

**How to apply / invariants:**
- Deletion order must respect the FKs: `email_token` → `booking` → `dog` → `customer` → `app_user` → `tenant`. (`app_user`→`tenant` and `customer`→`tenant` are `ON DELETE RESTRICT`; the dog/booking/email_token FKs are CASCADE.) If you add a table, insert it in child-before-parent position.
- JUnit 5 guarantees superclass `@BeforeEach` runs before subclass `@BeforeEach`, so subclasses always start clean. Base repo fields are kept private (injected, not shadowing subclass fields).
- **ITs must run sequentially.** Shared static DB + per-test wiping is unsafe under parallel execution (one test would wipe another's data mid-run).
- Repos are injected `@Autowired(required=false)` so `@DataJpaTest` slices that don't expose every repo degrade gracefully. **Fragility:** if a future slice exposes a *parent* repo but omits a *child* repo that has leaked rows, parent deletion can still FK-fail. If more tables/slices appear, prefer a single test-only `JdbcTemplate` `TRUNCATE ... CASCADE` cleaner over the optional-repo approach.
- Context-load also requires a mocked mailer: `@MockBean JavaMailSender` + `management.health.mail.enabled=false` live in the base class.
