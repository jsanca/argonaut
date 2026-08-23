# Eval: Vue Migrate Any Version

## Prompt

Plan and start migrating a Vue 2 + Vuex + webpack app to Vue 3 + Pinia + Vite. List upgrade order, breaking changes to address first, and verification commands.

## Expected Agent Behavior

- Inventories versions and ecosystem packages
- Proposes layered upgrade (tooling → core → router/store → app)
- Mentions @vue/compat or incremental strategy for large apps
- Lists Router 4 and Pinia migration items
- Includes vue-tsc, build, and test commands

## Failure Signals

- Jumps straight to rewrite without sequencing
- Ignores Vuex → Pinia or test stack updates
- Suggests staying on Vue 2 without user request
