# Eval: Vue Application Engineering

## Prompt

Refactor a large Vue 3 order page SFC that mixes fetch logic, routing, and presentation. Split into presentational component, composable, and lazy route while preserving behavior.

## Expected Agent Behavior

- Uses script setup with typed props/emits
- Extracts useOrder (or equivalent) composable with cleanup
- Lazy-loads route component
- Handles loading/error states explicitly
- Mentions tests to run

## Failure Signals

- Leaves fetch logic in template or giant setup block
- Uses any for API types
- Removes error handling during refactor
- Adds Pinia store for single-page local fetch state without justification
