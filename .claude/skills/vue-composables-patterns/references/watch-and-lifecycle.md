# Watch and Lifecycle

## watch vs watchEffect

```typescript
// watch — explicit source, control flush/immediate
watch(
  () => route.query.q,
  (query) => search(query as string),
  { flush: "post" },
);

// watchEffect — auto tracks deps; use when many deps or DOM sync
watchEffect(() => {
  document.title = pageTitle.value;
});
```

Use `watch` for IO tied to specific inputs; avoid `watchEffect` for async fetch without cleanup.

## Cleanup Patterns

```typescript
function useWindowWidth() {
  const width = ref(window.innerWidth);

  function onResize(): void {
    width.value = window.innerWidth;
  }

  onMounted(() => window.addEventListener("resize", onResize));
  onUnmounted(() => window.removeEventListener("resize", onResize));

  return { width };
}
```

For composables called outside setup, use `effectScope` or document that they require active component scope.

## Debounced Watch

```typescript
import { watchDebounced } from "@vueuse/core";

watchDebounced(
  searchTerm,
  (term) => runSearch(term),
  { debounce: 300, maxWait: 1000 },
);
```

Match project utility — VueUse vs custom debounce.

## onScopeDispose

```typescript
export function usePolling(fetcher: () => Promise<void>, intervalMs: number) {
  const timer = setInterval(() => void fetcher(), intervalMs);
  onScopeDispose(() => clearInterval(timer));
}
```

Ensures timers clear when composable scope ends in tests or nested scopes.
