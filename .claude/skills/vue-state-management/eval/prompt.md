# Eval: Vue State Management

## Prompt

A Vue app duplicates order list data in Pinia and a fetch composable, and filter state lives only in a parent component. Propose state ownership and refactor plan.

## Expected Agent Behavior

- Classifies server vs client vs URL state
- Recommends single source for orders (query lib or one store/composable)
- Moves shareable filters to route query or dedicated composable
- Uses storeToRefs pattern where Pinia remains
- Mentions tests for filter URL sync

## Failure Signals

- Adds second Pinia store without removing duplicate
- Puts all data in global store by default
- Ignores URL shareability for filters
