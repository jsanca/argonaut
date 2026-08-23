# Tokens and Scale

## Semantic color roles

Prefer role-based tokens over palette names in feature code:

| Role | Typical use |
| --- | --- |
| `background` / `foreground` | Page and text base |
| `primary` / `primary-foreground` | Main brand actions |
| `secondary` | Secondary actions, chips |
| `muted` / `muted-foreground` | Subtle backgrounds, helper text |
| `accent` | Highlights, selected states |
| `destructive` | Delete, irreversible actions |
| `border` / `input` / `ring` | Borders, fields, focus rings |

## Spacing and typography

- Use a single spacing scale (e.g. 4px base: 1, 2, 3, 4, 6, 8, 12, 16 in Tailwind terms).
- Limit font sizes to a defined ramp; pair size with line-height and weight intentionally.
- Cap line length for body copy (~60–75 characters) on large screens.

## Tailwind-oriented audit

Search for drift signals:

- Arbitrary values: `[13px]`, `[#abc123]`, `gap-[18px]`
- Repeated identical utility strings across files (candidate for component or `@apply` sparingly)
- `text-gray-*` / `bg-blue-*` in feature code instead of semantic tokens
- Missing responsive prefixes where layout clearly breaks on mobile

## Document token location

Before recommending new tokens, note where the project already defines them:

- `tailwind.config.*` / `@theme` in CSS
- `design/tokens/*` or `styles/variables.css`
- Component library theme (Ant Design, MUI, shadcn CSS variables)

Extend existing files — do not invent parallel token files without team agreement.
