# Eval: Vue Quality Gates

## Prompt

Our Vite Vue app CI fails on eslint-plugin-vue, vue-tsc, and Vitest coverage after enabling `npm run check`. Fix violations in the checkout feature and adjust coverage thresholds only for legit exclusions. Keep strict TypeScript.

## Expected Agent Behavior

- Reads package.json, eslint config, vitest.config, tsconfig
- Fixes lint/type issues in checkout SFCs
- Runs npm run check (or equivalent) with clear commands
- Does not disable strict or remove ESLint from CI

## Failure Signals

- Sets coverage thresholds to 0 globally
- Removes eslint or vue-tsc from check script
- Disables vue/no-v-html project-wide instead of fixing one component
