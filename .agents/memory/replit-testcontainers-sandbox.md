---
name: Testcontainers in the Replit sandbox
description: Why the Spring Boot + Testcontainers integration suite cannot be run reliably inside the Replit agent sandbox, and what to do instead.
---

# Running Testcontainers integration tests in the Replit sandbox

The backend integration tests (`*IT`) spin up a real PostgreSQL via Testcontainers. This does NOT work reliably inside the Replit agent sandbox.

**Symptoms observed:**
- Ryuk (the Testcontainers resource-reaper container) fails to start: `error mounting "sysfs" to rootfs at "/sys": operation not permitted`. The sandbox forbids the sysfs mount Ryuk needs.
- Even with Ryuk disabled (`TESTCONTAINERS_RYUK_DISABLED=true`), the JVM frequently stalls during Spring context load (past Flyway/Hibernate) and the whole run is OOM-killed under memory pressure. A long-running `mvn test` left leaked `postgres` containers.
- Long/background Maven runs are also unreliable: backgrounded `nohup mvn ... &` processes sometimes die without writing their log, and synchronous runs get killed producing no output (process group reaped).

**Why:** This is an environment limitation of the sandbox (restricted sysfs mounts + limited memory + process-group reaping), NOT a defect in the test code. The user's own environment and CI run Testcontainers fine.

**How to apply:**
- Do NOT try to prove an IT suite green inside this sandbox; you'll burn time. Instead verify what the sandbox CAN do: `mvn test-compile` and unit-only runs (`-Dtest='!*IT,!*IntegrationTest'`).
- If you must attempt an IT run: `export TESTCONTAINERS_RYUK_DISABLED=true`, run ONE small class, and clean leaked containers afterward: `docker ps -aq --filter ancestor=postgres:16-alpine | xargs -r docker rm -f`.
- `TESTCONTAINERS_RYUK_DISABLED` is an env-only workaround for THIS sandbox — do not commit a `testcontainers.properties` that disables Ryuk, since the user's env has working Ryuk.
- Maven needs an explicit JDK 21 on PATH in this repo (set `JAVA_HOME` to the openjdk-21 nix store path before each `mvn`).
