# ARG-KEV-FT-001 — Kev Fine-Tuning & Decision Specialization Investigation

**Date:** 2026-09-23
**Scope:** `libs-code/kev` (checked-out source, commit `557598f`), first-party `skills/kev-finetune` material
**Question:** How can Argonaut specialize an existing Kev model for a narrow decision domain *and measure whether the fine-tune actually improved it*, against a preserved baseline?

**Bottom line:** Kev ships a complete, first-party fine-tuning workflow — the `kev-finetune` skill plus `kev.train` — that already does everything the objective asks for: warm-start delta fine-tuning from a released checkpoint, temperature calibration on a held-out slice, a paired-bootstrap comparison against the released baseline, a public-data forgetting check, and a TypeSafe endpoint. Argonaut should **reuse it directly**, not build its own trainer. The one thing Argonaut must add is discipline: a locked, never-tuned-against evaluation set and an explicit out-of-domain regression control, so "improvement" cannot be defined as training accuracy or a better-fitted temperature on the same data.

---

## 1. Kev Training Architecture

Kev is a **prefill-only decision model**: a frozen Qwen causal LM + a trainable LoRA adapter + a small trainable "pointer head". There is no text generation and no next-token objective; the whole model is trained with per-question cross-entropy on the correct option.

### What is (and is not) trainable

| Component | Trainable? | Where it lives |
|---|---|---|
| **Qwen base weights** | **Frozen** | `AutoModelForCausalLM.from_pretrained(...)`; never updated |
| **LoRA adapters** | **Trainable** | rank-16 (α=32) low-rank adapters over attention, MLP and — on Qwen3.5 — the Gated DeltaNet projections (`q/k/v/o_proj`, `gate/up/down_proj`, `in_proj_qkv/z/a/b`, `out_proj`) |
| **Kev decision (pointer) head** | **Trainable** | `PointerHead(d, dp=256)`: `q`, `k` linear layers score each option's `</opt>` hidden state against the question's `<decide>` hidden state; softmax → probabilities. Trained from scratch. |
| **Calibration parameters** | **Not trained** | a single scalar `temperature` fitted *post-hoc* on a held-out set (`head.pt["temperature"]`); applied at inference only, never touched by gradient descent |

`kev.train --lora 16` is the default; rank is read from the init checkpoint in delta mode. `--lora_targets` (`all|dense|attn|qv`) controls which modules get adapters. `--special_embeddings` optionally also trains the five delimiter tokens' embeddings (off by default).

### Objective and how each question type is trained

All three question types collapse to one primitive — a distribution over option keys — so one cross-entropy loss covers all of them:

- **Noul** → two options `[false, true]`; label `true`/`false`.
- **Choice** → one option per named key; label is the option *name*.
- **Score** → one option per ordered level; label is the level *index*; optional `--ord_w` adds a ranked-probability-score term (the released checkpoints ship with `ord_w=0`).

