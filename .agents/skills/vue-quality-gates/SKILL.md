---
name: vue-quality-gates
description: Configure and fix Vue frontend CI quality gates. Use when setting up or repairing npm lint, format check, vue-tsc typecheck, Vitest with coverage thresholds, Husky pre-commit hooks, or CI failures on eslint-plugin-vue, Prettier, or test scripts.
---

# Vue Quality Gates

## Workflow

1. Read `package.json` scripts, ESLint config (`eslint.config.js` or `.eslintrc`), Prettier, `tsconfig.json`, `env.d.ts`, Vitest config, and CI workflow.
2. Identify the **CI gate script** — often `npm run check`, `lint`, `typecheck`, `test:ci`, or `build`.
3. Read `references/ci-pipeline-gates.md` for pipeline structure.
4. Read `references/lint-typecheck-and-coverage.md` for eslint-plugin-vue, vue-tsc, and coverage setup.
5. Fix type errors before relaxing `strict` — avoid `@ts-ignore` in changed files.
6. Keep ESLint and Prettier boundaries clear — use `eslint-config-prettier` to avoid rule fights.
7. Run the same npm script locally as CI (`npm ci` then gate script).

## Gate Checklist

- `vue-tsc --noEmit` or `npm run typecheck` passes with strict TypeScript when enabled.
- ESLint passes with zero errors on PR (warnings per team policy).
- Prettier check (`prettier --check`) or format gate passes.
- Unit tests pass in CI with stable `jsdom` / `happy-dom` environment.
- Coverage thresholds configured in Vitest — scoped to `src/`, not trivial files only.

## Output

Summarize scripts, CI config, threshold changes, commands (`npm run …`), and intentional exclusions.

## Related Skills

- `vue-testing-verification` — vue-test-utils patterns
- `testing-strategy` — pyramid and flaky test policy
- `vue-application-engineering` — feature fixes
