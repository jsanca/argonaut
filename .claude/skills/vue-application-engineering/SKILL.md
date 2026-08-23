---
name: vue-application-engineering
description: Build and review Vue 3 applications with script setup SFCs, composables, Vue Router, forms, and maintainable feature boundaries. Use when implementing features, refactoring Vue structure, or aligning with modern Vue patterns in existing apps.
---

# Vue Application Engineering

## Workflow

1. Inspect Vue version, build tool (Vite), UI library, Pinia usage, routing style, and existing SFC conventions.
2. Identify feature boundaries: page/container vs presentational components, composables, and API clients.
3. Prefer `<script setup lang="ts">` with explicit props/emits and typed composables.
4. Keep templates readable — extract subcomponents instead of large template blocks.
5. Use form libraries or typed patterns consistent with the project (VeeValidate, native v-model).
6. Route lazily for feature areas; guard auth routes — enforce authorization server-side too.
7. Verify with component tests and `npm test` / `vitest` scoped to changed files.

## References

- Read `references/components-and-composables.md` for SFC structure, props/emits, and smart/dumb split.
- Read `references/routing-and-forms.md` for lazy routes, guards, and form patterns.

## Checklist

- Components have single clear responsibility.
- Composables encapsulate reusable logic — not every line in setup.
- API types explicit; avoid `any` at boundaries.
- Error and loading states handled in UI.
- Public API of feature folders exported via barrel `index.ts` when repo uses them.
- Side effects cleaned up in `onUnmounted` or composable teardown.

## Output

Summarize structure changes, components/composables added, routing impact, tests run, and deferred refactors.
