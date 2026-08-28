# Task: ARGONAUT-0.2-004 Enable Runtime Vector Provider Selection in Argonaut Vector

## Context

Argonaut Vector is evolving into a retrieval laboratory where the same corpus and queries must be executed against multiple vector providers.

Current providers are:

* `IN_MEMORY`
* `INTEGRALLIS`
* `QDRANT`

At present, provider selection is primarily driven by startup configuration/profiles.

This makes controlled comparison cumbersome because changing provider requires configuration changes and/or application restarts.

The Docker Compose topology already provides Qdrant as an external service, while InMemory and Integrallis can live inside the Argonaut Vector process.

We want **all available vector providers initialized simultaneously**, with the active provider selected dynamically at runtime.

---

# Goal

Refactor provider selection so that:

1. InMemory, Integrallis and Qdrant can coexist in the same Spring application context.
2. One provider is designated as the **active runtime provider**.
3. The active provider can be changed through the REST API without restarting the application.
4. `VectorService` dynamically delegates store/search operations to the currently selected provider.
5. The existing configured provider remains useful as the **initial/default provider at startup**.
6. `/api/about` reports both the active provider and preferably the available providers.

This is intended primarily for controlled experiments and provider comparison.

---

# Important Design Constraint

Do **not** turn `VectorProperties` into mutable runtime state.

The current immutable configuration record is appropriate for startup configuration:

```java
@ConfigurationProperties(prefix = "argonaut")
public record VectorProperties(
        EmbeddingConfig embedding,
        VectorConfig vector) {
    ...
}
```

Keep the distinction:

```text
VectorProperties
    =
startup configuration

Runtime provider selection
    =
application state
```

Introduce an explicit runtime abstraction instead.

---

# Suggested Architecture

Names are suggestions, not requirements.

A design along these lines would be appropriate:

```text
                    VectorProperties
                          │
                          │ initial provider
                          ▼
                 ActiveVectorProvider
                    AtomicReference
                          │
                          ▼
                    VectorService
                          │
                          ▼
                VectorProviderRegistry
                /          |          \
               /           |           \
       InMemory       Integrallis      Qdrant
       Store/Search   Store/Search    Store/Search
```

Potential abstractions:

```java
public interface ActiveVectorProvider {

    VectorProvider current();

    void select(VectorProvider provider);
}
```

Implementation should be thread-safe.

An `AtomicReference<VectorProvider>` would be entirely reasonable.

For example:

```java
@Component
public class RuntimeVectorProvider implements ActiveVectorProvider {

    private final AtomicReference<VectorProvider> current;

    public RuntimeVectorProvider(VectorProperties properties) {
        this.current =
            new AtomicReference<>(properties.vector().provider());
    }

    @Override
    public VectorProvider current() {
        return current.get();
    }

    @Override
    public void select(VectorProvider provider) {
        current.set(provider);
    }
}
```

Do not blindly implement this example if the current architecture offers a cleaner Spring-native solution.

---

# Provider Registry

Create or adapt an abstraction that allows providers to be resolved dynamically.

Conceptually:

```java
VectorProviderRegistry
    IN_MEMORY   -> StorePort/SearchPort
    INTEGRALLIS -> StorePort/SearchPort
    QDRANT      -> StorePort/SearchPort
```

The exact design can be:

* maps,
* typed provider adapters,
* a registry abstraction,
* another clean mechanism.

Prefer explicit provider identity rather than relying on Spring bean names.

The system must fail clearly if a provider is requested but unavailable.

---

# Provider Lifecycle

All three providers should be instantiated during application startup when their required infrastructure is available.

In particular:

### InMemory

Always available.

### Integrallis

Initialize in-process.

Honor the configured storage path if one exists.

### Qdrant

Initialize against the configured Qdrant host/port.

Docker Compose already starts the Qdrant service alongside Argonaut Vector.

Review/remove current conditional configuration such as:

```java
@ConditionalOnProperty(
    prefix = "argonaut.vector.qdrant",
    name = "enabled",
    havingValue = "true"
)
```

if it prevents Qdrant from coexisting with the other providers.

Do not retain startup-time provider selection mechanisms that make the providers mutually exclusive.

Configuration controlling connectivity is still valid.

---

# Runtime API

Add an API for provider inspection and selection.

Suggested contract:

## Current provider

```http
GET /api/vector/provider
```

Example:

```json
{
  "provider": "QDRANT"
}
```

Preferably also expose:

```json
{
  "active": "QDRANT",
  "available": [
    "IN_MEMORY",
    "INTEGRALLIS",
    "QDRANT"
  ]
}
```

---

## Change provider

Suggested:

```http
PUT /api/vector/provider
Content-Type: application/json
```

Request:

```json
{
  "provider": "IN_MEMORY"
}
```

Response:

```json
{
  "previous": "QDRANT",
  "active": "IN_MEMORY"
}
```

Alternative REST shapes are acceptable if there is a clear reason.

---

