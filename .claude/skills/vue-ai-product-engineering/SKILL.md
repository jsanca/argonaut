---
name: vue-ai-product-engineering
description: Build AI-assisted product experiences in Vue including chat UI, streaming responses, loading states, citations, error recovery, and safe handling of model output. Use when implementing chat widgets, copilot panels, streaming SSE/WebSocket clients, or reviewing AI UX for accessibility and trust in Vue 3 apps.
---

# Vue AI Product Engineering

## Workflow

1. Inspect UI stack: Vue version, Pinia/composables, design system, existing API client, and auth/session handling.
2. Define user journeys: ask question, see streaming answer, view sources, retry, cancel, escalate to human.
3. Implement streaming with clear loading/progress states — never a blank screen while tokens arrive.
4. Render model output safely: sanitize markdown/HTML; avoid `v-html` on untrusted content without a sanitizer.
5. Show citations and uncertainty — link retrieved sources; distinguish "I don't know" from confident wrong answers.
6. Handle failures: timeout, rate limit, network drop, partial stream — with recoverable actions.
7. Verify accessibility: keyboard send, focus management, aria-live regions for streaming updates, and mobile layout.

## References

- Read `references/chat-ui-and-streaming.md` for composables, SSE parsing, and UI states.
- Read `references/trust-safety-accessibility.md` for citations, sanitization, a11y, and error UX.

## UX Checklist

- Send disabled while empty or in-flight; cancel/stop available during stream.
- Distinct UI for loading, streaming, complete, error, and empty states.
- Sources visible and clickable where product allows.
- No secrets or API keys in browser code.
- Rate-limit and session errors show actionable messages.
- Long answers remain readable (typography, scroll, code blocks).
- Screen reader users hear meaningful progress without spamming every token.

## Output

After UI work, summarize:

- Components and composables added
- Streaming protocol (SSE, fetch stream, WebSocket)
- States handled and user-visible copy
- Security/a11y measures for rendered model output
- Manual test steps and any automated tests
