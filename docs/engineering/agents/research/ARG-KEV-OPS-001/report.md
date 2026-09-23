# ARG-KEV-OPS-001 — Kev Local Runtime & Docker Deployment Investigation

**Date:** 2026-09-23
**Scope:** `libs-code/kev` (checked-out source, commit `557598f`)
**Question:** What is the smallest clean container strategy for running Kev as a reproducible local System One execution target for Argonaut?

**Bottom line:** Kev ships **no** Docker/container mechanism of its own — only a Modal GPU entrypoint (`modal_app.py`), which is a Modal-specific orchestrator, not a portable image. The smallest Argonaut-owned approach is **one generic image** whose runtime selects a Kev checkpoint by id, with model weights kept in a **Hugging Face cache volume**, not baked into the image. One Kev-side defect blocks containerization as-is: the server hardcodes `host="127.0.0.1"` and has no readiness endpoint. Both have a one-line wrapper workaround, so no Kev fork is required.

---

## 1. Current Kev Runtime Architecture

Kev is a prefill-only decision model: a rank-16 LoRA adapter plus a small "pointer head" on a frozen Qwen base. It does not generate text; it returns a probability distribution per typed question.

```
POST /v1/systemone  ->  kev.api.to_record (typed questions -> internal record)
                     ->  kev.model.encode (state + branch token packing, block-causal mask)
                     ->  DecisionModel (Qwen backbone, hybrid row form on Qwen3.5)
                     ->  PointerHead (option <decide> vs </opt> score -> softmax)
                     ->  kev.api.to_answers (Noul/Choice/Score response)
```

The server (`kev/serve.py`) is FastAPI + uvicorn and loads **one** checkpoint at startup:

- Entry: `python -m kev.serve --run <hub id | local dir | id@tag> --port <p>`
- `Checkpoint.load()` resolves the checkpoint repo (adapter + `head.pt` + tokenizer) via `snapshot_download`, then loads the base model through `transformers.AutoModelForCausalLM`. **All downloads happen before uvicorn starts listening** — there is no lazy/first-request loading.
- Checkpoint repos are tiny (the LoRA adapter + `head.pt`); the multi-GB cost is the base Qwen model, downloaded from `Qwen/*` at load time.
- Endpoints: `POST /v1/systemone`, `POST /v1/systemone/permute`, `POST /v1/systemone/separate`, `GET /v1/models`. Responses carry an `x-typesafe-request-id` header and optional bearer auth (`KEV_API_KEY`).
- Model selection is the `--run` CLI argument. There is **no** `KEV_MODEL` environment variable today.

Environment knobs (read by `LoadOptions.from_env()` in `kev/checkpoint.py` and `kev/serve.py`): `KEV_DTYPE` (`bf16|fp16|fp32`), `KEV_BACKEND` (`torch|mlx|auto`), `KEV_MERGE`, `KEV_ATTN`, `KEV_TEMPERATURE`, `KEV_LORA_SCALE`, `KEV_PREFIX_CACHE`, `KEV_PREFIX_MIN_TOKENS`, `KEV_DATE_FACTS`, `KEV_API_KEY`, `KEV_SHAPE_BUCKET`. Model choice is not among them.

## 2. Existing Docker / Container Support

**None.** Verified by search across the repo:

- No `Dockerfile`, `Containerfile`, `docker-compose*`, or `compose*.yaml` anywhere.
- The only containerization is `modal_app.py`, which builds a Modal image from `pyproject.toml`/`uv.lock`, ships `kev/`, and mounts two Modal volumes (`kev-hf-cache` for base weights, `kev-runs` for outputs). It installs `flash-linear-attention` + `triton>=3.7.1` because the Linux torch wheels are the CUDA build and the Qwen3.5 hybrid backbones need those kernels for fast inference.
- The `kev-finetune` skill's `scripts/kev_modal.py` is a *self-contained* Modal app that clones `github.com/jaredpalmer/kev` at a pinned ref and `pip install`s it — a useful "no local clone" packaging pattern, but Modal-only.

There is no OCI image to prefer, and nothing "sufficiently maintained" in the Docker sense. Argonaut must own the containerization.

## 3. Model Inventory (current generation, from source)

All three live checkpoints are **Qwen3.5 hybrid** backbones (Gated DeltaNet + attention), built on `decision-v7` + a short delta pass. They were updated 2026-09-21; the pre-delta weights are at Hub revision `v7-base`.

