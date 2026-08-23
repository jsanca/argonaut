# Routing and Forms

## Lazy Route

```typescript
import { createRouter, createWebHistory } from "vue-router";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/orders",
      component: () => import("@/features/orders/OrdersLayout.vue"),
      children: [
        {
          path: ":id",
          name: "order-detail",
          component: () => import("@/features/orders/OrderPage.vue"),
          props: true,
        },
      ],
    },
  ],
});
```

## Navigation Guard

```typescript
router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
});
```

Client guards improve UX only — APIs must enforce authorization independently.

## Typed Form With v-model

```vue
<script setup lang="ts">
import { reactive } from "vue";

const form = reactive({ email: "", quantity: 1 });
const errors = reactive<{ email?: string }>({});

function submit(): void {
  errors.email = form.email.includes("@") ? undefined : "Enter a valid email.";
  if (errors.email) return;
  // submit
}
</script>

<template>
  <form @submit.prevent="submit">
    <label for="email">Email</label>
    <input id="email" v-model="form.email" type="email" autocomplete="email" />
    <p v-if="errors.email" role="alert">{{ errors.email }}</p>
    <button type="submit">Save</button>
  </form>
</template>
```

When the repo uses VeeValidate or similar, follow its schema and field components instead of ad-hoc validation.

## Route Props

Prefer `props: true` or a `props` function on routes to keep page components testable without mocking the full router.
