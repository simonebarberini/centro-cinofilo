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
- Do NOT try to prove an IT suite green inside this sandbox; you'll burn time. Instead verify what the sandbox CAN do: `mvn test-compile` and unit-only runs.
- Since ADR-018, the project uses a standard Surefire/Failsafe split: `mvn test` and `mvn package` run ONLY unit tests and are green in this sandbox (no Docker needed). **`mvn install`/`mvn clean install` are NOT unit-only** — the standard Maven lifecycle runs `verify` (where failsafe's IT run is bound) BEFORE `install`, so `mvn install` always triggers the IT suite too, same as `mvn verify`, and will fail here for the Docker reason above (expected, not a regression). In this sandbox, always use `mvn test` or `mvn package` for a fast Docker-independent check; never `mvn install`/`mvn clean install` to "just build".
- If a genuinely new *IT test class fails, don't assume it's the Docker issue — check the failsafe report's `Caused by` chain for `ContainerLaunchException`/Ryuk sysfs; if it's a different exception, it's a real regression.
- If you must attempt an IT run: `export TESTCONTAINERS_RYUK_DISABLED=true`, run ONE small class, and clean leaked containers afterward: `docker ps -aq --filter ancestor=postgres:16-alpine | xargs -r docker rm -f`.
- `TESTCONTAINERS_RYUK_DISABLED` is an env-only workaround for THIS sandbox — do not commit a `testcontainers.properties` that disables Ryuk, since the user's env has working Ryuk.
- Maven needs an explicit JDK 21 on PATH in this repo (set `JAVA_HOME` to the openjdk-21 nix store path before each `mvn`).