| Model | Checkpoint id | Base (revision) | LoRA trainable | Built-in T | Trial |
|---|---|---|---|---|---|
| **Kev-0.8B** | `jaredpalmer/kev-0.8b` | `Qwen/Qwen3.5-0.8B-Base` (`dc7cdfe2`) | 11.3M (r=16) | 2.41 | `night2-08b-du2/00-trial-0` |
| **Kev-4B** | `jaredpalmer/kev-4b` | `Qwen/Qwen3.5-4B-Base` (`1001bb4d`) | 33.8M (r=16) | 2.14 | `night2-4b-du/00-trial-0` |
| **Kev-9B** | `jaredpalmer/kev-9b` | `Qwen/Qwen3.5-9B-Base` (`68c46c4b`) | 45.4M (r=16) | 2.30 | `night2-9b-du/00-trial-0` |

The task's "Kev 0.8B / 4B / 9B" map one-to-one onto these three Hub ids. Older generations (`kev-0.5b` Qwen2.5, `kev-0.6b`/`kev-4b@qwen3`/`kev-8b` Qwen3) are still published but superseded; do not use them for new work. The Qwen3 generation is noted in Kev's docs as the faster choice on Apple Silicon, but it is out of development.

## 4. Resource Requirements

Authoritative figures from the model cards and `modal_app.py`:

| Model | Serving (bf16) GPU RAM | Training peak | CPU feasibility | CUDA | Apple Silicon |
|---|---|---|---|---|---|
| Kev-0.8B | ~2 GB (fits a 4 GB GPU; card + README `--batch 1 --accum 8`) | 4 GB GPU | Yes, slow | Yes (fast with `flash-linear-attention`) | MLX backend |
| Kev-4B | **~9 GB** | 24.6 GB | Yes, very slow | Yes | MLX backend |
| Kev-9B | **~19 GB** | 39.5 GB | Marginal (fp32 needs ~36 GB RAM) | Yes | MLX backend |

- **dtype:** serving defaults to `bf16` on CUDA/MPS (half the memory, 2–4.5× lower latency, same argmax); `KEV_DTYPE=fp32` is the exact evaluation path (≈2× memory). `KEV_DTYPE` is the recommended precision knob.
- **Weights on disk:** bf16 ≈ 2 bytes/param → ~1.6 / 8 / 18 GB base downloads; fp32 ≈ 2×. The adapter repos themselves are ~45–180 MB.
- **CUDA dependency:** `transformers>=5.17`, `peft>=0.21`, and — for fast DeltaNet kernels on the hybrid backbones — `flash-linear-attention` + `triton>=3.7.1`. Without them, PyTorch falls back to slow reference code (still correct).
- **CPU:** `kev.device.default_device()` falls through `cuda → mps → cpu`, and on CPU the server keeps fp32. Correct but slow; fine for smoke/contract tests with the 0.8B, not for interactive 4B/9B use.
- **Apple Silicon:** the hybrid backbones run through `kev.mlx_model` (mlx-lm, installed only under `sys_platform == 'darwin' and platform_machine == 'arm64'`). MLX needs Metal, which a Linux container does not have.

## 5. Docker Alternatives

### A — One image per model (`argonaut/kev-0.8b`, `kev-4b`, `kev-9b`)
Separates nothing that matters. The runtime, code, and dependencies are identical across all three checkpoints; only the model id (and its base download) differs. Three images means three times the build/push/maintenance surface for the same Python/torch layers, and it invites baking weights in (see §8).

### B — One generic image, model selected at runtime (`KEV_MODEL=...` or `--run ...`)
Correctly treats the checkpoint as **runtime configuration**. The image holds code + dependencies only; the checkpoint id is an env var or command argument. One image to build, test, and patch.

### C — Generic image + Compose profiles (`kev-small` / `kev-default` / `kev-large`)
Not an alternative to B, but a **thin Compose ergonomics layer on top of B**. Profiles let `docker compose up kev-small` alias a `KEV_MODEL` preset without changing the image. Profiles are cheap and can be added later; they should not create separate images.

### Recommendation
**B**, with C available as sugar. The correct split is:

| Concern | Where it lives |
|---|---|
| Runtime + code + deps | the single `argonaut/kev` image |
| Model weights (base + adapter) | a Hugging Face cache **volume** |
| Model choice + precision | env / command-line **configuration** (`KEV_MODEL`, `KEV_DTYPE`, …) |
| Hardware acceleration | the **runtime** (nvidia-container-toolkit on Linux; native MLX on macOS) |

