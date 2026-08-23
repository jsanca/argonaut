# CI Pipeline Gates

## Typical Gate Script

```json
{
  "scripts": {
    "lint": "eslint . --ext .vue,.js,.jsx,.cjs,.mjs,.ts,.tsx,.cts,.mts",
    "format:check": "prettier --check \"src/**/*.{vue,ts,tsx,css,md}\"",
    "typecheck": "vue-tsc --noEmit -p tsconfig.app.json",
    "test:ci": "vitest run --coverage",
    "check": "npm run lint && npm run format:check && npm run typecheck && npm run test:ci"
  }
}
```

## GitHub Actions Example

```yaml
jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: npm
      - run: npm ci
      - run: npm run check
```

Match Node version to local `.nvmrc` / `engines` field.

## Pre-commit (Optional)

Husky + lint-staged for fast feedback — full `check` still runs in CI:

```json
{
  "lint-staged": {
    "*.{vue,ts}": ["eslint --fix", "prettier --write"]
  }
}
```

Do not replace CI typecheck with pre-commit-only partial checks.
