---
name: vue-composables-patterns
description: Apply Vue 3 composable patterns correctly including ref/reactive ownership, watch and watchEffect, computed derivation, and lifecycle cleanup. Use when extracting shared logic, fixing stale closures, duplicate watchers, or memory leaks in Vue applications.
---

# Vue Composables Patterns

## Workflow

1. Identify logic to extract: data fetch, form state, DOM listeners, media queries, or cross-component coordination.
2. Name composables `use*` — return refs/computed/functions; avoid returning bare reactive objects unless idiomatic for the repo.
3. Choose watcher type: `watch` for explicit sources; `watchEffect` for auto-tracked dependencies with clear teardown.
4. Clean up side effects in composable scope via `onScopeDispose`, `watch` cleanup, or `onUnmounted`.
5. Accept reactive inputs as getters (`() => props.id`) to keep watchers accurate when props change.
6. Keep composables focused — split when fetch, validation, and UI state diverge.
7. Verify with unit tests on composables via `@vue/test-utils` or dedicated harness.

## References

- Read `references/composable-design.md` for API shape, refs vs reactive, and dependency injection.
- Read `references/watch-and-lifecycle.md` for watch vs watchEffect, flush timing, and cleanup.

## Checklist

- No duplicate watchers firing the same side effect.
- Async race conditions handled (abort, stale flag, or request id).
- Event listeners and intervals removed on dispose.
- Computed used for derived state — not manual sync in watchers.
- Composable usable outside components when logic is pure (test-friendly).
- `toRef` / `toRefs` used when destructuring reactive props or store state without losing reactivity.

## Output

Summarize composables created or refactored, watcher changes, lifecycle fixes, and tests/commands run.
