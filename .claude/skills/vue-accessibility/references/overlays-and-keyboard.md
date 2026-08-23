# Overlays and Keyboard

## Modal Focus Trap

```vue
<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from "vue";

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ close: [] }>();
const dialogRef = ref<HTMLDialogElement | null>(null);
let previouslyFocused: HTMLElement | null = null;

watch(
  () => props.open,
  async (isOpen) => {
    if (isOpen) {
      previouslyFocused = document.activeElement as HTMLElement | null;
      await nextTick();
      dialogRef.value?.showModal();
      dialogRef.value?.focus();
    } else {
      dialogRef.value?.close();
      previouslyFocused?.focus();
    }
  },
);

function onKeydown(event: KeyboardEvent): void {
  if (event.key === "Escape") emit("close");
}
</script>

<template>
  <dialog
    ref="dialogRef"
    role="dialog"
    aria-modal="true"
    aria-labelledby="dialog-title"
    @keydown="onKeydown"
    @close="emit('close')"
  >
    <h2 id="dialog-title">Confirm action</h2>
    <!-- focusable content -->
    <button type="button" @click="emit('close')">Cancel</button>
  </dialog>
</template>
```

Prefer native `<dialog>` when supported; otherwise use a headless component matching project patterns.

## Live Regions for Async Updates

```vue
<div aria-live="polite" aria-atomic="true">
  {{ statusMessage }}
</div>
```

Use `polite` for non-urgent updates; `assertive` only for critical errors. Debounce streaming text so screen readers are not flooded.

## Menu Keyboard Pattern

- `Enter` / `Space` opens menu on trigger
- `ArrowDown` / `ArrowUp` moves between items
- `Escape` closes and returns focus to trigger
- `Tab` closes menu per WAI-ARIA menu pattern or use native `<select>` when possible

## Route Change Focus

After navigation, move focus to `h1` or `#main` so screen reader users hear the new page context:

```typescript
router.afterEach(async () => {
  await nextTick();
  document.getElementById("main")?.focus();
});
```
