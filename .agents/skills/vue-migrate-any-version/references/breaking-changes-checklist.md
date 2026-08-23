# Breaking Changes Checklist

## Vue 2 → 3 Highlights

| Vue 2 | Vue 3 |
| --- | --- |
| `new Vue()` | `createApp()` |
| Global API (`Vue.use`) | App instance `app.use()` |
| Filters | Computed methods or pipes in template |
| `$listeners` | Merged into `$attrs` |
| `v-model` on components | `modelValue` + `update:modelValue` |
| Functional components | Plain functions or `<script setup>` |
| Key on `<template v-for>` | Key on child element |

## Vue Router 3 → 4

- `new Router()` → `createRouter({ history: createWebHistory() })`
- `router.addRoutes` → add routes at config time or dynamic `addRoute`
- `*` catch-all → `/:pathMatch(.*)*`
- `mode: 'history'` → `createWebHistory()`

## Vuex → Pinia

- Modules → separate stores or composition-style stores
- `mapState` → `storeToRefs` + destructuring
- Mutations removed — actions may mutate state directly (Pinia style)

## Test Utils v1 → v2

- `mount` async behavior changes — use `flushPromises` where needed
- Global plugins via `global.plugins` in mount options
- Stubs/slots API updates — read migration guide for component library tests

## Template and TypeScript

- Enable `vue-tsc` strict templates after core migration.
- Replace `this.$refs` patterns with `ref()` and template refs in Composition API.
