# Upgrade Sequencing

## Recommended Order

1. **Toolchain** — Node LTS, package manager, Vite/webpack baseline.
2. **Vue core** — `vue`, `@vue/compiler-sfc`, `@vitejs/plugin-vue`.
3. **Router and state** — `vue-router`, `pinia` (or finish Vuex compatibility layer).
4. **UI libraries** — Element Plus, Vuetify 3, PrimeVue, etc. per compatibility matrix.
5. **Test stack** — Vitest, `@vue/test-utils`, `@vue/vue3-jest` if applicable.
6. **Application code** — Options API hotspots, filters, `$listeners`, event bus removals.

## Dependency Alignment

```json
{
  "vue": "^3.5.0",
  "vue-router": "^4.4.0",
  "pinia": "^2.2.0",
  "@vitejs/plugin-vue": "^5.1.0",
  "typescript": "^5.5.0",
  "vue-tsc": "^2.1.0"
}
```

Pin plugin-vue major to Vite major per release notes — verify before bumping.

## Incremental Vue 3 With Compat

When migrating large Vue 2 codebases:

1. Enable `@vue/compat` with migration config flags per feature.
2. Fix compat warnings category by category (GLOBAL_MOUNT, FILTERS, etc.).
3. Remove compat mode only when warning count is zero in CI.

Document compat flags in `vite.config` and track remaining warnings in migration ticket.

## CI During Migration

- Keep `npm run build`, `npm run test`, and `vue-tsc --noEmit` green on migration branch.
- Add temporary lint rules as warnings, not disabled — tighten each PR.
