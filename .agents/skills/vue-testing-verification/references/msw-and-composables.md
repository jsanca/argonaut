# MSW and Composables

## MSW Handler

```typescript
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

const server = setupServer(
  http.get("/api/v1/orders/:id", ({ params }) =>
    HttpResponse.json({ id: params.id, status: "SHIPPED" }),
  ),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

Register MSW in Vitest setup file — same handlers in CI and local.

## Composable Test Harness

```typescript
import { effectScope } from "vue";
import { useOrder } from "./useOrder";

it("loads order for id", async () => {
  const scope = effectScope();
  await scope.run(async () => {
    const { order, loading } = useOrder(() => "ord-1");
    await flushPromises();
    expect(loading.value).toBe(false);
    expect(order.value?.status).toBe("SHIPPED");
  });
  scope.stop();
});
```

Or mount a minimal host component that calls the composable in `setup`.

## Error Path

```typescript
server.use(
  http.get("/api/v1/orders/:id", () => HttpResponse.json({ message: "nope" }, { status: 500 })),
);

await flushPromises();
expect(wrapper.getByRole("alert")).toHaveTextContent(/unable to load/i);
```

Cover loading, success, error, and empty states — not happy path only.
