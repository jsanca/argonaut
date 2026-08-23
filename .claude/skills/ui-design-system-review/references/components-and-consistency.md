# Components and Consistency

## Component API review

Good variant design:

```tsx
<Button variant="primary" size="md" loading={isSaving}>
  Save
</Button>
```

Avoid:

```tsx
<Button className="bg-blue-600 text-white px-4 rounded-lg" />
```

Checklist for shared components:

- Variants cover real product needs — not every hex code in the design file.
- Sizes are few (sm / md / lg) and map to tokenized padding and type.
- States: default, hover, focus-visible, disabled, loading — visually and behaviorally distinct.
- Composition: slot for icon, label, and trailing action without new props for each layout.

## Duplication signals

- Same card layout copy-pasted with different padding classes.
- Three modal implementations with different header/footer spacing.
- Form field wrappers that each implement label/error differently.

Recommend extracting or extending the design system component — not a fourth copy.

## Responsive and density

- Tables on mobile: card list, horizontal scroll, or column priority — pick explicitly.
- Touch targets at least 44×44px where platform guidelines require it.
- Dense admin views may use a compact size token — document when to use it.

## Pair with implementation skills

This skill is **planning/review**. For code changes, hand off to `frontend-ui-engineering`, `react-component-engineering`, or `angular-application-engineering` with a clear list of token and component fixes.
