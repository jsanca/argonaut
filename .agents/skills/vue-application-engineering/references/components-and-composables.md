# Components and Composables

## Presentational SFC

```vue
<script setup lang="ts">
defineProps<{
  order: OrderSummary;
}>();

const emit = defineEmits<{
  viewDetails: [id: string];
}>();
</script>

<template>
  <article class="order-summary">
    <h2>Order {{ order.id }}</h2>
    <p>{{ order.status }}</p>
    <p class="total">{{ order.totalFormatted }}</p>
    <button type="button" @click="emit('viewDetails', order.id)">View details</button>
  </article>
</template>
```

## Smart Page With Composable

```vue
<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { useOrder } from "@/features/orders/useOrder";
import OrderSummary from "./OrderSummary.vue";

const route = useRoute();
const router = useRouter();
const { order, loading, error } = useOrder(() => route.params.id as string);

function onView(id: string): void {
  void router.push({ name: "order-details", params: { id } });
}
</script>

<template>
  <LoadingState v-if="loading" />
  <ErrorState v-else-if="error" :message="error" />
  <OrderSummary v-else-if="order" :order="order" @view-details="onView" />
</template>
```

## Feature Composable

```typescript
import { ref, watchEffect } from "vue";
import { fetchOrder } from "@/api/orders";

export function useOrder(idSource: () => string) {
  const order = ref<OrderSummary | null>(null);
  const loading = ref(true);
  const error = ref<string | null>(null);

  watchEffect(async (onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });
    loading.value = true;
    error.value = null;
    try {
      order.value = await fetchOrder(idSource());
    } catch {
      if (!cancelled) error.value = "Unable to load order.";
    } finally {
      if (!cancelled) loading.value = false;
    }
  });

  return { order, loading, error };
}
```

Match existing project patterns for Pinia vs composable-local state — do not mix without reason.
