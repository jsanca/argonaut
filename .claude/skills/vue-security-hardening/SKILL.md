---
name: vue-security-hardening
description: Secure Vue and browser-facing applications against XSS, unsafe HTML, token exposure, CSRF, and client-side secret leakage. Use when reviewing frontend auth flows, rendering user content with v-html, storing tokens, or hardening SPA security in Vue 3 apps.
---

# Vue Security Hardening

## Workflow

1. Identify trust boundaries: server-rendered HTML, user-generated content, URL params, localStorage, third-party scripts.
2. Review auth token storage and refresh — prefer HttpOnly cookies for session tokens when architecture allows.
3. Eliminate XSS vectors: sanitize HTML, avoid `v-html` on untrusted input, validate URLs in links and dynamic `href`.
4. Ensure API calls include CSRF protection when using cookie-based auth.
5. Audit dependencies for known vulnerabilities (`npm audit`, lockfile CI gate).
6. Confirm Content Security Policy and security headers are set at CDN/server — not only client code.
7. Add tests for escaping, auth redirects, and forbidden access UI paths where feasible.

## References

- Read `references/xss-and-content-rendering.md` for sanitization, markdown, v-html, and URL handling.
- Read `references/auth-and-storage.md` for tokens, cookies, CSRF, and env secrets in Vite.

## Checklist

- No API keys or private tokens in client bundle (`import.meta.env.VITE_*` is public).
- User HTML sanitized before `v-html`; markdown uses sanitizer plugin.
- `target="_blank"` links use `rel="noopener noreferrer"`.
- Auth guards enforced server-side — client routing hide is not security.
- Sensitive data not stored in localStorage unless threat model accepts XSS theft.
- Logout clears client state (Pinia) and invalidates session server-side.

## Output

Summarize threats addressed, code/config changes, residual risks, and verification steps.
