# Lint, Typecheck, and Coverage

## ESLint Flat Config (Vue + TypeScript)

```javascript
import pluginVue from "eslint-plugin-vue";
import vueTsConfig from "@vue/eslint-config-typescript";
import skipFormatting from "@vue/eslint-config-prettier/skip-formatting";

export default [
  ...pluginVue.configs["flat/recommended"],
  ...vueTsConfig(),
  skipFormatting,
  {
    rules: {
      "vue/multi-word-component-names": "off",
    },
  },
];
```

Enable `vue/block-lang` and script-setup rules per team strictness.

## vue-tsc

```json
{
  "scripts": {
    "typecheck": "vue-tsc --noEmit -p tsconfig.app.json"
  }
}
```

Ensure `env.d.ts` references `vite/client` and component type shims. Fix template type errors in SFC — do not disable `strictTemplates` globally without plan.

## Vitest Coverage Thresholds

```typescript
// vitest.config.ts
export default defineConfig({
  test: {
    coverage: {
      provider: "v8",
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 75,
        statements: 80,
      },
      include: ["src/**/*.{ts,vue}"],
      exclude: ["src/**/*.d.ts", "src/main.ts"],
    },
  },
});
```

Exclude generated types and bootstrap files explicitly — not entire features to pass gate.

## Common Failures

| Failure | Fix |
| --- | --- |
| `vue/no-v-html` | Sanitize or use text binding |
| Unused vars in script setup | Remove or prefix `_` per ESLint config |
| Template prop type mismatch | Fix prop types or narrow union |
| Vitest cannot parse `.vue` | Ensure `@vitejs/plugin-vue` in vitest config |
