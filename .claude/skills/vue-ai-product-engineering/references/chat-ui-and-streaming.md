# Chat UI and Streaming

## Streaming Composable

```typescript
import { ref, shallowRef, onUnmounted } from "vue";

export function useChatStream() {
  const text = ref("");
  const status = ref<"idle" | "streaming" | "done" | "error">("idle");
  const error = shallowRef<string | null>(null);
  let controller: AbortController | null = null;

  async function send(prompt: string): Promise<void> {
    controller?.abort();
    controller = new AbortController();
    text.value = "";
    status.value = "streaming";
    error.value = null;

    try {
      const response = await fetch("/api/chat/stream", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt }),
        signal: controller.signal,
      });
      if (!response.ok || !response.body) throw new Error("Stream failed");

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        text.value += decoder.decode(value, { stream: true });
      }
      status.value = "done";
    } catch (err) {
      if ((err as Error).name === "AbortError") return;
      status.value = "error";
      error.value = "Unable to complete response. Try again.";
    }
  }

  function cancel(): void {
    controller?.abort();
    status.value = "idle";
  }

  onUnmounted(() => controller?.abort());

  return { text, status, error, send, cancel };
}
```

## Chat Panel States

```vue
<template>
  <section aria-label="Assistant chat">
    <div v-if="status === 'idle' && !text" class="empty-state">Ask a question to begin.</div>
    <div v-else-if="status === 'streaming' && !text" aria-busy="true">Thinking…</div>
    <article v-else aria-live="polite">{{ text }}</article>
    <p v-if="error" role="alert">{{ error }}</p>
  </section>
</template>
```

Debounce aria-live updates during rapid token arrival — update visible text every frame but announce summaries at interval or completion.

## Citations Block

```vue
<ul v-if="sources.length" aria-label="Sources">
  <li v-for="source in sources" :key="source.id">
    <a :href="source.url" rel="noopener noreferrer">{{ source.title }}</a>
  </li>
</ul>
```
