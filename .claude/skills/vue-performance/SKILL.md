---
name: vue-performance
description: Diagnose and improve Vue application performance including render cost, reactivity granularity, list virtualization, code splitting, and bundle size. Use when fixing slow interactions, unnecessary re-renders, large bundles, or runtime performance regressions in Vue 3 apps.
---

# Vue Performance

## Workflow

1. Measure first: Vue DevTools performance, browser Performance tab, Lighthouse, or bundle analyzer — do not optimize blindly.
2. Identify bottleneck type: render churn, large lists, expensive watchers, network waterfall, or bundle parse cost.
3. Fix highest-impact issues: unstable keys, broad reactive objects, unvirtualized heavy lists, missing route lazy loading.
4. Apply targeted `computed`, `shallowRef`, `v-memo`, and `defineAsyncComponent` only where profiling shows benefit.
5. Lazy-load routes and heavy components; prefetch critical paths when appropriate.
6. Re-measure after each change; avoid premature micro-optimizations.
7. Document tradeoffs when readability regresses for measurable gain.

## References

- Read `references/render-and-bundle.md` for DevTools profiling, splitting, and lazy routes.
- Read `references/lists-and-interactions.md` for virtualization, debouncing, and interaction latency.

## Checklist

- DevTools confirms which components update excessively.
- Large lists virtualized or paginated.
- Route-level code splitting for infrequent views.
- Images and fonts optimized; third-party scripts deferred.
- `shallowRef` / `markRaw` used for large non-reactive payloads when appropriate.
- No blanket `v-memo` on every leaf without measurement.

## Output

Summarize baseline metric, root cause, changes made, after metric, and verification steps.
