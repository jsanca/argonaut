#!/usr/bin/env python3

import argparse
import json
from urllib import request, error


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
            body = response.read().decode("utf-8")
            return response.status, json.loads(body)

    except error.HTTPError as exc:
        body = exc.read().decode("utf-8")
        raise SystemExit(
            f"HTTP {exc.code}\n"
            f"URL: {url}\n"
            f"Response: {body}"
        )

    except error.URLError as exc:
        raise SystemExit(f"Could not reach {url}: {exc}")


def main():
    parser = argparse.ArgumentParser(
        description="Query the Argonaut Vector search API."
    )

    parser.add_argument(
        "query",
        help="Query text to search for.",
    )

    parser.add_argument(
        "--limit",
        type=int,
        default=5,
        help="Maximum number of results. Default: 5",
    )

    parser.add_argument(
        "--base-url",
        default="http://localhost:8086",
        help="Argonaut Vector base URL.",
    )

    args = parser.parse_args()

    endpoint = f"{args.base_url.rstrip('/')}/api/vector/search"

    status, results = post_json(
        endpoint,
        {
            "query": args.query,
            "limit": args.limit,
        },
    )

    print(f"Query: {args.query}")
    print(f"Limit: {args.limit}")
    print(f"HTTP:  {status}")
    print()

    if not results:
        print("No results.")
        return

    for rank, result in enumerate(results, start=1):
        print("=" * 80)
        print(
            f"#{rank}  "
            f"id={result.get('id')}  "
            f"score={result.get('score')}"
        )
        print("-" * 80)
        print(result.get("content", "").strip())
        print()

    print(f"Returned: {len(results)} result(s)")


if __name__ == "__main__":
    main()