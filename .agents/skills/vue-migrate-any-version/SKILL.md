---
name: vue-migrate-any-version
description: Plan and execute Vue and ecosystem migrations across major versions, Vue Router, Pinia, Vite, and testing libraries. Use when upgrading Vue 2 to 3, migrating Options API to Composition API, updating Vue Router 4+, or modernizing TypeScript and ESLint configs tied to Vue upgrades.
---

# Vue Migrate Any Version

## Workflow

1. Inventory current versions: Vue, Vue Router, Pinia/Vuex, test stack, Vite/webpack, and TypeScript.
2. Read official migration guides for target versions — note breaking changes and codemods.
3. Upgrade in layers: tooling (Node, Vite) → Vue core → router/store → app code fixes.
4. Run codemods where available (`@vue/compat`, eslint-plugin-vue fixes); fix type errors incrementally.
5. Run test suite and key E2E flows after each layer; do not batch all fixes blindly.
6. Update CI and lockfile together; document peer dependency resolutions.
7. Ship behind flag or branch if migration spans multiple PRs.

## References

- Read `references/upgrade-sequencing.md` for order of operations and dependency alignment.
- Read `references/breaking-changes-checklist.md` for Vue 2→3, Router, and Pinia migration items.

## Checklist

- Vue and `@vue/compiler-sfc` versions aligned with runtime.
- `@vue/compat` strategy documented if incremental Vue 3 migration.
- Global API → createApp; no `new Vue()` in Vue 3 apps.
- Vue Router 4+ patterns (`createRouter`, `createWebHistory`) applied consistently.
- Vuex → Pinia migration plan when applicable.
- Tests updated for async setup and vue-test-utils v2 API.
- Build, lint, and `vue-tsc` pass in CI with frozen lockfile.

## Output

Summarize version delta, upgrade steps executed, breaking fixes, verification commands, and deferred follow-ups.
