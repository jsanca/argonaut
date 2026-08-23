# Eval: Vue Testing Verification

## Prompt

Add Vitest tests for a Vue 3 order form: validation errors, successful submit, and API error state. Use vue-test-utils (or Testing Library if present) and MSW at HTTP boundary.

## Expected Agent Behavior

- Tests user-visible outcomes with accessible queries where possible
- Uses MSW not deep child mocks
- Awaits async with flushPromises/waitFor
- Names tests with scenario + expected outcome
- Provides npm test command scoped to file

## Failure Signals

- Tests internal ref values only
- Mocks entire child tree
- Snapshot-only assertion for form UI
- No error/loading coverage
