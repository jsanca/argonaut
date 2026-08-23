# Auth and Storage

## Token Storage Tradeoffs

| Approach | XSS risk | CSRF risk | Notes |
| --- | --- | --- | --- |
| HttpOnly cookie | Lower token theft | Needs CSRF token/header | Preferred for session |
| memory / Pinia | Cleared on refresh | N/A for Bearer header | OK for short-lived access |
| localStorage | High if XSS | N/A for Bearer | Avoid for refresh tokens |

## Axios/Fetch With CSRF Cookie

```typescript
import axios from "axios";

const api = axios.create({ withCredentials: true });

api.interceptors.request.use((config) => {
  const csrf = getCookie("XSRF-TOKEN");
  if (csrf) config.headers["X-XSRF-TOKEN"] = csrf;
  return config;
});
```

Align header name with backend framework (Spring, Laravel, etc.).

## Router Guard vs Server Auth

```typescript
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: "login" };
  }
});
```

Guards improve UX — APIs must return 401/403 for unauthorized resource access regardless.

## Vite Env Variables

```typescript
// Public — bundled into client
const apiBase = import.meta.env.VITE_API_BASE_URL;

// Never put private keys in VITE_ prefix
```

Secrets belong on server/BFF only.

## Logout Hygiene

```typescript
async function logout(): Promise<void> {
  await api.post("/auth/logout");
  authStore.$reset();
  await router.push({ name: "login" });
}
```

Clear Pinia stores and in-memory caches holding user data.
