"""Unit tests for retrieval metric calculations."""

import math
import unittest

from metrics import (
    recall_at_k,
    reciprocal_rank,
    ndcg_at_k,
    per_query_metrics,
    aggregate_metrics,
)


class TestRecallAtK(unittest.TestCase):

    def test_perfect_recall_at_1(self):
        self.assertEqual(recall_at_k(["A", "B", "C"], {"A"}, 1), 1.0)

    def test_miss_at_1_hit_at_3(self):
        self.assertEqual(recall_at_k(["B", "C", "A"], {"A"}, 1), 0.0)
        self.assertEqual(recall_at_k(["B", "C", "A"], {"A"}, 3), 1.0)

    def test_partial_recall(self):
        # 1 of 2 relevant docs in top-3
        result = recall_at_k(["A", "X", "Y"], {"A", "B"}, 3)
        self.assertAlmostEqual(result, 0.5)

    def test_multiple_relevant_docs(self):
        # Both relevant in top-3
        result = recall_at_k(["A", "B", "X"], {"A", "B"}, 3)
        self.assertEqual(result, 1.0)

    def test_no_relevant_docs(self):
        self.assertEqual(recall_at_k(["A", "B"], set(), 5), 0.0)

    def test_empty_retrieved(self):
        self.assertEqual(recall_at_k([], {"A"}, 5), 0.0)

    def test_k_larger_than_retrieved(self):
        # k=5 but only 2 results; only count what exists
        self.assertEqual(recall_at_k(["A", "B"], {"A"}, 5), 1.0)

    def test_recall_at_3_vs_at_5(self):
        retrieved = ["X", "Y", "Z", "A", "B"]
        relevant = {"A", "B"}
        self.assertEqual(recall_at_k(retrieved, relevant, 3), 0.0)
        self.assertEqual(recall_at_k(retrieved, relevant, 5), 1.0)


class TestReciprocalRank(unittest.TestCase):

    def test_first_result_relevant(self):
        self.assertEqual(reciprocal_rank(["A", "B", "C"], {"A"}), 1.0)

    def test_second_result_relevant(self):
        self.assertAlmostEqual(reciprocal_rank(["X", "A", "B"], {"A"}), 0.5)

    def test_third_result_relevant(self):
        self.assertAlmostEqual(reciprocal_rank(["X", "Y", "A"], {"A"}), 1 / 3)

    def test_no_relevant_in_results(self):
        self.assertEqual(reciprocal_rank(["X", "Y", "Z"], {"A"}), 0.0)

    def test_empty_retrieved(self):
        self.assertEqual(reciprocal_rank([], {"A"}), 0.0)

    def test_multiple_relevant_uses_first(self):
        # A is at rank 2, B at rank 3 — RR should be based on A (rank 2)
        self.assertAlmostEqual(reciprocal_rank(["X", "A", "B"], {"A", "B"}), 0.5)

    def test_empty_relevant(self):
        self.assertEqual(reciprocal_rank(["A", "B"], set()), 0.0)


class TestNdcgAtK(unittest.TestCase):

    def test_perfect_ndcg(self):
        # Only one relevant doc, it's first
        qrels = {"A": 3}
        result = ndcg_at_k(["A", "B", "C", "D", "E"], qrels, 5)
        self.assertAlmostEqual(result, 1.0)

    def test_perfect_ndcg_two_docs(self):
        qrels = {"A": 3, "B": 2}
        result = ndcg_at_k(["A", "B", "X", "Y", "Z"], qrels, 5)
        self.assertAlmostEqual(result, 1.0)

    def test_reversed_order_lower_ndcg(self):
        qrels = {"A": 3, "B": 1}
        ideal = ndcg_at_k(["A", "B", "X", "Y", "Z"], qrels, 5)
        reversed_ = ndcg_at_k(["B", "A", "X", "Y", "Z"], qrels, 5)
        self.assertAlmostEqual(ideal, 1.0)
        self.assertLess(reversed_, 1.0)

    def test_no_relevant_docs(self):
        result = ndcg_at_k(["A", "B", "C"], {}, 5)
        self.assertEqual(result, 0.0)

    def test_empty_retrieved(self):
        qrels = {"A": 3}
        result = ndcg_at_k([], qrels, 5)
        self.assertEqual(result, 0.0)

    def test_graded_relevance_ordering(self):
        # Grade 3 doc at rank 1 vs grade 1 doc at rank 1
        qrels = {"A": 3, "B": 1}
        # ideal: A first (grade 3), B second (grade 1)
        result = ndcg_at_k(["A", "B"], qrels, 5)
        self.assertAlmostEqual(result, 1.0)
        # sub-optimal: B first, A second
        sub = ndcg_at_k(["B", "A"], qrels, 5)
        self.assertLess(sub, 1.0)

    def test_missing_expected_document(self):
        # Expected A (grade 3) not returned at all
        qrels = {"A": 3}
        result = ndcg_at_k(["X", "Y", "Z", "W", "V"], qrels, 5)
        self.assertAlmostEqual(result, 0.0)


class TestNoAnswerQuery(unittest.TestCase):
    """Q025-style: no relevant documents exist."""

    def test_recall_no_answer(self):
        # No qrels -> empty relevant set
        self.assertEqual(recall_at_k(["DOC-001", "DOC-002"], set(), 5), 0.0)

    def test_mrr_no_answer(self):
        self.assertEqual(reciprocal_rank(["DOC-001", "DOC-002"], set()), 0.0)

    def test_ndcg_no_answer(self):
        # No qrels -> IDCG = 0 -> result = 0.0, not NaN
        self.assertEqual(ndcg_at_k(["DOC-001", "DOC-002"], {}, 5), 0.0)


class TestPerQueryMetrics(unittest.TestCase):

    def test_all_keys_present(self):
        qrels = {"A": 3, "B": 1}
        result = per_query_metrics(["A", "B"], qrels)
        self.assertIn("recall_at_1", result)
        self.assertIn("recall_at_3", result)
        self.assertIn("recall_at_5", result)
        self.assertIn("mrr", result)
        self.assertIn("ndcg_at_5", result)

    def test_empty_results_empty_qrels(self):
        result = per_query_metrics([], {})
        self.assertEqual(result["recall_at_1"], 0.0)
        self.assertEqual(result["mrr"], 0.0)
        self.assertEqual(result["ndcg_at_5"], 0.0)


class TestAggregateMetrics(unittest.TestCase):

    def test_mean_calculation(self):
        data = [
            {"recall_at_1": 1.0, "mrr": 1.0, "ndcg_at_5": 1.0,
             "recall_at_3": 1.0, "recall_at_5": 1.0},
            {"recall_at_1": 0.0, "mrr": 0.5, "ndcg_at_5": 0.5,
             "recall_at_3": 0.0, "recall_at_5": 0.0},
        ]
        agg = aggregate_metrics(data)
        self.assertAlmostEqual(agg["recall_at_1"], 0.5)
        self.assertAlmostEqual(agg["mrr"], 0.75)
        self.assertAlmostEqual(agg["ndcg_at_5"], 0.75)

    def test_empty_input(self):
        self.assertEqual(aggregate_metrics([]), {})


if __name__ == "__main__":
    unittest.main()
