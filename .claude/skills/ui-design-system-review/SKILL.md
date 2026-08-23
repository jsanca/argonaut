---
name: ui-design-system-review
description: Review design system consistency — tokens, spacing, typography, color roles, elevation, and component APIs. Use when auditing Tailwind or component libraries, planning a design system, reviewing UI PRs for visual drift, or consolidating one-off styles.
---

# UI Design System Review

## Workflow

1. Identify design sources — token files, Tailwind config, CSS variables, Figma tokens, or component library theme.
2. Inventory core primitives: color roles (not raw hex), spacing scale, typography scale, radii, shadows, breakpoints.
3. Compare implementation to intended tokens — find magic numbers, one-off colors, and duplicated utility blocks.
4. Review component boundaries — variants, sizes, states, and composition vs prop explosion.
5. Check responsive behavior — mobile-first breakpoints, touch targets, readable line lengths.
6. Assess visual hierarchy — primary/secondary/tertiary actions, heading levels, metadata density.
7. Align with accessibility — contrast pairs, focus styles, motion preferences (pair with accessibility skills when implementing).
8. Recommend token additions or deprecations — smallest set that removes drift.

## References

- Read `references/tokens-and-scale.md` for color roles, spacing, type, and Tailwind-oriented token patterns.
- Read `references/components-and-consistency.md` for variant APIs, duplication, and review checklist.

## Review Checklist

- Semantic color roles (`primary`, `muted`, `destructive`) — not hardcoded palette names in features.
- Spacing uses the shared scale — no arbitrary `13px` or one-off margins.
- Typography uses defined steps — headings, body, caption have named styles.
- Interactive components share focus ring, hover, and disabled treatments.
- Cards, forms, and tables reuse the same padding and border language.
- Dark mode (if supported) uses token pairs, not inverted one-offs.
- Component props express intent (`variant="destructive"`) not presentation (`className="red-button"`).

## Output

Deliver a design system review with:

- **Sources reviewed** — config files, key components, sample screens
- **Drift findings** — magic values, duplicate patterns, inconsistent variants
- **Token recommendations** — add, merge, or deprecate
- **Component gaps** — missing states or variants blocking consistency
- **Rollout** — safe migration order (tokens first, then primitives, then pages)
