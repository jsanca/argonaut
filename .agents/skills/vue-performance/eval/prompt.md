# Eval: Vue Performance

## Prompt

A Vue 3 data table with 5k rows re-renders on every parent state tick and scrolls poorly. Diagnose and apply targeted fixes without blanket v-memo everywhere.

## Expected Agent Behavior

- Recommends measurement first (DevTools, Lighthouse)
- Identifies unstable keys, missing virtualization, or broad reactive state
- Proposes virtualization or pagination plus shallowRef for readonly data if needed
- Lazy-loads heavy child components if applicable
- Re-measure after changes

## Failure Signals

- Adds v-memo to every cell without profiling
- Converts entire app to Pinia for local list state
- Ignores key stability or parent-driven re-render cascade
