#!/usr/bin/env python3
"""
Argonaut Vector Golden Retrieval Evaluator v0.1

Evaluates retrieval quality across one or more vector providers using the
controlled synthetic corpus and golden queries.

Usage:
    python3 evaluate_retrieval.py --provider IN_MEMORY
    python3 evaluate_retrieval.py --provider QDRANT
    python3 evaluate_retrieval.py --provider ALL

See README.md for corpus preconditions and metric definitions.
"""

import argparse
import json
import math
import sys
import textwrap
from datetime import datetime, timezone
from pathlib import Path

import requests

from metrics import per_query_metrics, aggregate_metrics

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
CORPUS_VERSION = "v0.1"
CORPUS_BASE = REPO_ROOT / "docs/knowledge/vector-synthetic-corpus" / CORPUS_VERSION

GOLDEN_QUERIES_PATH = CORPUS_BASE / "queries/golden-queries.jsonl"
RELEVANCE_TSV_PATH = CORPUS_BASE / "qrels/relevance.tsv"

RESULTS_BASE = Path(__file__).resolve().parent / "results"

ALL_PROVIDERS = ["IN_MEMORY", "INTEGRALLIS", "QDRANT"]
PROVIDER_DIR = {
    "IN_MEMORY": "in-memory",
    "INTEGRALLIS": "integrallis",
    "QDRANT": "qdrant",
}

NO_ANSWER_QUERY_ID = "Q025"
SEARCH_LIMIT = 5

# Preflight: a known document ID that must appear in a loaded corpus.
# Any query that reliably retrieves a specific DOC is fine for this.
PREFLIGHT_QUERY = "HNSW graph navigation vector index"
PREFLIGHT_EXPECTED_DOC = "DOC-001"


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

def load_queries(path: Path) -> list[dict]:
    queries = []
    with path.open() as f:
        for line in f:
            line = line.strip()
            if line:
                queries.append(json.loads(line))
    return queries


def load_qrels(path: Path) -> dict[str, dict[str, int]]:
    """Returns {query_id: {doc_id: relevance_grade}}."""
    qrels: dict[str, dict[str, int]] = {}
    with path.open() as f:
        header = True
        for line in f:
            if header:
                header = False
                continue
            line = line.strip()
            if not line:
                continue
            parts = line.split("\t")
            if len(parts) != 3:
                continue
            qid, doc_id, grade = parts[0], parts[1], int(parts[2])
            qrels.setdefault(qid, {})[doc_id] = grade
    return qrels


# ---------------------------------------------------------------------------
# API calls
# ---------------------------------------------------------------------------

def set_provider(base_url: str, provider: str) -> None:
    resp = requests.put(
        f"{base_url}/api/vector/provider",
        json={"provider": provider},
        timeout=10,
    )
    if not resp.ok:
        print(f"ERROR: Failed to set provider to {provider}: {resp.status_code} {resp.text}",
              file=sys.stderr)
        sys.exit(1)


def get_active_provider(base_url: str) -> str:
    resp = requests.get(f"{base_url}/api/vector/provider", timeout=10)
    resp.raise_for_status()
    return resp.json()["active"]


def search(base_url: str, query: str, limit: int = SEARCH_LIMIT) -> list[dict]:
    resp = requests.post(
        f"{base_url}/api/vector/search",
        json={"query": query, "limit": limit},
        timeout=30,
    )
    resp.raise_for_status()
    return resp.json()


# ---------------------------------------------------------------------------
# Preflight check
# ---------------------------------------------------------------------------

def preflight_check(base_url: str, provider: str) -> None:
    """
    Verify that the selected provider appears to contain the synthetic corpus.
    Uses a known query and checks if a known doc appears in results.
    """
    try:
        results = search(base_url, PREFLIGHT_QUERY, limit=5)
    except Exception as e:
        print(f"ERROR: Preflight search failed: {e}", file=sys.stderr)
        sys.exit(1)

    returned_ids = [r["id"] for r in results]
    if PREFLIGHT_EXPECTED_DOC not in returned_ids:
        print(
            f"ERROR: Selected provider {provider} does not appear to contain "
            f"synthetic corpus {CORPUS_VERSION}.\n"
            f"       Expected '{PREFLIGHT_EXPECTED_DOC}' in results for a known query.\n"
            f"       Got: {returned_ids}\n"
            f"       Load the corpus before running evaluation.",
            file=sys.stderr,
        )
        sys.exit(1)