Do **not** create separate images merely because checkpoints have different parameter counts — the checkpoint id is the only thing that changes.

## 6. Recommended Image Strategy

A single reusable image built from the pinned Kev source, with an entrypoint that maps Argonaut configuration onto `kev.serve`:

```dockerfile
# argonaut-owned, lives in the Argonaut repo (or alongside libs-code/kev), NOT in the Kev repo
FROM python:3.13-slim   # Kev requires Python 3.12/3.13 (torch has no 3.14 wheels)
# install kev[serve] from a pinned Kev ref; see note on torch below
# ENTRYPOINT reads KEV_MODEL and execs: python -m kev.serve --run "$KEV_MODEL" --port "$PORT"
```

Two image variants are worth distinguishing, because the Linux `uv sync` torch wheel is the **CUDA** build:

- **`argonaut/kev:cuda`** — default, `flash-linear-attention` + `triton>=3.7.1`, ~3+ GB image (dominated by CUDA torch). The Linux/CUDA target.
- **`argonaut/kev:cpu`** — CPU torch wheel, no CUDA/fla/triton, ~0.5–1 GB image. The CI/smoke/contract-test target.

The image is large because of **torch**, not because of Kev weights — which is exactly why weights must stay out of the image.

The one Kev-side defect: `kev/serve.py:179` hardcodes `uvicorn.run(app, host="127.0.0.1", ...)`. A container bound to loopback is unreachable from the host. Two fixes, in order of preference:

1. **Argonaut entrypoint wrapper (no fork):** a small `serve.py` that imports Kev's `app`, loads the checkpoint into `app.state.server`, and runs `uvicorn.run(app, host="0.0.0.0", ...)`. This reuses `kev.checkpoint`/`kev.serve.Server` unchanged and never forks Kev.
2. **Upstream contribution (cleanest):** add a `--host`/`KEV_HOST` knob to `kev.serve`. Small, benefits the project, but requires a Kev PR to land before Argonaut can rely on it.

## 7. Model-Selection Configuration

Design the Argonaut contract around a **Kev checkpoint id**, never around Qwen internals:

```text
KEV_MODEL=jaredpalmer/kev-0.8b      # default (smoke / contract / CI)
KEV_MODEL=jaredpalmer/kev-4b        # recommended for real use
KEV_MODEL=jaredpalmer/kev-9b        # max accuracy, explicit
KEV_MODEL=jaredpalmer/kev-4b@v7-base  # any Hub revision/tag works via id@tag
```

