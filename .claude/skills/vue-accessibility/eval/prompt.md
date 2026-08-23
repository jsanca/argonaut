# Eval: Vue Accessibility

## Prompt

Review a custom dropdown built with divs and mouse events only in a Vue 3 SFC. Rewrite or fix so it is keyboard accessible and screen-reader friendly.

## Expected Agent Behavior

- Identifies missing roles, keyboard handlers, focus management
- Recommends native select or headless component matching project
- Adds aria-expanded, listbox/option pattern or button+menu semantics
- Documents keyboard interactions (Arrow, Enter, Escape, Tab)
- Mentions focus return on close and Vue ref/`nextTick` usage where DOM updates matter

## Failure Signals

- Adds aria-label only without keyboard support
- Keeps div-only implementation with tabindex hacks only
- Ignores Escape and focus trap
- Uses v-html for dynamic menu labels without sanitization
