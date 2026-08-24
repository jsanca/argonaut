# ARGONAUT-INFRA-001 — Containerized UC-001 Laboratory

**Date:** 2026-08-23  
**Status:** Implemented; Docker image build verification in progress

## Outcome

The UC-001 laboratory can now be assembled with Docker Compose. It contains five independently built backend services—Spring AI, LangChain4j, LangGraph4j, Embabel, and Koog—and a production Vue UI served by nginx.

The browser has one public origin. nginx serves the UI and proxies `/api/frameworks/<framework>/...` to the matching backend's existing `/api/...` contract. The framework registry supplies only those same-origin prefixes; it contains no internal hostnames or provider credentials.

## Delivered configuration

| Artifact | Responsibility |
| --- | --- |
| `compose.yaml` | Defines the five backend services and nginx UI, backend health checks, runtime environment, restart policy, and the external UI port. |
| `Dockerfile.backend` | Reusable Java 25 multi-stage image: Maven reactor build selected with `MODULE`, then a JRE runtime image. Maven cache mounts make retries resilient to interrupted dependency downloads. |
| `docker/HttpHealthcheck.java` | Tiny JDK-native HTTP probe used in the slim JRE image to check the real `/api/health` route. |
| `argonaut-ui/Dockerfile` | Node production build followed by nginx static serving; it does not run Vite in production. |
| `argonaut-ui/nginx.conf` | SPA fallback, cache-safe runtime registry, and same-origin reverse-proxy routes. |
| `argonaut-ui/public/config/frameworks.json` | Runtime registry of UI-visible framework identifiers and same-origin base paths only. |
| `.env.example` | Secret-free configuration template, including all five service ports and the UI port. |

## Runtime behavior

Backend containers receive `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, and `OPENROUTER_BASE_URL` at runtime. The UI container receives none of them. A missing provider key does not prevent a service health endpoint from starting, but it prevents a real provider-backed experiment from succeeding; this is expected and is documented in `.env.example` and the root README.

The UI waits only for containers to be started, not for all health checks to pass. This deliberately preserves its partial-failure behavior: unavailable frameworks show independently instead of preventing the console from loading. Health checks invoke the actual `/api/health` endpoint, but are not used as an artificial readiness gate for the UI.

## Verification

| Check | Result |
| --- | --- |
| `mvn verify -q` | Passed locally: 156 tests, 0 failures. |
| `npm test` | Passed: 19 tests across 3 files. |
| `npm run build` | Passed: production Vite bundle generated. |
| `docker compose config --quiet` | Passed. |
| Full Compose image build | Retrying during verification. The first attempt failed solely because Maven Central ended a Kotlin compiler transfer prematurely; the Dockerfile now uses a persistent Maven cache mount. |
| Full stack health routes / experiment | Pending the complete image build. A provider-backed experiment also requires user-supplied OpenRouter credentials. |

## Non-goals preserved

No backend endpoint, experiment contract, provider implementation, or framework-specific application behavior was changed. The work adds deployment topology, production UI serving, runtime routing, and documentation only; the small UI TypeScript fixes restore the existing test/build pipeline without changing its network contract.