# ---------------------------------------------------------------------------
# Evaluation
# ---------------------------------------------------------------------------

def evaluate_provider(base_url: str, provider: str, queries: list[dict],
                      qrels: dict[str, dict[str, int]]) -> dict:
    """Run all golden queries against the provider and return raw result data."""

    print(f"\n[{provider}] Setting provider...", flush=True)
    set_provider(base_url, provider)

    active = get_active_provider(base_url)
    if active != provider:
        print(f"ERROR: Provider verification failed. Expected {provider}, got {active}.",
              file=sys.stderr)
        sys.exit(1)
    print(f"[{provider}] Active provider confirmed: {active}", flush=True)

    print(f"[{provider}] Running preflight corpus check...", flush=True)
    preflight_check(base_url, provider)
    print(f"[{provider}] Corpus check passed.", flush=True)

    query_results = []
    no_answer_results = []

    for q in queries:
        qid = q["id"]
        query_text = q["query"]
        categories = q.get("categories", [])
        is_no_answer = qid == NO_ANSWER_QUERY_ID or "no-answer" in categories

        print(f"[{provider}] {qid}: {query_text[:60]}...", flush=True)

        raw_results = search(base_url, query_text, limit=SEARCH_LIMIT)
        ranked_ids = [r["id"] for r in raw_results]
        ranked_with_scores = [{"rank": i + 1, "id": r["id"], "score": r["score"]}
                               for i, r in enumerate(raw_results)]

        if is_no_answer:
            no_answer_results.append({
                "query_id": qid,
                "query": query_text,
                "categories": categories,
                "notes": q.get("notes", ""),
                "expected_relevant_docs": "none",
                "returned_docs": ranked_with_scores,
                "top_score": raw_results[0]["score"] if raw_results else None,
            })
            continue

        query_qrels = qrels.get(qid, {})
        metrics = per_query_metrics(ranked_ids, query_qrels)

        query_results.append({
            "query_id": qid,
            "query": query_text,
            "categories": categories,
            "notes": q.get("notes", ""),
            "expected_qrels": query_qrels,
            "actual_results": ranked_with_scores,
            "metrics": metrics,
        })

    answerable = query_results
    agg = aggregate_metrics([r["metrics"] for r in answerable])

    # Category breakdown: queries may belong to multiple categories
    category_map: dict[str, list[dict]] = {}
    for r in answerable:
        for cat in r["categories"]:
            category_map.setdefault(cat, []).append(r["metrics"])

    category_agg = {
        cat: {**aggregate_metrics(ms), "query_count": len(ms)}
        for cat, ms in sorted(category_map.items())
    }

    return {
        "provider": provider,
        "corpus_version": CORPUS_VERSION,
        "evaluated_at": datetime.now(timezone.utc).isoformat(),
        "query_count": len(queries),
        "answerable_query_count": len(answerable),
        "no_answer_query_count": len(no_answer_results),
        "aggregate_metrics": agg,
        "category_metrics": category_agg,
        "query_results": answerable,
        "no_answer_results": no_answer_results,
    }


# ---------------------------------------------------------------------------
# Report generation
# ---------------------------------------------------------------------------

