---
name: HTTP security headers ownership & deferral
description: How security headers are split between Nginx (SPA) and Spring (API), and which headers are intentionally deferred and why.
---

# HTTP security headers — ownership split & deferral

**Rule — two owners, no overlap:**
- **Nginx owns headers for the Angular SPA** (the `index.html` shell + hashed static assets). Set them in the `location /` and the static-asset regex block only.
- **Spring owns headers for `/api` responses** (a private `securityHeaders()` Customizer applied to BOTH SecurityFilterChains — public auth/health and the protected resource server).
- Nginx must NOT add security headers on the `/api/` proxy location, or API responses get double-set.

**Why:** API responses and the SPA document have different needs (e.g. API = `no-store` + `no-referrer`; SPA = `no-cache` on index.html, long-immutable on assets, `strict-origin-when-cross-origin`). One layer per response type keeps each concern in the layer that actually serves that response.

**Nginx precedence gotcha:** a regex `location` wins over a prefix `location` unless the prefix uses `^~` (or exact `=`). The `/api/` proxy MUST be `location ^~ /api/`, otherwise an API path ending in `.js/.css/...` would be captured by the static-asset regex (404 + wrong SPA headers), breaking the ownership split.

**HSTS is default-ON in Spring Security** for secure requests. To *defer* HSTS you must explicitly call `.httpStrictTransportSecurity(h -> h.disable())` inside the headers config — omitting it is NOT the same as disabling it. CSP/COOP/CORP/COEP are default-OFF, so nothing to disable for those.

**Deferred deliberately (do not add until decided):** Content-Security-Policy and Strict-Transport-Security.
**Why deferred:** they depend on the final deploy architecture, TLS termination point, and future integrations (Stripe / OAuth / CDN). Introducing them once, with the full picture, avoids repeated churn.
**How to apply:** when re-opening this, decide CSP source allowlists + HSTS max-age/preload together with the deploy/TLS design; re-enable HSTS in Spring and add CSP in whichever layer serves the document.

**Permissions-Policy:** kept minimal (`geolocation=(), camera=(), microphone=()`). `payment=` is intentionally omitted so a future Stripe Payment Request integration is not pre-blocked; tighten later if Stripe is ruled out.
