---
name: ux-design-review
description: Review user experience for flows, information architecture, heuristics, content clarity, and loading/empty/error states. Use when evaluating wireframes, PRs that change navigation, onboarding, forms, dashboards, or when users report confusion or drop-off.
---

# UX Design Review

## Workflow

1. Clarify the user goal, primary persona, and success criteria for the flow under review.
2. Map the happy path and top failure paths — identify steps, decisions, and system feedback at each point.
3. Apply heuristic review (visibility of status, match to real-world conventions, error prevention/recovery, consistency).
4. Check information architecture — labels, hierarchy depth, and whether users know where they are.
5. Review content and microcopy — button labels, empty states, errors (actionable, no blame).
6. Verify explicit handling of loading, empty, error, and disabled states on every meaningful screen.
7. Note accessibility dependencies — keyboard path and focus order should support the UX, not fight it.
8. Prioritize findings by user impact; separate quick wins from structural IA changes.

## References

- Read `references/heuristics-and-flows.md` for Nielsen heuristics, flow mapping, and review prompts.
- Read `references/states-and-content.md` for loading/empty/error patterns and microcopy guidelines.

## Review Checklist

- User can answer "what can I do here?" within five seconds on key screens.
- Primary action is obvious; destructive actions are visually and procedurally distinct.
- Errors explain what failed and how to recover — not generic "Something went wrong."
- Empty states guide the next action — not blank panels.
- Long flows show progress or save partial work where abandonment is likely.
- Terminology matches user mental model — not internal system names.
- Mobile and keyboard paths are considered, not desktop-only assumptions.

## Output

Deliver a UX review with:

- **Scope** — screens/flows reviewed and user goal
- **Findings** — severity (blocker / major / minor), heuristic violated, user impact
- **Recommendations** — specific copy, layout, or flow changes
- **States** — gaps in loading, empty, error, disabled
- **Follow-up** — what to validate with users or analytics
