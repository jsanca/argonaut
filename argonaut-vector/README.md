# argonaut-vector

Standalone vector embedding and search experiment (port 8086). Runs completely independently — no other Argonaut module is required.

Supported providers: `in_memory` (default), `integrallis`, `qdrant`.

---

## Running inside Docker (recommended)

The application and Qdrant both run as Linux/amd64 containers, which satisfies the
`linux-x86_64` native library requirement of `ai.djl.huggingface:tokenizers:0.36.0`.
This is the recommended path on an Intel Mac host.

```bash
cd argonaut-vector
docker compose up --build
```

First build downloads the 90 MB ONNX model from HuggingFace and stores it in the
Maven BuildKit cache — subsequent builds skip the download entirely.

The stack is ready when `argonaut-vector` logs:
```
Started ArgonautVectorApplication in ... seconds
```

### Topology

```
macOS Intel host
      |
 Docker (linux/amd64)
      |
      +-- argonaut-vector  → :8086
      +-- qdrant            → :6333 (REST), :6334 (gRPC)
```

`argonaut-vector` talks to Qdrant via the Compose service name (`qdrant:6334`).
The env var `ARGONAUT_VECTOR_QDRANT_HOST=qdrant` overrides the default `localhost`
from `application-qdrant.yml`.

### Smoke test

Store documents:

```bash
curl -s -X POST http://localhost:8086/api/vector/store \
  -H 'Content-Type: application/json' \
  -d '{"id":"doc-1","content":"The quick brown fox jumps over the lazy dog"}'

curl -s -X POST http://localhost:8086/api/vector/store \
  -H 'Content-Type: application/json' \
  -d '{"id":"doc-2","content":"Spring Boot simplifies the creation of production-ready Java applications"}'
```

Search:

```bash
curl -s -X POST http://localhost:8086/api/vector/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"fast fox","limit":3}' | jq .
```

Health and info:

```bash
curl http://localhost:8086/api/health
curl http://localhost:8086/api/about
```

### Stopping and cleaning

Stop (data preserved):

```bash
docker compose stop
```

Restart:

```bash
docker compose start
```

Stop and erase all Qdrant data:

```bash
docker compose down -v
```

---

## Running on the host (without Docker)

`in_memory` and `integrallis` need no external infrastructure.
Requires a Linux host or a JDK/platform that ships the `osx-x86_64` native tokenizer lib.
On an Intel Mac, `ai.djl.huggingface:tokenizers:0.36.0` lacks `osx-x86_64` support — use Docker instead.

```bash
# Default — in_memory provider
mvn spring-boot:run -pl argonaut-vector

# Integrallis (embedded, persistent) — set a storage path
mvn spring-boot:run -pl argonaut-vector \
  -Dspring-boot.run.jvmArguments="\
    -Dargonaut.vector.provider=integrallis \
    -Dargonaut.vector.integrallis.storage-path=/tmp/argonaut-vector-store"
```

---

## Configuration reference

| Property | Default | Description |
|---|---|---|
| `argonaut.vector.provider` | `in_memory` | Active backend: `in_memory`, `integrallis`, `qdrant` |
| `argonaut.vector.qdrant.enabled` | `false` | Must be `true` to instantiate `QdrantVectorAdapter` |
| `argonaut.vector.qdrant.host` | `localhost` | Qdrant hostname |
| `argonaut.vector.qdrant.grpc-port` | `6334` | Qdrant gRPC port |
| `argonaut.vector.integrallis.storage-path` | `""` | Empty = in-process; set a path for persistent storage |

Activate the `qdrant` Spring profile to enable the Qdrant backend with all properties set for local use.
When running in Docker Compose, `ARGONAUT_VECTOR_QDRANT_HOST=qdrant` overrides the host.
