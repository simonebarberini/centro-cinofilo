---
name: JWT algorithm must be pinned explicitly on both sides
description: Why JWT signing and verification must hard-pin the same HMAC algorithm, and how JJWT silently derives it from key length
---

The HMAC algorithm for JWTs must be an **explicit, identical choice on BOTH the signing
side and the verification side**. Never let it be inferred.

**Why:** A 401 "JWT invalid" bug shipped because the two sides disagreed on the algorithm
(not the key). JJWT's `Keys.hmacShaKeyFor(bytes)` + a bare `signWith(key)` selects the HMAC
variant from the key's BYTE LENGTH (32–47B→HS256, 48–63B→HS384, ≥64B→HS512), while the Spring
resource-server `NimbusJwtDecoder` was pinned to a single `MacAlgorithm`. A 60-byte dev secret
produced HS384 tokens that the HS256-pinned decoder rejected. It was invisible because the
test secret happened to be 47 bytes → HS256, coincidentally matching the decoder.

**How to apply:**
- Sign with an explicit algorithm: build the key as `new SecretKeySpec(bytes, "HmacSHA512")`
  and call `.signWith(key, Jwts.SIG.HS512)` — do NOT use `Keys.hmacShaKeyFor` (length-derived).
- Decoder must pin the same: `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(MacAlgorithm.HS512)`.
- Enforce the secret's minimum byte length for the chosen algorithm at startup (fail-fast in a
  singleton bean constructor → blocks Spring context creation). HS512 needs ≥64 bytes.
- Byte length ≠ entropy: docs should recommend `openssl rand -hex 64`, not human-readable strings.

**Testing trap to avoid:** A unit test that only round-trips the signer against itself, or an
integration test whose secret length coincidentally matches the decoder, will NOT catch a
signer/decoder algorithm divergence. The real regression guard must wire the production
`JwtService` AND the production `JwtDecoder` bean together and assert the decode succeeds with
the expected `alg` header.
