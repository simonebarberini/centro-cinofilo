---
name: Spring Boot @ConfigurationProperties — chiavi di mappa con caratteri speciali
description: Come usare path come /auth/login come chiavi di mappa in @ConfigurationProperties senza che Spring normalizzino via relaxed binding.
---

## Regola

Quando una `Map<String, T>` in `@ConfigurationProperties` usa chiavi con `/` o altri caratteri speciali (es: path HTTP), usare la bracket notation di Spring Boot:

**In YAML:**
```yaml
rate-limit:
  endpoint-policies:
    "[/auth/login]": LOGIN
    "[/auth/register]": REGISTER
```

**In @TestPropertySource(properties = {...}):**
```java
"rate-limit.endpoint-policies.[/auth/login]=LOGIN"
```

**Why:** Spring Boot relaxed binding normalizza le chiavi di mappa rimuovendo i caratteri non-alfanumerici. `/auth/login` diventa `authlogin`. La bracket notation `[/auth/login]` è il meccanismo ufficiale Spring Boot per preservare le chiavi verbatim. Senza bracket, `PolicyRegistry.findByPath("/auth/login")` non trova mai la policy a runtime.

**How to apply:** A ogni `@ConfigurationProperties` che usa path HTTP o chiavi con `.`, `/`, `-` come chiavi di `Map<String, T>`.

Nota: costruire `RateLimitProperties` direttamente in Java (nei test unitari puri come `PolicyRegistryTest`) non ha questo problema — il binding YAML non è coinvolto.