def format_text_report(data: dict) -> str:
    lines = []
    provider = data["provider"]
    agg = data["aggregate_metrics"]
    cat = data["category_metrics"]

    lines.append("=" * 72)
    lines.append("Argonaut Vector Golden Evaluation")
    lines.append(f"Provider:          {provider}")
    lines.append(f"Corpus:            synthetic-{data['corpus_version']}")
    lines.append(f"Evaluated at:      {data['evaluated_at']}")
    lines.append(f"Queries:           {data['query_count']}")
    lines.append(f"Answerable:        {data['answerable_query_count']}")
    lines.append(f"No-answer:         {data['no_answer_query_count']}")
    lines.append("")

    lines.append("Aggregate Metrics (answerable queries only)")
    lines.append("-" * 48)
    lines.append(f"  Recall@1:  {agg.get('recall_at_1', 0):.4f}")
    lines.append(f"  Recall@3:  {agg.get('recall_at_3', 0):.4f}")
    lines.append(f"  Recall@5:  {agg.get('recall_at_5', 0):.4f}")
    lines.append(f"  MRR:       {agg.get('mrr', 0):.4f}")
    lines.append(f"  nDCG@5:    {agg.get('ndcg_at_5', 0):.4f}")
    lines.append("")

    # Category breakdown
    lines.append("Category Breakdown")
    lines.append("-" * 72)
    header = f"{'Category':<42} {'Q':>4}  {'R@1':>6}  {'R@3':>6}  {'R@5':>6}  {'MRR':>6}  {'nDCG@5':>7}"
    lines.append(header)
    lines.append("-" * 72)
    for cat_name, cm in cat.items():
        row = (
            f"{cat_name:<42} {cm['query_count']:>4}"
            f"  {cm.get('recall_at_1', 0):>6.3f}"
            f"  {cm.get('recall_at_3', 0):>6.3f}"
            f"  {cm.get('recall_at_5', 0):>6.3f}"
            f"  {cm.get('mrr', 0):>6.3f}"
            f"  {cm.get('ndcg_at_5', 0):>7.3f}"
        )
        lines.append(row)
    lines.append("")

    # Per-query detail
    lines.append("Per-Query Results")
    lines.append("=" * 72)
    for r in data["query_results"]:
        qid = r["query_id"]
        lines.append(f"\n{qid}")
        lines.append(f"Query:      {r['query']}")
        lines.append(f"Categories: {', '.join(r['categories'])}")
        lines.append("")
        lines.append("Expected:")
        if r["expected_qrels"]:
            for doc_id, grade in sorted(r["expected_qrels"].items(),
                                        key=lambda x: -x[1]):
                lines.append(f"  {doc_id}  relevance={grade}")
        else:
            lines.append("  (none)")
        lines.append("")
        lines.append("Actual (Top-5):")
        for res in r["actual_results"]:
            marker = "*" if res["id"] in r["expected_qrels"] else " "
            grade = r["expected_qrels"].get(res["id"], 0)
            grade_str = f" [rel={grade}]" if res["id"] in r["expected_qrels"] else ""
            lines.append(
                f"  #{res['rank']:1d} {marker} {res['id']}  score={res['score']:.6f}{grade_str}"
            )
        lines.append("")
        m = r["metrics"]
        lines.append("Metrics:")
        lines.append(f"  Recall@1:  {m['recall_at_1']:.4f}")
        lines.append(f"  Recall@3:  {m['recall_at_3']:.4f}")
        lines.append(f"  Recall@5:  {m['recall_at_5']:.4f}")
        lines.append(f"  MRR:       {m['mrr']:.4f}")
        lines.append(f"  nDCG@5:    {m['ndcg_at_5']:.4f}")
        lines.append("-" * 48)

    # No-answer section
    lines.append("")
    lines.append("No-Answer / Abstention Behavior")
    lines.append("=" * 72)
    lines.append(
        "Note: The vector search API always returns nearest neighbors.\n"
        "      These queries have no relevant documents. Results below are\n"
        "      informational only and are excluded from aggregate metrics."
    )
    lines.append("")
    for r in data["no_answer_results"]:
        lines.append(f"{r['query_id']}: {r['query']}")
        lines.append(f"  Expected relevant docs: {r['expected_relevant_docs']}")
        lines.append(f"  Top score: {r['top_score']:.6f}" if r["top_score"] is not None else "  No results returned")
        lines.append("  Returned:")
        for res in r["returned_docs"]:
            lines.append(f"    #{res['rank']:1d} {res['id']}  score={res['score']:.6f}")
        lines.append("")

    return "\n".join(lines)


