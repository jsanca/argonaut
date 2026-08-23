---
name: vue-accessibility
description: Implement and review accessible Vue UIs with semantic HTML, keyboard support, ARIA when necessary, focus management, and WCAG-oriented patterns. Use when building forms, modals, menus, data tables, or fixing accessibility audit findings in Vue 3 applications.
---

# Vue Accessibility

## Workflow

1. Inspect design system components for built-in a11y; prefer native semantics over custom ARIA widgets.
2. Verify keyboard path: Tab order, Enter/Space activation, Escape to close overlays, arrow keys in composite widgets.
3. Associate labels with inputs; group related fields; announce errors accessibly.
4. Manage focus on route changes, modal open/close, and destructive actions — use Vue refs and `nextTick` when DOM updates.
5. Check color contrast, motion preferences (`prefers-reduced-motion`), and touch targets on mobile.
6. Test with keyboard only and screen reader spot-check on critical flows.
7. Fix issues at the component pattern level so future screens inherit correctness.

## References

- Read `references/forms-and-semantics.md` for labels, errors, landmarks, and headings in Vue templates.
- Read `references/overlays-and-keyboard.md` for modals, menus, focus trap, and live regions.

## Checklist

- Interactive controls are `button` or `a` — not unlabeled `div`s with `@click` only.
- Icons have text alternative or `aria-label` when standalone.
- Modals trap focus and restore on close.
- Dynamic updates use `aria-live` appropriately (not overly verbose on every reactive tick).
- Tables use `th` scope/headers for data grids when semantic table fits.
- Skip link or landmark navigation available on app shell pages.
- `v-html` avoided on untrusted content; prefer sanitized or plain text.

## Output

Summarize issues found, WCAG-oriented fixes, components updated, and manual verification steps (keyboard + screen reader).
