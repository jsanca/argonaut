---
name: vue-testing-verification
description: Write and review Vue tests with vue-test-utils, Vitest, and user-centric assertions. Use when adding component tests, fixing flaky UI tests, testing composables, forms, async UI, or improving frontend CI verification in Vue 3 apps.
---

# Vue Testing Verification

## Workflow

1. Inspect test runner (Vitest), `@vue/test-utils` setup, MSW usage, and existing test conventions.
2. Test behavior users see — not implementation details (internal refs, private composable state unless extracted).
3. Prefer role and label queries via `@testing-library/vue` when project uses it; otherwise test-utils `find`/`get` with accessible selectors.
4. Use `userEvent` or `trigger` for interactions; await async UI with `flushPromises`, `find`, or `waitFor`.
5. Mock network at HTTP boundary (MSW) rather than mocking every child component.
6. Cover loading, success, error, and empty states for data-driven components.
7. Run the narrowest test command (`npm test -- OrderForm`) before full suite.

## References

- Read `references/vue-test-utils-patterns.md` for mount, stubs, async, and query strategies.
- Read `references/msw-and-composables.md` for API mocking and composable testing.

## Checklist

- Tests describe scenario and expected outcome in the name.
- No snapshot-only tests for complex UI without supplementary assertions.
- Tests do not depend on execution order.
- Fake timers only when testing debounce/time — restored after.
- Accessibility roles used in queries where possible.
- CI uses `jsdom` or happy-dom consistently with local dev.

## Output

Summarize behaviors tested, query strategies used, mocks added, commands to run, and intentional gaps.
