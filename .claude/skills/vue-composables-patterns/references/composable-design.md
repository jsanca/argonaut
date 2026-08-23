# Composable Design

## Focused Composable API

```typescript
import { ref, computed, type Ref } from "vue";

export function useCounter(initial = 0) {
  const count = ref(initial);
  const doubled = computed(() => count.value * 2);

  function increment(): void {
    count.value += 1;
  }

  return { count, doubled, increment };
}
```

Return refs directly — callers keep reactivity when destructuring.

## Accepting Reactive Inputs

```typescript
import { ref, watch, type MaybeRefOrGetter, toValue } from "vue";

export function useUserProfile(userId: MaybeRefOrGetter<string>) {
  const profile = ref<UserProfile | null>(null);
  const loading = ref(false);

  watch(
    () => toValue(userId),
    async (id, _, onCleanup) => {
      let stale = false;
      onCleanup(() => {
        stale = true;
      });
      loading.value = true;
      try {
        profile.value = await fetchProfile(id);
      } finally {
        if (!stale) loading.value = false;
      }
    },
    { immediate: true },
  );

  return { profile, loading };
}
```

## Inject When Context Matters

```typescript
import { inject, provide, type InjectionKey } from "vue";

const ThemeKey: InjectionKey<Ref<"light" | "dark">> = Symbol("theme");

export function provideTheme(initial: "light" | "dark") {
  const theme = ref(initial);
  provide(ThemeKey, theme);
  return theme;
}

export function useTheme() {
  const theme = inject(ThemeKey);
  if (!theme) throw new Error("useTheme requires provideTheme");
  return theme;
}
```

Prefer explicit props/emits over inject for most feature code — reserve inject for app-shell concerns.
