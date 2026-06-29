# Memory index

- [Testcontainers in Replit sandbox](replit-testcontainers-sandbox.md) — IT suite can't be run reliably here (Ryuk sysfs block + OOM during context load); verify in user env/CI.
- [Integration-test DB cleanup](integration-test-cleanup.md) — shared static Postgres + non-tx @SpringBootTest commits leak rows; use one FK-safe teardown in the base class, run ITs sequentially.
