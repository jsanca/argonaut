# Forms and Semantics

## Label Association

```vue
<script setup lang="ts">
defineProps<{ errors?: { email?: string } }>();
</script>

<template>
  <label for="email" class="block text-sm font-medium">
    Email
    <input
      id="email"
      name="email"
      type="email"
      autocomplete="email"
      :aria-invalid="errors?.email ? true : undefined"
      :aria-describedby="errors?.email ? 'email-error' : undefined"
      class="mt-1 w-full rounded border px-3 py-2"
    />
  </label>
  <p
    v-if="errors?.email"
    id="email-error"
    role="alert"
    class="mt-1 text-sm text-red-700"
  >
    {{ errors.email }}
  </p>
</template>
```

## Page Landmarks

```vue
<template>
  <a href="#main" class="sr-only focus:not-sr-only">Skip to main content</a>
  <header>...</header>
  <nav aria-label="Primary">...</nav>
  <main id="main">...</main>
  <footer>...</footer>
</template>
```

## Heading Hierarchy

One `h1` per page view; do not skip levels for styling — use CSS for visual size.

## Button vs Link

- `button` for actions on current page (submit, open modal)
- `RouterLink` or `a href` for navigation to new URL

```vue
<!-- Wrong -->
<div @click="save">Save</div>

<!-- Right -->
<button type="button" @click="save">Save</button>
```

## Images

```vue
<img :src="product.imageUrl" :alt="product.name" />
<img src="/icons/search.svg" alt="" aria-hidden="true" />
```

Decorative images: empty alt or `aria-hidden`.
