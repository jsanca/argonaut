# Eval: UI Design System Review

## Prompt

Our React + Tailwind app has grown to 40 screens. Designers complain buttons look different on every page. Engineers grep for `bg-indigo-600`, `bg-primary`, and `btn-primary` interchangeably. There is a `tailwind.config.ts` with extended colors but no one documents which to use.

Review the design system approach and recommend a consolidation plan before we add a new admin console.

## Expected behavior

- Agent asks to inspect token sources (Tailwind config, CSS variables, shared Button component).
- Agent identifies semantic token gap and inconsistent naming — recommends role-based tokens.
- Agent proposes component variant API and migration order (tokens → Button/Input → pages).
- Agent flags arbitrary values and duplicate patterns as drift — does not rewrite entire app in one pass.
- Output stays at planning/review level with actionable rollout steps.
