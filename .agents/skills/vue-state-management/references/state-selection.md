# State Selection

## Decision Guide

| State type | Prefer |
| --- | --- |
| Toggle, hover, local input | `ref` in component or composable |
| Shared between siblings | Lift to parent or composable |
| Cross-route client UI | Pinia store |
| Filters, pagination, tabs (shareable) | Vue Router `query` / `params` |
| Remote API data | TanStack Query, fetch composable, or Pinia + explicit fetch |
| Auth session snapshot | Pinia + server cookie/Bearer strategy |

## Local Composable State

```typescript
export function useCheckoutDraft() {
  const items = ref<CartItem[]>([]);
  const note = ref("");

  function addItem(item: CartItem): void {
    items.value.push(item);
  }

  return { items, note, addItem };
}
```

## When Not to Use Pinia

- Single page uses the data
- Data is server-owned and cacheable — use query library
- Ephemeral wizard step valid only until submit

## Server State With Query Library

```typescript
import { useQuery } from "@tanstack/vue-query";

export function useOrders() {
  return useQuery({
    queryKey: ["orders"],
    queryFn: fetchOrders,
  });
}
```

Keeps loading/error/refetch semantics out of hand-rolled stores.