- The entrypoint translates `KEV_MODEL` → `kev.serve --run`. Argonaut selects a **Kev checkpoint**; Kev owns the mapping to the underlying Qwen base, revision, and adapter layout (all recorded in the checkpoint's `head.pt`/`adapter_config.json`).
- Default: **`jaredpalmer/kev-0.8b`**. It optimizes startup/resource cost (smallest base download, ~2 GB RAM, CPU-feasible) which is what smoke tests, contract tests, and CI need. Kev's own docs recommend 4B for general use and 9B for accuracy/calibration; both remain selectable explicitly. (If Argonaut wants *quality* as the default for interactive development, `kev-4b` is the Kev-recommended default — the trade is a ~8 GB base download.)
- Keep the other `KEV_*` knobs exposed (notably `KEV_DTYPE`, `KEV_API_KEY`) but document them as Kev-owned; Argonaut config should not restate Qwen architecture.

## 8. Cache / Persistence Strategy

**Weights stay out of the image; use a Hugging Face cache volume.**

| Location | Verdict |
|---|---|
| Container image | **No.** Weights are ~1.6–18 GB per model; baking them makes the image multi-GB, kills rebuild speed, and couples a base download to image pushes. |
| **Docker volume (`HF_HOME=/cache`)** | **Yes.** Persistent, survives container restarts, per-model lazy download on first use, `HF_HUB_OFFLINE=1` enables fully offline runs afterward. |
| Host-mounted `~/.cache/huggingface` | Optional. Reuses a developer's existing cache (no re-download), but couples the container to host paths and macOS/Linux cache layout differences. |
| Ephemeral container FS | No. Every restart re-downloads multi-GB bases. |

Mechanics (already proven by Kev's Modal images, which set `HF_HOME=/hf`):

- Set `HF_HOME` (and `HF_HUB_DISABLE_PROGRESS_BARS=1`, `TOKENIZERS_PARALLELISM=false`) in the image.
- The checkpoint repo (`jaredpalmer/kev-*`) and the base (`Qwen/Qwen3.5-*-Base`) both land in the HF cache on first load. A given `KEV_MODEL` therefore costs one download, then cold starts are cache-bound.
- For CI, warm the volume once and reuse it, or pin `HF_HUB_OFFLINE=1` against a pre-populated cache to make runs repeatable and network-independent.
- Optional `HF_TOKEN` (for gated bases) should be supplied as a secret, never baked into the image — consistent with the existing Modal `KEV_HF_SECRET` pattern. All three current bases are public, so no token is needed today.

## 9. Hardware Matrix

| Target | Mechanism | Notes |
|---|---|---|
| **Linux + NVIDIA** | Docker (nvidia-container-toolkit) + `argonaut/kev:cuda` | The canonical container path. `flash-linear-attention` + `triton>=3.7.1` required for fast DeltaNet kernels. 4B needs ≥ ~10 GB VRAM, 9B ~20 GB. |
| **Linux, CPU-only** | Docker CPU image | Works; fp32. 0.8B practical for smoke/contract; 4B/9B are slow but correct. |
| **macOS / Apple Silicon** | **Native**, not Docker | Docker Desktop does **not** expose the Apple GPU to Linux containers, so MLX/MPS cannot run inside Docker. Native `uv sync --extra serve` + MLX is strictly better (and Kev's documented Mac path). |
| **No GPU anywhere** | CPU (native or Docker) | 0.8B only; correctness unaffected, latency is. |

Do **not** claim Docker gives native Apple GPU acceleration — it does not. The documented split is: **Docker for Linux/CUDA and CPU-CI; native `uv` + MLX on macOS.** Forcing one mechanism everywhere would regress the Mac experience.

## 10. Compose Proposal

Default model via a variable, larger checkpoints without editing source:

```yaml
# compose.yaml (Argonaut-owned)
services:
  kev:
    image: argonaut/kev:cpu          # swap :cuda when a GPU is available
    build:
      context: .
      dockerfile: Dockerfile.kev
    environment:
      KEV_MODEL: ${KEV_MODEL:-jaredpalmer/kev-0.8b}
      KEV_DTYPE: ${KEV_DTYPE:-bf16}
      PORT: "8008"
    ports:
      - "${KEV_PORT:-8008}:8008"
    volumes:
      - kev-hf-cache:/cache
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8008/v1/models', timeout=5).status==200 else 1)"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 120s
    restart: unless-stopped

volumes:
  kev-hf-cache:
```

Usage:

```bash
docker compose up kev                                   # Kev-0.8B (default)
KEV_MODEL=jaredpalmer/kev-9b docker compose up kev      # Kev-9B, no source edit
```

(Profiles — `kev-small`/`kev-default`/`kev-large` as `KEV_MODEL` presets — can be layered on later without image changes; see §5C.)

## 11. Readiness Strategy

Kev has **no health or readiness endpoint**, and it has no first-request lazy loading — the model is fully loaded before `uvicorn.run` is called, so "the socket accepts and `GET /v1/models` returns 200" is equivalent to "ready to answer `/v1/systemone`."

`GET /v1/models` is the closest existing signal: it returns the resolved checkpoint, base, device, backend, dtype, temperature, and prefix-cache stats. Smallest solutions, in order:

1. **Probe `GET /v1/models` (no Kev change).** Use it as the Compose `healthcheck` and the Argonaut readiness probe. A 200 means the model is loaded; startup logs (`serving <run> ... on <device> via <backend>`) confirm it.
2. **Add a trivial `GET /health` (Kev upstream, 3 lines).** Cleaner semantically and cheaper than `/v1/models`, but requires a Kev PR. Worth proposing; not a blocker.

Recommendation: ship with (1), propose (2) upstream.

## 12. CI Feasibility

**Yes, with Kev-0.8B and a warm cache.**

- **Download:** ~1.6 GB base + ~45 MB adapter. This is the dominant first-run cost; amortize it with a persisted HF cache (a warmed volume or a CI cache keyed on the checkpoint id). `HF_HUB_OFFLINE=1` + pre-populated cache makes runs network-free and repeatable.
- **RAM:** ~2–4 GB (bf16/fp32) for 0.8B — fits standard runners.
- **Execution:** CPU-only is viable for the 0.8B; a single request is sub-second, and the server runs one request at a time (no batching across callers).
- **GPU:** not required. The `:cuda` image is for real Linux/CUDA environments, not CI runners.
- **Determinism:** contract tests must assert the **API shape**, not a specific prediction. Assert: 200 with the correct `answers` schema (Noul `.noul`, Choice `.choice`/`.probabilities`/`.confidence`, Score `.score`/`.legend`), the `x-typesafe-request-id` response header, `422` on invalid input, and `usage`/`latency_ms` presence. Probabilities differ across checkpoints and even dtypes (bf16 vs fp32); never pin them.

Note: `tests/test_api.py` already exercises this contract against a running server (`KEV_BASE_URL=...`). Pointing it at a Kev container gives Argonaut a ready-made contract suite.

## 13. System One Smoke Test

Request shape for one packed `POST /v1/systemone` containing **1 Choice + 1 Score + 1 Noul** (the exact README example):

```bash
curl -s localhost:8008/v1/systemone -H 'content-type: application/json' -d '{
  "state": "Shoes arrived two weeks late and in the wrong size. Also I see two charges on my card.",
  "model": "kev-latest",
  "questions": {
    "department":  {"type": "choice", "instructions": "Which team should handle this?",
                    "criteria": {"returns": "Exchanges, refunds, wrong or damaged items",
                                 "shipping": "Delivery status, delays, lost packages",
                                 "billing": "Charges, invoices, payment problems"}},
    "escalate":    {"type": "noul",  "instructions": "Does this need urgent human attention?"},
    "frustration": {"type": "score", "instructions": "How frustrated is the customer?",
                    "criteria": ["Calm", "Frustrated", "Very angry"]}
  }}'
```

Expected response shape (probabilities are model/dtype-dependent and are illustrative only):

```json
{
  "model": "kev-latest",
  "answers": {
    "department":  { "type": "choice", "choice": "returns", "confidence": 0.21,
                     "probabilities": { "returns": 0.47, "shipping": 0.28, "billing": 0.25 } },
    "escalate":    { "type": "noul", "noul": 0.93 },
    "frustration": { "type": "score", "score": 1.44, "confidence": 0.78,
                     "legend": { "0": "Calm", "1": "Frustrated", "2": "Very angry" },
                     "probabilities": { "0": 0.00, "1": 0.56, "2": 0.44 } }
  },
  "usage": { "input_tokens": 101, "output_tokens": 161 },
  "latency_ms": 495
}
```

**Status:** verified against `kev/api.py` (`to_record`/`to_answers`) and the `kev.serve` contract; the exact wire call was **not executed** in this investigation because it requires a ~GB base-model download that the container strategy (not yet implemented) is meant to own. Executing this curl against the container is step 1 of the implementation slice. The TypeSafe SDK (`TypeSafeClient(api_key="local", base_url=..., model="kev-latest")`) is the same contract under investigation in `argonaut-decision-jev`, so a successful container response proves Argonaut's System One client works against Kev unchanged.

## 14. Recommended Implementation Slice

Do **not** implement until explicitly approved. The minimal slice, in dependency order:

1. **Unblock the loopback bind.** Write an Argonaut entrypoint (`serving/kev_serve.py`) that loads a checkpoint via `kev.checkpoint` and serves Kev's `app` on `0.0.0.0`; `KEV_MODEL` → `--run`. (Parallel: propose `--host`/`KEV_HOST` upstream in a Kev PR.)
2. **One Dockerfile** (`Dockerfile.kev`, Argonaut-owned) with `:cuda` and `:cpu` targets, `HF_HOME=/cache`, `flash-linear-attention`+`triton` on the CUDA target only, `USER` non-root, `KEV_MODEL` documented default `jaredpalmer/kev-0.8b`.
3. **`compose.yaml` service `kev`** per §10 with the `GET /v1/models` healthcheck and a `kev-hf-cache` volume.
4. **Smoke gate:** run the §13 curl against the container; assert schema + `x-typesafe-request-id` + `422` on bad input (reuse `tests/test_api.py` against the container).
5. **CI:** add a contract-test job that runs the `:cpu` image with a warm HF cache and validates the API shape only (no prediction assertions).

Explicitly out of scope for this slice (and recommended against): baking weights into images, one-image-per-model, any attempt to run MLX/MPS inside Docker, and any Kev fork — the wrapper entrypoint and the upstream `--host` contribution keep Argonaut on top of stock Kev.
