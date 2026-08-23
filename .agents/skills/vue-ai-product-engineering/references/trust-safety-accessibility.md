# Trust, Safety, and Accessibility

## Sanitized Markdown Rendering

Never bind raw model output with `v-html` without sanitization:

```vue
<script setup lang="ts">
import DOMPurify from "dompurify";
import { computed } from "vue";
import { renderMarkdown } from "@/lib/markdown";

const props = defineProps<{ content: string }>();
const safeHtml = computed(() =>
  DOMPurify.sanitize(renderMarkdown(props.content)),
);
</script>

<template>
  <div class="prose" v-html="safeHtml" />
</template>
```

Prefer rendering to plain text or a restricted markdown subset when product allows.

## Uncertainty and Guardrails

- Show when answer is grounded in retrieved docs vs general knowledge.
- Surface "I don't know" instead of hallucinating product facts.
- Disable send for disallowed topics per product policy — explain why briefly.

## Keyboard and Focus

- Textarea: `Enter` submits, `Shift+Enter` newline (document in UI hint).
- After send, focus stays in input or moves to new message region predictably.
- Stop/cancel button is keyboard reachable and labeled.

## Error Copy

User-facing messages must be actionable — not stack traces:

```typescript
const MESSAGES = {
  rateLimit: "Too many requests. Wait a moment and try again.",
  network: "Connection lost. Check your network and retry.",
  session: "Session expired. Sign in again to continue.",
} as const;
```

## Secrets

- API keys stay server-side; browser calls your BFF/proxy.
- `import.meta.env.VITE_*` values are public — never store private keys there.
