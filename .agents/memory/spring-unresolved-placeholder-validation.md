---
name: Unresolved Spring placeholders pass @NotBlank but fail format validators
description: Why a missing env var in application.yml can surface as an @Email/@Pattern validation error instead of a placeholder-resolution error, and how to diagnose it.
---

# Unresolved `${VAR}` placeholders and Bean Validation

When `application.yml` declares `someProp: ${ENV_VAR}` with no default and `ENV_VAR` is unset,
Spring does NOT always throw a "could not resolve placeholder" error. In some binding paths
(observed with `@ConfigurationProperties` + `@Validated`), the literal string `"${ENV_VAR}"` gets
bound as the property's value instead.

That literal string is non-empty, so `@NotBlank` does not catch it. But it will fail any format
validator applied to the same field — `@Email`, `@Pattern`, `@Size`, etc. — producing a validation
error message that looks like it's about *format* ("must be a valid email address"), when the
real problem is a **missing environment variable for the active profile**.

**Why this matters:** the error message is misleading. Debugging time is wasted looking for a
malformed value when the actual bug is "no value was ever supplied for this profile."

**How to apply:**
- If a `@ConfigurationProperties` bean with `@Email`/`@Pattern`/format-validated fields fails to
  bind with a message like "must be a valid X", check whether the bound `Value:` in the
  `BindException` report is literally the placeholder text `${...}` before assuming the config
  file itself has a bad value.
- Any required property with no default (by design, e.g. for security-sensitive prod config) must
  have an explicit value supplied for EVERY Spring profile that boots the full `ApplicationContext`
  in tests (not just `dev`/prod) — otherwise every test that loads that context fails at startup,
  masquerading as unrelated test failures.
- A fast way to isolate this class of bug without needing the full test infra (e.g. when
  Testcontainers/Docker isn't available) is to boot the app directly (`spring-boot:run`) against
  any real Postgres with the same profile and property overrides the test base class uses, minus
  the suspected missing property — the `BindException` report shows the exact bound value.
