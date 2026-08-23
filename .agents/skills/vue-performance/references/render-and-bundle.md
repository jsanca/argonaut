# Render and Bundle

## Profile With Vue DevTools

1. Open Performance tab — record interaction (filter change, tab switch).
2. Inspect component render count and time — target hot paths first.
3. Check whether parent re-renders cascade to heavy children unnecessarily.

## Lazy Route and Async Component

```typescript
const HeavyChart = defineAsyncComponent(() => import("./HeavyChart.vue"));

const routes = [
  {
    path: "/reports",
    component: () => import("@/features/reports/ReportsPage.vue"),
  },
];
```

## Reduce Reactive Overhead

```typescript
import { shallowRef, markRaw } from "vue";

// Large API payload not fully edited in UI
const dataset = shallowRef<ChartData | null>(null);
dataset.value = markRaw(await fetchChartData());
```

Use `ref` for primitives and objects that need deep reactivity; `shallowRef` for large readonly blobs.

## Bundle Analysis

```bash
npm run build -- --report
# or vite-plugin-visualizer in vite.config
```

Split vendor chunks intentionally — avoid duplicating Vue in multiple async chunks via misconfigured manualChunks.

## v-memo (Targeted)

```vue
<Row v-for="item in rows" :key="item.id" v-memo="[item.id, item.status]" />
```

Apply only when profiling shows list row re-render cost dominates.
