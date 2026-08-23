# Lists and Interactions

## Virtualize Long Lists

Use `@tanstack/vue-virtual`, `vue-virtual-scroller`, or table pagination — match project library.

```vue
<script setup lang="ts">
import { useVirtualizer } from "@tanstack/vue-virtual";
import { ref, computed } from "vue";

const parentRef = ref<HTMLElement | null>(null);
const items = ref(Array.from({ length: 10_000 }, (_, i) => ({ id: i, label: `Row ${i}` })));

const virtualizer = useVirtualizer(
  computed(() => ({
    count: items.value.length,
    getScrollElement: () => parentRef.value,
    estimateSize: () => 40,
  })),
);
</script>
```

## Debounce Expensive Handlers

```typescript
import { watchDebounced } from "@vueuse/core";

watchDebounced(filters, applyFilters, { debounce: 250 });
```

Prefer debouncing at composable boundary — not duplicate `@input` handlers in every field.

## Stable Keys

```vue
<!-- Wrong for mutable lists -->
<div v-for="(row, index) in rows" :key="index">

<!-- Right -->
<div v-for="row in rows" :key="row.id">
```

## Interaction Latency

- Move heavy work off main thread (Web Worker) when parsing large CSV/JSON client-side.
- Show immediate UI feedback (`aria-busy`, skeleton) before async completes.
- Avoid synchronous JSON.parse on megabyte payloads in watchers — parse once and shallowRef.
