# Pinia and URL State

## Feature Store

```typescript
import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useCartStore = defineStore("cart", () => {
  const items = ref<CartItem[]>([]);
  const total = computed(() => items.value.reduce((sum, i) => sum + i.price, 0));

  function add(item: CartItem): void {
    items.value.push(item);
  }

  function clear(): void {
    items.value = [];
  }

  return { items, total, add, clear };
});
```

## storeToRefs in Components

```vue
<script setup lang="ts">
import { storeToRefs } from "pinia";
import { useCartStore } from "@/stores/cart";

const cart = useCartStore();
const { items, total } = storeToRefs(cart);
</script>
```

Destructure actions directly from store — only state/getters need `storeToRefs`.

## Sync Filters to URL

```typescript
import { watch } from "vue";
import { useRoute, useRouter } from "vue-router";

export function useFilterQuery() {
  const route = useRoute();
  const router = useRouter();
  const status = ref((route.query.status as string) ?? "all");

  watch(status, (value) => {
    void router.replace({ query: { ...route.query, status: value === "all" ? undefined : value } });
  });

  return { status };
}
```

URL is shareable source of truth for filters — Pinia optional for mirror/cache only.

## Hydration Notes (SSR)

When using Nuxt or SSR, ensure Pinia state serialized on server matches client hydration — avoid random IDs or Date objects without normalization in shared state.
