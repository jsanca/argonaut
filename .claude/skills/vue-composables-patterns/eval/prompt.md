# Eval: Vue Composables Patterns

## Prompt

A Vue page refetches user data on every keystroke in a search box and leaks listeners on unmount. Refactor using composables with proper watch debouncing and cleanup.

## Expected Agent Behavior

- Extracts useSearch or similar with debounced watch
- Uses watch cleanup or abort for stale requests
- Removes listeners/intervals on unmount or onScopeDispose
- Explains watch vs watchEffect choice

## Failure Signals

- Keeps refetch on every input event without debounce
- watchEffect async without stale guard
- Duplicates fetch logic in component and composable
