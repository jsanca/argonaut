"""
Pure retrieval metric calculations.

Binary relevance (for Recall@K and MRR): grade > 0
Graded relevance (for nDCG@K): full integer grade
"""

import math
from typing import Sequence


def recall_at_k(retrieved: Sequence[str], relevant: set[str], k: int) -> float:
    """
    Fraction of relevant documents found in the top-k retrieved results.

    relevant: set of document IDs with grade > 0
    """
    if not relevant:
        return 0.0
    top_k = set(retrieved[:k])
    return len(top_k & relevant) / len(relevant)


def reciprocal_rank(retrieved: Sequence[str], relevant: set[str]) -> float:
    """
    Reciprocal rank of the first relevant document in the retrieved list.
    Returns 0.0 if no relevant document appears.
    """
    for i, doc_id in enumerate(retrieved, start=1):
        if doc_id in relevant:
            return 1.0 / i
    return 0.0


def dcg_at_k(retrieved: Sequence[str], qrels: dict[str, int], k: int) -> float:
    """Discounted Cumulative Gain at k using graded relevance."""
    dcg = 0.0
    for i, doc_id in enumerate(retrieved[:k], start=1):
        grade = qrels.get(doc_id, 0)
        dcg += grade / math.log2(i + 1)
    return dcg


def ideal_dcg_at_k(qrels: dict[str, int], k: int) -> float:
    """Ideal DCG: grades sorted descending, truncated at k."""
    sorted_grades = sorted(qrels.values(), reverse=True)[:k]
    return sum(g / math.log2(i + 2) for i, g in enumerate(sorted_grades))


def ndcg_at_k(retrieved: Sequence[str], qrels: dict[str, int], k: int) -> float:
    """
    Normalized Discounted Cumulative Gain at k.

    qrels: mapping of doc_id -> relevance grade (0 = irrelevant, implied by absence)
    Returns 0.0 when ideal DCG is 0 (no relevant docs).
    """
    idcg = ideal_dcg_at_k(qrels, k)
    if idcg == 0.0:
        return 0.0
    return dcg_at_k(retrieved, qrels, k) / idcg


def per_query_metrics(
    retrieved: Sequence[str],
    qrels: dict[str, int],
) -> dict[str, float]:
    """
    Compute all standard metrics for a single query.

    qrels: doc_id -> grade mapping (grade > 0 = relevant for binary metrics)
    """
    relevant = {doc_id for doc_id, grade in qrels.items() if grade > 0}
    return {
        "recall_at_1": recall_at_k(retrieved, relevant, 1),
        "recall_at_3": recall_at_k(retrieved, relevant, 3),
        "recall_at_5": recall_at_k(retrieved, relevant, 5),
        "mrr": reciprocal_rank(retrieved, relevant),
        "ndcg_at_5": ndcg_at_k(retrieved, qrels, 5),
    }


def aggregate_metrics(per_query: list[dict[str, float]]) -> dict[str, float]:
    """Mean of each metric across all queries."""
    if not per_query:
        return {}
    keys = per_query[0].keys()
    return {k: sum(q[k] for q in per_query) / len(per_query) for k in keys}
