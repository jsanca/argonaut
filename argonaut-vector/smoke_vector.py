#!/usr/bin/env python3

import argparse
import json
from pathlib import Path
from urllib import request, error

BASE_URL = "http://localhost:8086"
PROVIDER_ENDPOINT = f"{BASE_URL}/api/vector/provider"
SEARCH_ENDPOINT = f"{BASE_URL}/api/vector/search"

TESTS = [
    {
        "id": "SMOKE-SEMANTIC",
        "description": "Semantic retrieval / startup probe",
        "query": "How should Kubernetes handle applications that take a long time to initialize?",
        "limit": 5,
    },
    {
        "id": "SMOKE-LEXICAL",
        "description": "Exact identifier / configuration property",
        "query": "management.endpoint.health.probes.enabled",
        "limit": 5,
    },
    {
        "id": "SMOKE-NO-ANSWER",
        "description": "Query intentionally unrelated to the corpus",
        "query": "What is the recommended tire pressure for a 2024 Toyota Corolla?",
        "limit": 5,
    },
]


def get_json(url: str):
    req = request.Request(url, method="GET")

    try:
        with request.urlopen(req, timeout=30) as response:
            return response.status, json.loads(
                response.read().decode("utf-8")
            )
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8")
        raise RuntimeError(f"HTTP {exc.code}: {body}") from exc
    except error.URLError as exc:
        raise RuntimeError(f"Cannot reach {url}: {exc}") from exc


def put_json(url: str, payload: dict):
    data = json.dumps(payload).encode("utf-8")

    req = request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="PUT",
    )

    try:
        with request.urlopen(req, timeout=30) as response:
            return response.status, json.loads(
                response.read().decode("utf-8")
            )
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8")
        raise RuntimeError(f"HTTP {exc.code}: {body}") from exc
    except error.URLError as exc:
        raise RuntimeError(f"Cannot reach {url}: {exc}") from exc


def post_json(url: str, payload: dict):
    data = json.dumps(payload).encode("utf-8")

    req = request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with request.urlopen(req, timeout=30) as response:
            return response.status, json.loads(
                response.read().decode("utf-8")
            )
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8")
        raise RuntimeError(f"HTTP {exc.code}: {body}") from exc
    except error.URLError as exc:
        raise RuntimeError(f"Cannot reach {url}: {exc}") from exc


def select_provider(provider: str):
    status, response = put_json(
        PROVIDER_ENDPOINT,
        {"provider": provider},
    )

    if status != 200:
        raise SystemExit(
            f"Could not select provider {provider}: HTTP {status}"
        )

    return response


def current_provider():
    status, response = get_json(PROVIDER_ENDPOINT)

    if status != 200:
        raise SystemExit(
            f"Could not read current provider: HTTP {status}"
        )

    return response


def format_report(provider: str, executions: list) -> str:
    lines = []

    lines.append("Argonaut Vector Smoke Test")
    lines.append("=" * 90)
    lines.append(f"Provider: {provider}")
    lines.append("")

    for execution in executions:
        test = execution["test"]
        results = execution["results"]

        lines.append("=" * 90)
        lines.append(f"{test['id']} — {test['description']}")
        lines.append("=" * 90)
        lines.append(f"Query: {test['query']}")
        lines.append(f"Limit: {test['limit']}")
        lines.append("")

        if not results:
            lines.append("NO RESULTS")
            lines.append("")
            continue

        for rank, result in enumerate(results, start=1):
            content = result.get("content", "")
            preview = " ".join(content.split())[:300]

            lines.append(
                f"#{rank:<2} "
                f"{result.get('id', '<no-id>'):<12} "
                f"score={result.get('score')}"
            )
            lines.append(f"    {preview}")
            lines.append("")

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(
        description="Run Argonaut Vector smoke tests against a selected provider."
    )

    parser.add_argument(
        "--provider",
        default="IN_MEMORY",
        help="Vector provider. Default: IN_MEMORY",
    )

    parser.add_argument(
        "--output-dir",
        default="smoke-results",
        help="Directory where smoke reports are stored.",
    )

    args = parser.parse_args()

    requested_provider = args.provider.upper()

    print(f"Selecting provider: {requested_provider}")

    switch = select_provider(requested_provider)

    print(
        f"Provider switch: "
        f"{switch.get('previous')} -> {switch.get('active')}"
    )

    provider_state = current_provider()
    active_provider = provider_state.get("active")

    if active_provider != requested_provider:
        raise SystemExit(
            f"Provider verification failed: "
            f"requested={requested_provider}, active={active_provider}"
        )

    print(f"Active provider confirmed: {active_provider}")
    print()

    executions = []

    for test in TESTS:
        print(f"Running {test['id']}...")

        status, results = post_json(
            SEARCH_ENDPOINT,
            {
                "query": test["query"],
                "limit": test["limit"],
            },
        )

        if status != 200:
            raise SystemExit(
                f"{test['id']} failed with HTTP {status}"
            )

        executions.append({
            "test": test,
            "results": results,
        })

    report = format_report(
        active_provider,
        executions,
    )

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    safe_provider = active_provider.lower().replace("_", "-")
    output_file = output_dir / f"{safe_provider}-smoke.txt"

    output_file.write_text(report, encoding="utf-8")

    print()
    print(report)
    print()
    print(f"Saved: {output_file}")


if __name__ == "__main__":
    main()