# Existing `/api/about`

Update:

```http
GET /api/about
```

so that its provider field represents the **current runtime provider**, not merely the startup configuration.

Prefer exposing both:

```json
{
  "module": "argonaut-vector",
  "version": "0.1.0-SNAPSHOT",
  "activeProvider": "QDRANT",
  "availableProviders": [
    "IN_MEMORY",
    "INTEGRALLIS",
    "QDRANT"
  ]
}
```

Keep the rest of the existing metadata.

---

# VectorService

Store and search must resolve the provider **at operation time**.

Conceptually:

```java
public void storeDocument(...) {
    VectorProvider provider = activeProvider.current();
    registry.store(provider).store(...);
}

public List<SearchResult> search(...) {
    VectorProvider provider = activeProvider.current();
    return registry.search(provider).search(...);
}
```

Do not resolve the provider once in the constructor and permanently retain that selected adapter.

Switching providers through the API must affect subsequent requests immediately.

---

# Thread Safety

Runtime switching may happen while the service is receiving requests.

Provider selection therefore needs to be thread-safe.

A provider switch should be atomic.

It is acceptable that an individual request that already resolved provider A finishes against provider A while a concurrent switch changes subsequent operations to provider B.

Do not introduce heavyweight locking unless needed.

---

# Data Isolation

Providers must retain independent stores.

Switching:

```text
QDRANT
   ↓
IN_MEMORY
```

must not implicitly copy data between them.

This is important for the lab.

Each provider represents an independent retrieval implementation.

For now, corpus loading into each provider can be performed explicitly by selecting the provider and running the corpus loader.

Do not implement automatic cross-provider replication as part of this task.

---

# Configuration Semantics

The existing:

```yaml
argonaut:
  vector:
    provider: ...
```

should remain meaningful.

Its new meaning is:

> provider selected when Argonaut Vector starts.

It should **not** limit which provider beans are instantiated.

Example:

```yaml
argonaut:
  vector:
    provider: qdrant
```

means:

```text
Application starts
      ↓
all providers initialize
      ↓
active runtime provider = QDRANT
```

Afterward:

```http
PUT /api/vector/provider
```

may change it.

No restart required.

---

# Docker Compose

Adjust Compose/configuration as necessary so one topology supports all providers simultaneously.

Desired topology:

```text
docker compose up

        ┌────────────────────┐
        │  Argonaut Vector   │
        │                    │
        │  InMemory          │
        │  Integrallis       │
        │  Qdrant adapter ───────┐
        └────────────────────┘   │
                                 │
                          ┌──────▼──────┐
                          │   Qdrant    │
                          └─────────────┘
```

We should no longer need to edit Compose or switch Spring profiles merely to compare providers.

---

# Observability

Provider selection must be visible in logs.

At startup, log something equivalent to:

```text
Vector providers available: [IN_MEMORY, INTEGRALLIS, QDRANT]
Initial active vector provider: QDRANT
```

On runtime change:

```text
Vector provider changed: QDRANT -> IN_MEMORY
```

Do not log every vector operation at INFO simply to identify the provider.

Normal request-level/provider-level diagnostics may use DEBUG if useful.

---

# Error Handling

Invalid provider:

```http
PUT /api/vector/provider

{
  "provider": "ORACLE"
}
```

must return an appropriate `4xx` response.

Selecting a known but unavailable provider must also fail clearly rather than silently falling back.

Never silently change providers.

---

# Tests

Add focused tests covering at least:

### Startup

* configured default provider becomes active
* all expected providers are registered

### Runtime selection

* current provider can be queried
* Qdrant → InMemory
* InMemory → Integrallis
* Integrallis → Qdrant

### Delegation

Verify that after switching provider:

```text
store/search
```

delegate to the newly active adapter.

### Invalid selection

Unknown/unavailable provider does not modify the existing active provider.

### Concurrency

At minimum verify that provider state implementation is safe for concurrent reads/switches.

Do not over-engineer a concurrency benchmark.

---

# Backward Compatibility

Existing endpoints must continue working:

```text
GET  /api/health
GET  /api/about
POST /api/vector/store
POST /api/vector/search
```

Existing Store/Search request formats should remain unchanged for this task.

Provider selection belongs to the runtime context, not individual search requests.

---

# Non-goals

Do not implement:

* automatic corpus replication
* provider-specific request overrides
* hybrid search
* ranking comparison
* evaluation metrics
* benchmark runners
* UI
* distributed provider state

Those belong to later Argonaut Vector work.

---

# Definition of Done

The following workflow must work without rebuilding or restarting Argonaut Vector:

```text
docker compose up

GET /api/about
→ active provider = QDRANT

load corpus
search

PUT /api/vector/provider
→ IN_MEMORY

load corpus
search

PUT /api/vector/provider
→ INTEGRALLIS

load corpus
search
```

At the end we must be able to run the same smoke/evaluation suite against all three providers from a single running Argonaut Vector instance.
