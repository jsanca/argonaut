# Eval: Vue Security Hardening

## Prompt

Review a Vue 3 app that stores JWT refresh tokens in localStorage, renders user markdown with v-html, and uses router guards as the only auth enforcement. List risks and concrete fixes.

## Expected Agent Behavior

- Identifies XSS token theft via localStorage
- Recommends HttpOnly cookies or threat-model-aware storage
- Requires sanitization for v-html/markdown
- Stresses server-side authorization on APIs
- Mentions CSRF if moving to cookies

## Failure Signals

- Says router guard is sufficient security
- Keeps v-html without sanitizer
- Suggests encoding JWT in sessionStorage as "fix"
