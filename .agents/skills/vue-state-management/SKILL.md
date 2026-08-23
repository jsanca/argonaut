---
name: vue-state-management
description: Choose and implement Vue state patterns including local refs, composables, URL state, Pinia stores, and server/cache state. Use when fixing prop drilling, inconsistent shared state, stale async data, or deciding between composables, Pinia, and query libraries like TanStack Query in Vue apps.
---

# Vue State Management

## Workflow

1. Classify state: UI ephemeral, form draft, shared client state, server/cache state, or URL-addressable state.
2. Prefer the simplest owner: `ref`/`reactive` locally until a second consumer needs the data.
3. Use URL/search params for shareable filters, tabs, and pagination when appropriate.
4. Use server-state tools (TanStack Query for Vue, Pinia Colada, or fetch composables) for remote data — do not duplicate server data in global stores without reason.
5. Use Pinia for cross-route client state with clear actions and store boundaries.
6. Extract composables for reusable logic that does not need global persistence.
7. Verify with tests simulating user flows and concurrent updates.

## References

- Read `references/state-selection.md` for when to use local, URL, server, composable, and Pinia state.
- Read `references/pinia-and-url-state.md` for store design, storeToRefs, and route query sync.

## Checklist

- Single source of truth per datum — no duplicated server cache.
- State updates are predictable; Pinia actions document side effects.
- Derived values use `computed` — not manual sync in watchers.
- Async race conditions handled (abort, stale flag, or query library dedupe).
- Form state colocated with form unless truly shared.
- Store modules scoped by feature — not one mega-store.

## Output

Summarize state types identified, pattern chosen, files changed, migration notes, and tests covering shared-state behavior.