def format_comparison_report(all_data: dict[str, dict]) -> str:
    lines = []
    lines.append("=" * 72)
    lines.append("Argonaut Vector — Provider Comparison")
    lines.append(f"Corpus: synthetic-{CORPUS_VERSION}")
    lines.append("")

    lines.append(f"{'Provider':<14} {'R@1':>6}  {'R@3':>6}  {'R@5':>6}  {'MRR':>6}  {'nDCG@5':>7}")
    lines.append("-" * 60)
    for provider, data in all_data.items():
        agg = data["aggregate_metrics"]
        lines.append(
            f"{provider:<14}"
            f"  {agg.get('recall_at_1', 0):>6.4f}"
            f"  {agg.get('recall_at_3', 0):>6.4f}"
            f"  {agg.get('recall_at_5', 0):>6.4f}"
            f"  {agg.get('mrr', 0):>6.4f}"
            f"  {agg.get('ndcg_at_5', 0):>7.4f}"
        )
    lines.append("")

    # Ranking divergence analysis
    providers = list(all_data.keys())
    if len(providers) < 2:
        return "\n".join(lines)

    # Use first provider's queries as reference
    ref_provider = providers[0]
    ref_queries = {r["query_id"]: [x["id"] for x in r["actual_results"]]
                   for r in all_data[ref_provider]["query_results"]}
    all_query_ids = list(ref_queries.keys())

    divergent = []
    identical = []
    for qid in all_query_ids:
        ref_top5 = ref_queries.get(qid, [])
        same = all(
            [x["id"] for x in all_data[p]["query_results"]
             if x["query_id"] == qid][0:1] == ref_top5[:1]  # quick check
            if any(x["query_id"] == qid for x in all_data[p]["query_results"])
            else False
            for p in providers[1:]
        )
        # Full top-5 comparison
        rankings = {}
        for p in providers:
            qr = next((r for r in all_data[p]["query_results"] if r["query_id"] == qid), None)
            if qr:
                rankings[p] = [x["id"] for x in qr["actual_results"]]
        all_same = len(set(tuple(v) for v in rankings.values())) == 1
        if all_same:
            identical.append(qid)
        else:
            divergent.append(qid)

    lines.append("Ranking Divergence (Top-5 across all providers)")
    lines.append("-" * 50)
    lines.append(f"Identical Top-5: {len(identical)}/{len(all_query_ids)}")
    if divergent:
        lines.append(f"Divergent:       {', '.join(divergent)}")
    else:
        lines.append("Divergent:       none")
    lines.append("")
    lines.append(
        "Score semantics note: Provider scores are NOT directly comparable.\n"
        "  IN_MEMORY / Qdrant:  raw cosine similarity\n"
        "  Integrallis:         (1 + cosine) / 2\n"
        "All metrics above are based on ranking, not score thresholds."
    )

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------

def write_results(provider: str, data: dict) -> None:
    dir_name = PROVIDER_DIR[provider]
    out_dir = RESULTS_BASE / dir_name
    out_dir.mkdir(parents=True, exist_ok=True)

    json_path = out_dir / "golden-v0.1.json"
    txt_path = out_dir / "golden-v0.1.txt"

    with json_path.open("w") as f:
        json.dump(data, f, indent=2)

    with txt_path.open("w") as f:
        f.write(format_text_report(data))

    print(f"[{provider}] Results written to {out_dir}/")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Argonaut Vector Golden Retrieval Evaluator v0.1",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent("""
            Examples:
              python3 evaluate_retrieval.py --provider IN_MEMORY
              python3 evaluate_retrieval.py --provider QDRANT --base-url http://localhost:8086
              python3 evaluate_retrieval.py --provider ALL
        """),
    )
    parser.add_argument(
        "--provider",
        default="IN_MEMORY",
        choices=ALL_PROVIDERS + ["ALL"],
        help="Vector provider to evaluate (default: IN_MEMORY)",
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8086",
        help="Argonaut Vector base URL (default: http://localhost:8086)",
    )
    args = parser.parse_args()

    # Validate input files
    for path, label in [(GOLDEN_QUERIES_PATH, "golden queries"), (RELEVANCE_TSV_PATH, "qrels")]:
        if not path.exists():
            print(f"ERROR: {label} file not found: {path}", file=sys.stderr)
            sys.exit(1)

    queries = load_queries(GOLDEN_QUERIES_PATH)
    qrels = load_qrels(RELEVANCE_TSV_PATH)

    print(f"Loaded {len(queries)} queries, {sum(len(v) for v in qrels.values())} qrel judgments")

    providers = ALL_PROVIDERS if args.provider == "ALL" else [args.provider]

    all_results: dict[str, dict] = {}
    for provider in providers:
        data = evaluate_provider(args.base_url, provider, queries, qrels)
        write_results(provider, data)
        all_results[provider] = data

        agg = data["aggregate_metrics"]
        print(f"\n[{provider}] Summary:")
        print(f"  Recall@1:  {agg.get('recall_at_1', 0):.4f}")
        print(f"  Recall@3:  {agg.get('recall_at_3', 0):.4f}")
        print(f"  Recall@5:  {agg.get('recall_at_5', 0):.4f}")
        print(f"  MRR:       {agg.get('mrr', 0):.4f}")
        print(f"  nDCG@5:    {agg.get('ndcg_at_5', 0):.4f}")

    if len(all_results) >= 2:
        comparison = format_comparison_report(all_results)
        comp_path = RESULTS_BASE / "provider-comparison.txt"
        with comp_path.open("w") as f:
            f.write(comparison)
        print(f"\nComparison report: {comp_path}")
        print(comparison)


if __name__ == "__main__":
    main()
