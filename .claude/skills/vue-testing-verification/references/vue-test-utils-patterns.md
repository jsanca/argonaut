# Vue Test Utils Patterns

## Mount With Plugins

```typescript
import { mount, flushPromises } from "@vue/test-utils";
import { createPinia } from "pinia";
import OrderPage from "@/features/orders/OrderPage.vue";

it("shows order summary when load succeeds", async () => {
  const wrapper = mount(OrderPage, {
    props: { id: "ord-1" },
    global: {
      plugins: [createPinia()],
      stubs: { RouterLink: true },
    },
  });

  await flushPromises();

  expect(wrapper.getByRole("heading", { name: /order ord-1/i })).toBeTruthy();
});
```

Use `@testing-library/vue` `render` + `screen` when the repo standardizes on Testing Library queries.

## User Interactions

```typescript
import userEvent from "@testing-library/user-event";

await userEvent.click(wrapper.getByRole("button", { name: /save/i }));
await flushPromises();
expect(wrapper.getByRole("alert")).toHaveTextContent(/saved/i);
```

## Testing Emits

```typescript
await wrapper.get("button").trigger("click");
expect(wrapper.emitted("submit")).toBeTruthy();
expect(wrapper.emitted("submit")![0]).toEqual([{ email: "a@b.com" }]);
```

Prefer asserting visible outcome over emit-only when both reflect user behavior.

## Async Components

```typescript
import { defineAsyncComponent } from "vue";

const AsyncChild = defineAsyncComponent(() => import("./HeavyChild.vue"));
// mount with global.stubs or wait for Suspense + flushPromises
```

Stub async children in unit tests unless integration test scope requires real chunk.
