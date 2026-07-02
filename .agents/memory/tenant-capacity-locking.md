---
name: Serializing tenant-capacity changes (overbooking race)
description: How concurrent occupancy-changing operations are serialized to keep the per-tenant capacity invariant, and the design constraints behind it.
---

# Serializing tenant-capacity changes

The capacity invariant is: per day, count of CONFIRMED overlapping bookings ≤ `tenant.capacityBoxes`
(an integer on the tenant — there is NO Box entity; ranges are start-inclusive / end-exclusive;
`BookingStatus` is only CONFIRMED / CANCELLED). A naive check-then-write overbooks under concurrency
(TOCTOU): two transactions both read "1 free" and both insert.

**Decision:** Serialize every occupancy-changing operation on the **tenant row** with a
`PESSIMISTIC_WRITE` lock, funneled through ONE domain component. Capacity check + persist run while
the lock is held; the lock releases at commit, so a blocked transaction re-reads the committed
booking when it proceeds. Works under READ_COMMITTED — no SERIALIZABLE / retry loop needed.

**Why a tenant-row lock (not unique constraints / SERIALIZABLE / app locks):** capacity is a
per-day *aggregate* (`count ≤ N`), which a unique index cannot express. A single coarse lock per
tenant is simple, deadlock-free (always the same single row, locked first), and tenant-scoped so it
doesn't serialize unrelated tenants. SERIALIZABLE would need client-side retry on serialization
failures — more moving parts for no gain at this scale.

**Design constraints the user (CTO) insisted on — keep them:**
- The component's PUBLIC API speaks the DOMAIN (tenant capacity), never the mechanism. No "lock" /
  "critical section" / "mutex" in type or method names. Method is `executeForTenant(tenantId, Supplier)`.
  Javadoc must state it is the SOLE authorized point that serializes all tenant-capacity-changing ops.
- Propagation is `@Transactional(MANDATORY)`: the guard must run inside the caller's transaction so
  the row lock lives until that transaction commits. It is a separate Spring bean from the service so
  the proxy actually applies.
- Reducing `capacityBoxes` is a DIFFERENT, deferred concern — settings changes can still break the
  invariant after bookings exist; guard/validate that workflow separately, don't fold it in here.

**How to apply / invariants:**
- Tenant-first lock ordering is mandatory. Acquire the tenant lock as the FIRST occupancy-changing
  DB action. Keep the locked section short: do customer/dog lookups, request/date validation, and
  building the entity OUTSIDE the guard; do the capacity check + setters + save INSIDE it.
  Pitfall: JPA auto-flush runs pending dirty changes before the next query — if you mutate a managed
  entity (e.g. update path) before entering the guard, those writes flush before the lock. So apply
  the entity setters INSIDE the guarded lambda, after the lock, not before.
- Lambda captures must be effectively final (precompute new dates/status into locals).
- create / update / cancel all route through the guard for uniformity (even cancel, which only frees
  capacity). The capacity check itself runs only when the resulting status is CONFIRMED, and excludes
  the booking's own id.
- Tenant-not-found inside the guard is a should-never-happen invariant (tenant already resolved from
  the authenticated context): throw `IllegalStateException` (matches sibling services), NOT the
  auth-layer `TenantNotFoundException` — avoids coupling bookings → auth.
- Validate concurrency with a Testcontainers IT (two concurrent creates at capacity=1 via a
  CyclicBarrier: exactly one succeeds, the other throws OverbookingException, final CONFIRMED == 1).
  See replit-testcontainers-sandbox.md — that IT can't be executed in the Replit sandbox; it runs in
  CI/local.

**Future direction (documented decision, NOT yet implemented — see replit.md "Decisioni
architetturali" ADR-001):** this guard is intended to grow from a pure synchronization primitive
into the owner of the whole "tenant capacity" domain (waitlist, multiple bookings, bulk imports,
controlled overbooking). When adding any capacity/occupancy-changing feature, concentrate the logic
HERE, not in BookingService (which stays a use-case coordinator). This keeps the capacity domain from
fragmenting as the product grows.
