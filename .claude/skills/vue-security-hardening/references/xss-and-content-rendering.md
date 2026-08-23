# XSS and Content Rendering

## Avoid Raw v-html

```vue
<!-- Unsafe -->
<div v-html="userComment" />

<!-- Safer -->
<p>{{ userComment }}</p>

<!-- If HTML required -->
<div v-html="sanitizedComment" />
```

```typescript
import DOMPurify from "dompurify";
import { computed } from "vue";

const sanitizedComment = computed(() => DOMPurify.sanitize(props.userComment));
```

## Dynamic URLs

```vue
<a :href="safeUrl" rel="noopener noreferrer">Open</a>
```

```typescript
function toSafeHttpUrl(raw: string): string | null {
  try {
    const url = new URL(raw);
    if (url.protocol === "http:" || url.protocol === "https:") return url.href;
  } catch {
    return null;
  }
  return null;
}
```

Block `javascript:` and data URLs in user-supplied links.

## Template Injection Awareness

Do not compile user strings as Vue templates or evaluate user code. Dynamic components must use allowlisted component maps:

```typescript
const ALLOWED = { UserCard, OrderRow } as const;
const resolved = computed(() => ALLOWED[props.name as keyof typeof ALLOWED]);
```

## CSP-Friendly Scripts

Avoid inline scripts in `index.html` when CSP uses nonces — load config via JSON endpoint or build-time env only for non-secret values.