`question_loss` (`kev/train.py`) is cross-entropy on the label (or on a soft `target` distribution when the question carries one). Optional, all-default-0 modifiers exist (`--label_smoothing`, `--brier_w`, `--focal_gamma`) but none is in a release. Optional auxiliary losses: `--perm_kl` (symmetric KL between two option orders, for order invariance) and `--anchor_w` (KL toward the frozen base's zero-shot distribution, from `kev.anchors`).

### Question packing

One record's questions share one `state` and are packed into a block-causal sequence (`<state> … <q> instr <opt> o </opt> … <decide>`), with a mask that lets each token see the state and its own question only — questions cannot read each other. On the hybrid Qwen3.5 backbones the recurrent DeltaNet layers cannot honor that mask, so every question runs as its own causal row continuing the cached state (exact isolation by construction). Training context: 384 state tokens / 1,024 per branch / 2,048 packed; records that exceed it are dropped with a printed count.

### Optimizer, schedule, batch

- **Optimizer:** AdamW, `weight_decay=0.01`, two param groups (LoRA at `--lr`, head at `--head_lr` or `--lr`), grad-norm clip 1.0.
- **Schedule:** OneCycleLR, `pct_start=0.1`.
- **Batch:** `--batch` records per forward pass, `--accum` micro-batches per optimizer step (small variable-length records with custom masks ⇒ small batches + accumulation). Released recipe: `--batch 4 --accum 2` (4B/9B), `--batch 8` (0.8B).
- **Precision:** `--dtype bf16` (autocast forward, fp32 master weights, CUDA only); `--weights_dtype` controls the frozen backbone's storage dtype.
- **Devices:** `cuda`, `mps`, `cpu` (`kev.device.default_device()`).

### Augmentation

Per-epoch, per-record: Choice options always re-permuted; probabilistic "none of the above" insertion (as correct answer or wrong alternative), distractor insertion, and `--p_none_pair` minimal pairs. These are why the trained model is more option-order-stable.

## 2. Existing Fine-Tuning Workflow (first-party)

Kev has **two** first-party training paths:

1. **`kev.train`** (`python -m kev.train`) — the raw trainer, `--data your.jsonl --init_from <checkpoint>` for delta fine-tuning.
2. **`kev.experiment` / `modal_app.py`** — the *research* study runner: frozen suites under `evals/`, allowlisted trials, gates, transfer scoring, provenance. Designed for reproducing the released checkpoints, not user workloads.
3. **`skills/kev-finetune` + `scripts/kev_modal.py`** — the *user-workload* path: `validate`, `train`, `evaluate`, `compare`, `pull`, `publish`, `teardown`, plus a `Serve` endpoint. This is the tool that matches the Argonaut objective.

The `kev-finetune` skill already implements the entire lifecycle the objective describes:

```
workload spec (workload.json)
  → data (convert_data / generate_data / programmatic)
  → split_data (train / calibration / development by state)
  → kev_modal.py::train  (warm-start delta + temperature fit + score baseline + paired bootstrap + forgetting check)
  → serve (Modal endpoint or local kev.serve)
  → teardown
```

`train` warms-start the released LoRA + pointer head (`--init_from`), fits a temperature on `calibration.jsonl` and writes it into the checkpoint, scores `development.jsonl` for both the fine-tuned model **and** the released baseline (each temperature-fitted on the same calibration slice), computes a record-clustered paired bootstrap, and runs a 300-record public `decision-v7` regression check. Reports: `result.json`, `errors.jsonl`, `train.log`.

**Recommendation: reuse the skill directly.** It is exactly the "compare against a baseline" machinery the objective requires, it is tested (`tests/test_skill_scripts.py`), and it pins the Kev commit (`kev_ref`) so runs are reproducible. Building an Argonaut-specific trainer would re-implement warm-start, calibration, bootstrap, and deploy for no scientific gain. The only gap the skill does **not** enforce for us is the discipline around a locked test set and an unrelated-domain control (see §9, §11).

## 3. Dataset Format

One JSON object per line. A record is a System One request (`state` + `questions`) with a `label` on every question. This is *byte-identical* to what `POST /v1/systemone` serves: `kev.data.materialize` routes records through `kev.api.to_record`, the same code the server uses.

```jsonl
{"state": "Order 5521 arrived two weeks late and now I see two charges on my card. Fix this today.",
 "questions": {
   "department":  {"type": "choice", "instructions": "Which team should handle this ticket first?",
                   "criteria": {"returns": "...", "shipping": "...", "billing": "...", "account": "..."},
                   "label": "billing"},
   "escalate":    {"type": "noul", "instructions": "Does this need urgent attention within the hour?", "label": true},
   "frustration": {"type": "score", "instructions": "How frustrated is the customer?",
                   "criteria": ["Calm or neutral", "Annoyed", "Angry or threatening to leave"], "label": 2}}}
```

**Labels per type** (from `kev/data.py:load_records` and `split_data.py`):

| Type | `criteria` | `label` |
|---|---|---|
| `noul` | optional `{"true": ..., "false": ...}` | `true` / `false` |
| `choice` | `{name: description or null}`, 1–255 options | an option **name** |
| `score` | list of 2–255 ordered levels | a level **index** (int from 0) |
| any | — | optional soft `target` `{key: weight}` for undecidable records (teaches "no evidence ⇒ low confidence") |

**Packed heterogeneous records: yes.** One record may contain any number of heterogeneous questions sharing one state — a Choice, a Score and a Noul together is the normal case (the released models trained on multi-question records). `state` may be a string, or a JSON object/array (rendered as `key: value` lines; field names are visible to the model). Each question carries an independent label; the model never sees question ids.

**Format constraints:** whole request ≤ 2,048 tokens, each branch ≤ 1,024; records are dropped if they exceed it. Balanced labels per option (≥ 5% each); duplicate states deduplicated, conflicting labels dropped (`split_data.py`).

## 4. Calibration Workflow

Calibration is a **post-hoc single scalar temperature**, not part of training.

- **Fit:** `kev.metrics.fit_temperature(rows)` minimizes mean NLL over a log grid 0.25–4 (released checkpoints use 121 points, micro aggregation; skill/research trials use the 81-point default). Fit set = the **calibration** partition only.
- **Application:** `PointerHead.forward` divides logits by `temperature` in eval mode; argmax is invariant, so accuracy is unchanged by calibration — only confidence quality moves.
- **Storage:** the fitted value is written to `head.pt["temperature"]`, so *every* loader (`kev.serve`, `kev.benchmark`, the Space, the skill's endpoint) serves calibrated probabilities by default. `KEV_TEMPERATURE=1.0` restores raw logits.
- **Reporting:** `scripts/calibrate_checkpoint.py` reports raw vs calibrated ECE/Brier/NLL/confident-errors and a group-disjoint 5-fold out-of-fold ECE with bootstrap intervals, so an in-sample fit can be checked against held-out records.

The released checkpoints carry temperatures 2.41 (0.8B), 2.14 (4B), 2.30 (9B) — all fitted in-distribution. **These do not transfer to a new domain**: on a new workload, a temperature fitted on the workload's own calibration rows is required (the skill's `train` does this for both the fine-tuned model and the baseline). This is the single most important calibration fact for Argonaut: a specialized model must be *re-calibrated on its own domain*, not inherit the released temperature.

Probability quality is first-class throughout: `metrics()` reports Brier, NLL, ECE (10-bin), `confident_error_rate` (wrong answers with p≥0.9), `coverage_at_{5pct,1pct}_error` (selective automation), AURC, and per-top-bin error rates. A model that predicts the right class but is badly calibrated will fail these — the objective explicitly wants these, and Kev already measures them.

## 5. Model / Checkpoint Options

For a specialization experiment, start from a released checkpoint as the `--init_from` warm start (delta mode). The trainer reads base, LoRA rank, and head size from the init checkpoint and refuses mismatches.

| Option | Checkpoint id | LoRA trainable | Fitted T | Notes |
|---|---|---|---|---|
| **Kev-0.8B** | `jaredpalmer/kev-0.8b` | 11.3M | 2.41 | smallest/fastest; delta lr 4e-5; recommended first loop |
| **Kev-4B** | `jaredpalmer/kev-4b` | 33.8M | 2.14 | recommended general; delta lr 2e-5; ~12–15 min / 400–1000 records on H100 |
| **Kev-9B** | `jaredpalmer/kev-9b` | 45.4M | 2.30 | final quality; ~30 min; needs A100-80GB/H100 to serve |

All are Qwen3.5 hybrid bases. Per the skill's own guidance: use 0.8B for fast loops, 4B as the working model, 9B only when data stops moving the 4B. **Prefer `kev-0.8b` for the first Argonaut run** — it makes the experiment cheap and understandable — then promote to 4B once the data/protocol are proven.

## 6. Hardware / Cost Matrix

Training is delta fine-tuning (warm start), far cheaper than the from-scratch recipe. Figures from `skills/kev-finetune` and `scripts/kev_modal.py`.

| Environment | Feasibility | Runtime (delta, ~400–1000 recs) | Cost | Setup |
|---|---|---|---|---|
| **Modal GPU (H100)** | Recommended | 0.8B ~8 min · 4B ~12–15 min · 9B ~30 min | ~$1–3/run (H100 $3.95/h; T4 $0.59/h free tier) | `uvx modal setup` once |
| **Local NVIDIA GPU** | Yes | similar (slower on smaller cards) | electricity | install torch CUDA + `flash-linear-attention` + `triton>=3.7.1` |
| **Local Apple Silicon (MPS)** | Marginal | slow — no DeltaNet MPS kernels | free | one job at a time |
| **Local CPU** | Smoke only | 0.8B only, slow | free | no GPU |

**Recommendation: Modal H100** for the first run — it is the first-party path (the skill's image clones Kev at a pinned ref and installs the DeltaNet kernels), it bounds cost up front (`train` prints a cost bound), and it needs no local GPU. GPU hourly rates baked into the scripts: H100 $3.95, H200 $4.54, B200 $6.25, A100-80GB $2.50, A100 $2.10, L40S $1.95, A10G $1.10, L4 $0.80, T4 $0.59.

## 7. Proposed First Experiment

**Domain: code-review escalation** (per change-set). Chosen for experimental clarity, not product importance:

- **Labels can be explained** — escalation/owner/risk follow explicit rules a human can write down.
- **Ground truth is inspectable** — a programmatic generator produces *deterministically correct* labels from structured features (no reliance on LLM-judged labels).
- **Natural Noul/Choice/Score semantics** — and, critically, all three in one packed record, exercising exactly the heterogeneous-packing property Argonaut is investigating.

**Decision contract (one state, three packed questions):**

| Question | Type | Options | Label |
|---|---|---|---|
| `escalate` | noul | "Does this change need a senior reviewer before merge?" | `true`/`false` |
| `reviewer` | choice | `{platform, security, data, frontend}` ("Which team should review this?") | option name |
| `risk` | score | `["Trivial", "Low", "Medium", "High"]` ("How risky is this change?") | level index |

**State** = an object Kev renders: `{"title": ..., "description": ..., "files": [...], "tests": ..., "touches_auth": ...}` (or a rendered diff summary).

**Labels via deterministic rules** (source 4 in `data-generation.md`): security ⇐ touches auth/crypto/keys; data ⇐ migrations/queries; risk = f(area, breadth of files, test coverage); escalate ⇐ high risk or security or infra. This yields *perfectly consistent labels* and cheap **minimal pairs** (same change, one fact flipped, label flips) — the strongest teaching signal and a built-in sanity check on what the model attends to. Optionally mix in LLM-paraphrased records (`generate_data.py`) for surface variety, but never let the LLM supply the *label logic*.

Support-ticket routing (the skill's own example, `assets/workload.example.json`, with measured 67.7% → 73.6% results) is the ready fallback if Argonaut prefers to start from a proven recipe.

## 8. Dataset Size (experimentally justified)

From `plan_size.py` (McNemar paired approximation) and `data-format.md`:

| Tier | Records | Purpose | Split |
|---|---|---|---|
| **Smoke** | 100–200 | prove the pipeline end-to-end; scores noisy | 70/15/15 |
| **Minimum useful** | 300–600 | first real signal; bootstrap CIs begin to separate | 70/15/15 |
| **Meaningful** | ~1,000 | detect +5 accuracy points at 80% power with 3 questions/record | 70/15/15 |

`plan_size.py workload.json --baseline-acc <measured> --min-gain 0.05` computes the exact number; the target of "+5 points at 80% power, three questions per record" resolves to ~1,000 records (~$0.25 with gpt-4.1-mini, ~$0 with a programmatic generator). Calibration needs ≥ 100 questions (≈ 34+ records at 3 questions/record) for a stable temperature fit.

**Partition policy (non-negotiable):**

- **train / calibration / development** — split by *state hash* (`split_data.py`), so a state never straddles partitions. Default 70/15/15.
- **Locked test** — a separate, held-back file (ideally real data) read at most once per candidate. **Never fit the temperature on it, never tune against it.**
- Never evaluate on training data; never reuse development as a search set across more than a couple of rounds.

## 9. Baseline Protocol

Before training, execute the chosen released checkpoint against the **locked** evaluation set and record, per `kev.benchmark`/`metrics()`:

- accuracy, Brier, NLL, ECE (raw and temperature-fitted), `confident_error_rate`, `coverage_at_5pct_error`
- per-question-type performance (`grouped_metrics` by `type`, and by question id)
- latency (median/p95) if relevant

Use `kev.benchmark --run jaredpalmer/kev-0.8b --data <locked>.jsonl` (or `kev_modal.py::evaluate`) to produce `rows.json` + `report.json`; **store these as the frozen baseline** (rows preserve logits, so any temperature can be re-applied later). The baseline's carried temperature was fitted on *public* data — the skill refits it on the domain's calibration slice so the "baseline calibrated" number is the fair zero-shot figure. Both sides of the comparison must be temperature-fitted on the *same* calibration slice, or the calibration advantage is confounded with the training effect.

## 10. Training Protocol (first run)

Start minimal; do not copy the full from-scratch recipe.

| Setting | Value | Rationale |
|---|---|---|
| `--init_from` | `jaredpalmer/kev-0.8b` | smallest checkpoint, cheapest loop |
| dataset | ~300–600 records (or ~1,000 if generator is programmatic) | first meaningful tier |
| `--epochs` | 1 | deltas use one epoch |
| `--lr` | 4e-5 (the 0.8B delta lr; cap 5e-5) | read from init checkpoint; halve on regression |
| LoRA | rank 16 (read from init) | fixed by the checkpoint |
| `--batch` / `--accum` | 1 / 8 (or the init's args) | small padded batches |
| `--dtype` | bf16 | autocast, CUDA |
| `--replay` | 2000 (public `decision-v7` mix) | the forgetting guard; drop to 500 for large data, 0 only for throwaway |
| device | Modal H100 | first-party path |

Estimated: **~8 minutes, ~$1** on an H100 for 0.8B. The skill's `train` runs this as `modal run scripts/kev_modal.py::train --data data/x --name x-v1 --init-from jaredpalmer/kev-0.8b`, and automatically fits the temperature on calibration and scores the baseline on the same development file.

## 11. Evaluation & Regression Control

Compare fine-tuned vs baseline on the **same locked set**:

- accuracy, Brier, ECE, `confident_error_rate`, `coverage_at_5pct_error`, per-question-type and per-question metrics, paired bootstrap (95% CI) on acc/Brier/ECE.
- **In-domain** = the domain's locked set. **Out-of-domain** = a control unrelated to the domain (see below).

**Success is not improved training accuracy.** It is a paired-bootstrap accuracy/Brier gain on the *held-out* domain set **with** `confident_error_rate` no worse than baseline and no out-of-domain regression.

**Regression-control strategy** (the "did we teach it the domain, or just make it confident and less capable?" question):

1. **Public forgetting check (built in).** The skill scores both models on a 300-record `decision-v7` development sample; accept a drop of ~2 accuracy points, more means drift (lower `--lr`, keep `--replay`).
2. **Explicit unrelated-domain control (add this).** Keep a small out-of-domain probe the domain never touches — e.g. 100–200 records from a *different* decision domain (support-ticket routing, or a knowledge probe like a SciQ/MMLU slice) — scored on baseline and fine-tuned. This isolates *general decision capability* from *domain skill*: a fine-tune that lifts in-domain accuracy but tanks the control has overfit, not specialized.
3. **Calibration honesty.** Require calibrated `confident_error_rate` (fine-tuned) ≤ baseline, and `mean_conf` within a few points of accuracy; a confident-error spike usually means memorized near-duplicates or too many epochs.

This maps cleanly onto `kev.experiment`'s existing gate vocabulary (`task_accuracy_regression`, `transfer_accuracy_regression`, `transfer_confident_errors_max`, `heldout_pairs_min`) even though the skill path exposes it as `regression` + the paired bootstrap.

## 12. Reproducibility Requirements

Record, at minimum:

| Field | Kev-native source |
|---|---|
| base checkpoint | `init_from` + resolved `--init-from` |
| base revision | `checkpoint.Meta.base_revision` |
| Kev commit | `result.json["kev_ref"]` (pinned; `config.json` also records it) |
| dataset hash | `split_data.py` `summary.json`; `result.json` data counts |
| train/val/test split | `summary.json` partitions + `seed` |
| random seed | `--seed` in `config.json` |
| training parameters | `config.json` (resolved) + `training_config.json` |
| result metrics | `result.json` (raw + calibrated, bootstrap, regression) |
| output checkpoint | run dir on the volume / `pull --checkpoint` |

Kev's *research* runner (`kev.experiment`) goes further — `provenance.json` records `config_sha256`, `suite_sha256`, hashes of every `kev/*.py`, `git_commit`, torch version, device, GPU name, and `head.pt`/`adapter` sha256 — and refuses to resume if source or suite changes. **Prefer Kev's existing metadata rather than inventing a parallel system**; the skill path covers everything above except per-source-code hashes, which are available by invoking `kev.experiment.source_hashes()` if a stronger guarantee is wanted.

## 13. Serving Strategy

A fine-tuned checkpoint is served exactly like a released one — `kev.serve --run <checkpoint>` or the skill's Modal endpoint — and the resulting model still speaks `POST /v1/systemone`. **The Argonaut client needs no semantic change; only the base URL / model id / key change:**

- Modal: `KEV_SERVE_RUN=x-v1 modal deploy scripts/kev_modal.py` → a TypeSafe-compatible URL; `TypeSafeClient(base_url=URL, api_key=..., model="kev-latest")`.
- Local: `pull --checkpoint` then `kev.serve --run runs/x-v1/checkpoint`.
- Optional Hub publish (`kev.publish --private`) makes the checkpoint addressable as `you/kev-4b-x` anywhere a Kev id works.

Two constraints to honor: (a) instructions and option names must exactly match training (the fine-tune binds those strings); (b) the fitted temperature ships inside `head.pt`, so the served probabilities are already domain-calibrated — but re-read thresholds after every retrain. Serving GPU: L4 for 0.8B/4B, A100-80GB/H100 for 9B. This is verified to be true by the skill's own `evaluate --remote` step, which re-scores the deployed endpoint and checks the served numbers match the offline score.

## 14. Relationship to System One / Argonaut Evaluation

The specialization experiment is a drop-in producer for the future cross-model comparison. `kev.benchmark --remote <url>` (`RemotePredictor`) and `kev.jev` already score *any* System One-compatible endpoint on the same labelled records, so a later harness can hold a frozen workload and run:

```
TypeSafe Jev            (kev.jev / RemotePredictor against the Jev gateway)
Kev baseline (released) (kev.benchmark --run jaredpalmer/kev-*)
Kev fine-tuned          (kev.benchmark --run <checkpoint> or --remote <deployed>)
Kev larger checkpoint   (kev.benchmark --run jaredpalmer/kev-9b)
```

all on the same locked set, using `api_request()` to strip labels/targets/metadata before anything leaves the machine. The only prerequisite is that the locked set be kept frozen and never tuned against — which this protocol enforces. **No such integration is implemented in this task** (per instructions); the deliverable is the protocol that makes it possible.

## 15. Recommended Next Task

A single, bounded implementation slice (not a full training run) — to be authorized separately:

1. **Freeze the decision contract + locked set** for code-review escalation: write `workload.json` (instructions/options/guidance), a programmatic generator for ~300–600 labelled records with deterministic labels + minimal pairs, and a small locked hold-out (plus the out-of-domain control slice). Run `plan_size.py` and `split_data.py`; validate on Modal (`kev_modal.py::validate`).
2. **Record the frozen baseline**: `evaluate --run jaredpalmer/kev-0.8b` (or `kev.benchmark`) on the locked set + control; commit the `rows.json`/`report.json`.
3. **One training run**: `modal run scripts/kev_modal.py::train --data data/x --name x-v1 --init-from jaredpalmer/kev-0.8b` (epochs 1, lr 4e-5, replay 2000).
4. **Evaluate**: baseline-vs-fine-tuned on the locked set + regression + control; decide on the paired bootstrap, confident errors, and no-regression gate (reusing `references/hill-climbing.md`).

The fine-tuning run itself is deliberately **not** executed here; it awaits explicit authorization.
